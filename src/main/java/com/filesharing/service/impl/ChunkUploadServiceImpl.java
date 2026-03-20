package com.filesharing.service.impl;

import com.filesharing.dto.request.ChunkInitRequest;
import com.filesharing.dto.request.ChunkUploadRequest;
import com.filesharing.dto.response.ChunkInitResponse;
import com.filesharing.dto.response.ChunkUploadResponse;
import com.filesharing.dto.response.FileSimpleResponse;
import com.filesharing.entity.ChunkUploadRecord;
import com.filesharing.entity.FileEntity;
import com.filesharing.entity.User;
import com.filesharing.exception.BusinessException;
import com.filesharing.repository.ChunkUploadRecordRepository;
import com.filesharing.repository.FileRepository;
import com.filesharing.service.ChunkUploadService;
import com.filesharing.util.FileStorageUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * 分片上传服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class ChunkUploadServiceImpl implements ChunkUploadService {
    
    private final ChunkUploadRecordRepository chunkUploadRecordRepository;
    private final FileRepository fileRepository;
    private final FileStorageUtil fileStorageUtil;
    
    @Override
    public ChunkInitResponse initChunkUpload(ChunkInitRequest request, User uploader) {
        try {
            // 生成上传ID
            String uploadId = UUID.randomUUID().toString().replace("-", "");
            
            // 创建上传记录
            ChunkUploadRecord record = new ChunkUploadRecord();
            record.setUploadId(uploadId);
            record.setOriginalName(request.getFileName());
            record.setFileSize(request.getFileSize());
            record.setChunkSize(request.getChunkSize());
            record.setTotalChunks(request.getTotalChunks());
            record.setUploader(uploader);
            record.setStatus(ChunkUploadRecord.UploadStatus.INITIALIZED);
            record.setExpireTime(LocalDateTime.now().plusHours(24)); // 24小时过期
            
            ChunkUploadRecord savedRecord = chunkUploadRecordRepository.save(record);
            
            log.info("分片上传初始化成功: uploadId={}, 文件名={}, 总分片数={}", 
                    uploadId, request.getFileName(), request.getTotalChunks());
            
            return ChunkInitResponse.builder()
                    .uploadId(uploadId)
                    .isFastTransfer(false)
                    .message("初始化成功")
                    .build();
                    
        } catch (Exception e) {
            log.error("分片上传初始化失败", e);
            throw new BusinessException("初始化失败: " + e.getMessage());
        }
    }
    
    @Override
    public ChunkUploadResponse uploadChunk(ChunkUploadRequest request, User uploader) {
        try {
            // 查找上传记录
            ChunkUploadRecord record = chunkUploadRecordRepository.findByUploadId(request.getUploadId())
                    .orElseThrow(() -> new BusinessException("上传记录不存在"));
            
            // 验证权限
            if (!record.getUploader().getId().equals(uploader.getId())) {
                throw new BusinessException("无权限上传此文件");
            }
            
            // 验证上传状态
            if (record.getStatus() == ChunkUploadRecord.UploadStatus.COMPLETED ||
                record.getStatus() == ChunkUploadRecord.UploadStatus.CANCELLED ||
                record.getStatus() == ChunkUploadRecord.UploadStatus.FAILED) {
                throw new BusinessException("上传状态异常");
            }
            
            // 保存分片
            String chunkFileName = fileStorageUtil.saveChunk(
                    request.getChunk(), request.getUploadId(), request.getChunkIndex());
            
            // 更新上传记录
            record.setUploadedChunks(record.getUploadedChunks() + 1);
            record.setStatus(ChunkUploadRecord.UploadStatus.UPLOADING);
            
            // 检查是否所有分片都已上传
            if (record.getUploadedChunks() >= record.getTotalChunks()) {
                // 合并分片
                String mergedFileName = fileStorageUtil.mergeChunks(
                        request.getUploadId(), record.getTotalChunks(), record.getOriginalName());
                
                // 创建文件记录
                FileEntity fileEntity = new FileEntity();
                fileEntity.setOriginalName(record.getOriginalName());
                fileEntity.setStorageName(mergedFileName);
                fileEntity.setFilePath("/uploads/" + mergedFileName);
                fileEntity.setFileSize(record.getFileSize());
                fileEntity.setContentType(getContentType(record.getOriginalName()));
                fileEntity.setExtension(fileStorageUtil.getFileExtension(record.getOriginalName()));
                fileEntity.setMd5Hash(record.getMd5Hash());
                fileEntity.setStatus(FileEntity.FileStatus.AVAILABLE);
                fileEntity.setUploader(uploader);
                
                fileRepository.save(fileEntity);
                
                record.setStatus(ChunkUploadRecord.UploadStatus.COMPLETED);
                
                log.info("分片上传完成: uploadId={}, 文件名={}", request.getUploadId(), record.getOriginalName());
            }
            
            chunkUploadRecordRepository.save(record);
            
            // 计算进度
            double progress = (double) record.getUploadedChunks() / record.getTotalChunks() * 100;
            
            return ChunkUploadResponse.builder()
                    .uploadId(request.getUploadId())
                    .chunkIndex(request.getChunkIndex())
                    .success(true)
                    .uploadedChunks(record.getUploadedChunks())
                    .totalChunks(record.getTotalChunks())
                    .progress(progress)
                    .message("分片上传成功")
                    .build();
                    
        } catch (Exception e) {
            log.error("分片上传失败: uploadId={}", request.getUploadId(), e);
            throw new BusinessException("上传失败: " + e.getMessage());
        }
    }
    
    @Override
    @Transactional(readOnly = true)
    public ChunkUploadResponse getUploadProgress(String uploadId) {
        ChunkUploadRecord record = chunkUploadRecordRepository.findByUploadId(uploadId)
                .orElseThrow(() -> new BusinessException("上传记录不存在"));
        
        double progress = record.getTotalChunks() > 0 ? 
                (double) record.getUploadedChunks() / record.getTotalChunks() * 100 : 0;
        
        return ChunkUploadResponse.builder()
                .uploadId(uploadId)
                .uploadedChunks(record.getUploadedChunks())
                .totalChunks(record.getTotalChunks())
                .progress(progress)
                .message("获取进度成功")
                .build();
    }
    
    @Override
    public void cancelUpload(String uploadId, User user) {
        ChunkUploadRecord record = chunkUploadRecordRepository.findByUploadId(uploadId)
                .orElseThrow(() -> new BusinessException("上传记录不存在"));
        
        if (!record.getUploader().getId().equals(user.getId())) {
            throw new BusinessException("无权限取消此上传");
        }
        
        record.setStatus(ChunkUploadRecord.UploadStatus.CANCELLED);
        chunkUploadRecordRepository.save(record);
        
        // 清理已上传的分片文件
        cleanupChunks(uploadId, record.getTotalChunks());
        
        log.info("上传已取消: uploadId={}", uploadId);
    }
    
    @Override
    public void cleanupExpiredUploads() {
        try {
            LocalDateTime now = LocalDateTime.now();
            List<ChunkUploadRecord> expiredRecords = chunkUploadRecordRepository.findExpiredUploads(now);
            
            int cleanedCount = 0;
            for (ChunkUploadRecord record : expiredRecords) {
                try {
                    // 清理分片文件
                    cleanupChunks(record.getUploadId(), record.getTotalChunks());
                    
                    // 删除记录
                    chunkUploadRecordRepository.delete(record);
                    cleanedCount++;
                    
                    log.debug("清理过期上传记录: uploadId={}", record.getUploadId());
                } catch (Exception e) {
                    log.warn("清理过期上传记录失败: uploadId={}", record.getUploadId(), e);
                }
            }
            
            log.info("过期上传记录清理完成，共清理 {} 条记录", cleanedCount);
        } catch (Exception e) {
            log.error("清理过期上传记录失败", e);
        }
    }
    
    // 私有方法
    
    private String getContentType(String fileName) {
        String extension = fileStorageUtil.getFileExtension(fileName);
        switch (extension.toLowerCase()) {
            case "jpg":
            case "jpeg":
                return "image/jpeg";
            case "png":
                return "image/png";
            case "gif":
                return "image/gif";
            case "pdf":
                return "application/pdf";
            case "txt":
                return "text/plain";
            case "doc":
            case "docx":
                return "application/msword";
            case "xls":
            case "xlsx":
                return "application/vnd.ms-excel";
            case "ppt":
            case "pptx":
                return "application/vnd.ms-powerpoint";
            default:
                return "application/octet-stream";
        }
    }
    
    private void cleanupChunks(String uploadId, Integer totalChunks) {
        try {
            for (int i = 0; i < totalChunks; i++) {
                String chunkFileName = uploadId + "_part_" + i;
                // 这里应该调用文件存储工具删除分片文件
                // fileStorageUtil.deleteChunk(chunkFileName);
            }
        } catch (Exception e) {
            log.warn("清理分片文件失败: uploadId={}", uploadId, e);
        }
    }
}