package com.filesharing.service.impl;

import com.filesharing.dto.FileResponse;
import com.filesharing.dto.FileUploadResponse;
import com.filesharing.dto.request.MobileUploadRequest;
import com.filesharing.dto.response.FileSimpleResponse;
import com.filesharing.entity.FileEntity;
import com.filesharing.entity.User;
import com.filesharing.exception.BusinessException;
import com.filesharing.repository.FileRepository;
import com.filesharing.service.FileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 文件服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class FileServiceImpl implements FileService {
    
    private final FileRepository fileRepository;
    
    @Override
    public FileUploadResponse uploadFile(MultipartFile file, Long folderId, User uploader) {
        try {
            // 生成存储文件名
            String storageName = UUID.randomUUID().toString();
            String originalFilename = file.getOriginalFilename();
            String extension = getFileExtension(originalFilename);
            
            // 创建文件实体
            FileEntity fileEntity = new FileEntity();
            fileEntity.setOriginalName(originalFilename);
            fileEntity.setStorageName(storageName);
            fileEntity.setFilePath("/uploads/" + storageName + "." + extension);
            fileEntity.setFileSize(file.getSize());
            fileEntity.setContentType(file.getContentType());
            fileEntity.setExtension(extension);
            fileEntity.setStatus(FileEntity.FileStatus.AVAILABLE);
            fileEntity.setUploader(uploader);
            
            // 保存文件实体
            FileEntity savedFile = fileRepository.save(fileEntity);
            
            log.info("文件上传成功: {} -> {}", originalFilename, storageName);
            
            return FileUploadResponse.builder()
                    .id(savedFile.getId())
                    .originalName(savedFile.getOriginalName())
                    .storageName(savedFile.getStorageName())
                    .fileSize(savedFile.getFileSize())
                    .contentType(savedFile.getContentType())
                    .isNewlyUploaded(true)
                    .message("文件上传成功")
                    .build();
                    
        } catch (Exception e) {
            log.error("文件上传失败: {}", e.getMessage());
            throw new BusinessException("文件上传失败: " + e.getMessage());
        }
    }
    
    @Override
    @Transactional(readOnly = true)
    public FileResponse getFileById(Long fileId) {
        FileEntity fileEntity = fileRepository.findById(fileId)
                .orElseThrow(() -> new BusinessException("文件不存在"));
        
        return convertToFileResponse(fileEntity);
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<FileResponse> getUserFiles(User user, String fileName, int page, int size) {
        // 简化实现，实际应该分页查询
        return fileRepository.findByUploader(user, 
                org.springframework.data.domain.PageRequest.of(page, size))
                .getContent()
                .stream()
                .map(this::convertToFileResponse)
                .collect(Collectors.toList());
    }
    
    @Override
    public FileEntity downloadFile(Long fileId) {
        return fileRepository.findById(fileId)
                .orElseThrow(() -> new BusinessException("文件不存在"));
    }
    
    @Override
    public void deleteFile(Long fileId, User currentUser) {
        FileEntity fileEntity = fileRepository.findById(fileId)
                .orElseThrow(() -> new BusinessException("文件不存在"));
        
        // 验证权限
        if (!fileEntity.getUploader().getId().equals(currentUser.getId())) {
            throw new BusinessException("无权删除此文件");
        }
        
        fileEntity.setStatus(FileEntity.FileStatus.DELETED);
        fileEntity.setDeletedAt(LocalDateTime.now());
        fileRepository.save(fileEntity);
        
        log.info("文件删除成功: {}", fileEntity.getOriginalName());
    }
    
    @Override
    public FileResponse moveFile(Long fileId, Long targetFolderId, User currentUser) {
        // 简化实现
        throw new BusinessException("暂未实现");
    }
    
    @Override
    public FileResponse renameFile(Long fileId, String newName, User currentUser) {
        // 简化实现
        throw new BusinessException("暂未实现");
    }
    
    @Override
    public FileResponse setFilePublic(Long fileId, Boolean isPublic, User currentUser) {
        // 简化实现
        throw new BusinessException("暂未实现");
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<FileResponse> getPublicFiles(int page, int size) {
        return fileRepository.findPublicFiles(
                org.springframework.data.domain.PageRequest.of(page, size))
                .getContent()
                .stream()
                .map(this::convertToFileResponse)
                .collect(Collectors.toList());
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<FileResponse> searchFilesByExtension(String extension, int page, int size) {
        return fileRepository.findByExtension(extension,
                org.springframework.data.domain.PageRequest.of(page, size))
                .getContent()
                .stream()
                .map(this::convertToFileResponse)
                .collect(Collectors.toList());
    }
    
    @Override
    @Transactional(readOnly = true)
    public FileEntity checkFileExists(String md5Hash) {
        List<FileEntity> existingFiles = fileRepository.findByMd5Hash(md5Hash);
        return existingFiles.isEmpty() ? null : existingFiles.get(0);
    }
    
    @Override
    @Transactional(readOnly = true)
    public FileEntity getFileEntityById(Long fileId) {
        return fileRepository.findById(fileId)
                .orElseThrow(() -> new BusinessException("文件不存在"));
    }
    
    @Override
    public void cleanupDeletedFiles() {
        // 简化实现
        log.info("清理已删除文件任务执行");
    }
    
    // 移动端专用方法实现
    
    @Override
    public FileUploadResponse uploadFile(MobileUploadRequest request, User uploader) {
        // 简化实现
        throw new BusinessException("暂未实现");
    }
    
    @Override
    public List<FileSimpleResponse> getRecentFiles(User user, Integer limit) {
        // 简化实现
        return java.util.Collections.emptyList();
    }
    
    @Override
    public List<FileSimpleResponse> getFavoriteFiles(User user) {
        // 简化实现
        return java.util.Collections.emptyList();
    }
    
    @Override
    public void favoriteFile(Long fileId, User user) {
        // 简化实现
        log.info("收藏文件: {}", fileId);
    }
    
    @Override
    public void unfavoriteFile(Long fileId, User user) {
        // 简化实现
        log.info("取消收藏文件: {}", fileId);
    }
    
    @Override
    public List<FileSimpleResponse> searchFiles(String keyword, User user, Integer limit) {
        // 简化实现
        return java.util.Collections.emptyList();
    }
    
    @Override
    public List<FileSimpleResponse> getOfflineAvailableFiles(User user) {
        // 简化实现
        return java.util.Collections.emptyList();
    }
    
    /**
     * 获取文件扩展名
     */
    private String getFileExtension(String filename) {
        if (filename == null || filename.lastIndexOf('.') == -1) {
            return "";
        }
        return filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
    }
    
    /**
     * 转换为FileResponse
     */
    private FileResponse convertToFileResponse(FileEntity fileEntity) {
        return FileResponse.builder()
                .id(fileEntity.getId())
                .originalName(fileEntity.getOriginalName())
                .storageName(fileEntity.getStorageName())
                .fileSize(fileEntity.getFileSize())
                .contentType(fileEntity.getContentType())
                .extension(fileEntity.getExtension())
                .status(fileEntity.getStatus().name())
                .isPublic(fileEntity.getIsPublic())
                .downloadCount(fileEntity.getDownloadCount())
                .uploaderName(fileEntity.getUploader().getUsername())
                .uploaderId(fileEntity.getUploader().getId())
                .createdAt(fileEntity.getCreatedAt().toString())
                .updatedAt(fileEntity.getUpdatedAt().toString())
                .build();
    }
}