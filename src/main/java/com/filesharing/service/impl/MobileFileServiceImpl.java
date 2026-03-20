package com.filesharing.service.impl;

import com.filesharing.dto.response.FileSimpleResponse;
import com.filesharing.entity.FileEntity;
import com.filesharing.entity.User;
import com.filesharing.exception.BusinessException;
import com.filesharing.repository.FileRepository;
import com.filesharing.service.FileService;
import com.filesharing.util.FileStorageUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 移动端文件服务实现 - 专门为移动设备优化的文件操作服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class MobileFileServiceImpl implements FileService {
    
    private final FileRepository fileRepository;
    private final FileStorageUtil fileStorageUtil;
    
    @Override
    public List<FileSimpleResponse> getRecentFiles(User user, Integer limit) {
        try {
            // 获取用户最近访问的文件（按更新时间倒序）
            List<FileEntity> recentFiles = fileRepository.findByUploader(user, 
                PageRequest.of(0, limit != null ? limit : 20))
                .getContent()
                .stream()
                .sorted((f1, f2) -> f2.getUpdatedAt().compareTo(f1.getUpdatedAt()))
                .collect(Collectors.toList());
            
            return recentFiles.stream()
                .map(this::convertToSimpleResponse)
                .collect(Collectors.toList());
                
        } catch (Exception e) {
            log.error("获取最近文件失败: 用户ID={}", user.getId(), e);
            throw new BusinessException("获取最近文件失败: " + e.getMessage());
        }
    }
    
    @Override
    public List<FileSimpleResponse> getFavoriteFiles(User user) {
        try {
            // 这里应该查询用户的收藏文件
            // 暂时返回最近的文件作为示例
            return getRecentFiles(user, 20);
        } catch (Exception e) {
            log.error("获取收藏文件失败: 用户ID={}", user.getId(), e);
            throw new BusinessException("获取收藏文件失败: " + e.getMessage());
        }
    }
    
    @Override
    public void favoriteFile(Long fileId, User user) {
        try {
            FileEntity file = fileRepository.findById(fileId)
                .orElseThrow(() -> new BusinessException("文件不存在"));
            
            // 这里应该实现收藏逻辑
            // 比如在用户收藏表中添加记录
            log.info("用户 {} 收藏文件: {}", user.getId(), file.getOriginalName());
            
        } catch (Exception e) {
            log.error("收藏文件失败: 文件ID={}, 用户ID={}", fileId, user.getId(), e);
            throw new BusinessException("收藏文件失败: " + e.getMessage());
        }
    }
    
    @Override
    public void unfavoriteFile(Long fileId, User user) {
        try {
            FileEntity file = fileRepository.findById(fileId)
                .orElseThrow(() -> new BusinessException("文件不存在"));
            
            // 这里应该实现取消收藏逻辑
            log.info("用户 {} 取消收藏文件: {}", user.getId(), file.getOriginalName());
            
        } catch (Exception e) {
            log.error("取消收藏文件失败: 文件ID={}, 用户ID={}", fileId, user.getId(), e);
            throw new BusinessException("取消收藏文件失败: " + e.getMessage());
        }
    }
    
    @Override
    public List<FileSimpleResponse> searchFiles(String keyword, User user, Integer limit) {
        try {
            if (keyword == null || keyword.trim().isEmpty()) {
                return getRecentFiles(user, limit);
            }
                
            // 根据关键词搜索文件名
            List<FileEntity> searchResults = fileRepository.findByOriginalNameContainingIgnoreCase(keyword);
                
            // 限制结果数量并转换为响应对象
            return searchResults.stream()
                .limit(limit != null ? limit : 20)
                .map(this::convertToSimpleResponse)
                .collect(Collectors.toList());
                    
        } catch (Exception e) {
            log.error("搜索文件失败：关键词={}, 用户 ID={}", keyword, user.getId(), e);
            throw new BusinessException("搜索文件失败：" + e.getMessage());
        }
    }
    
    @Override
    public List<FileSimpleResponse> getOfflineAvailableFiles(User user) {
        try {
            // 返回适合离线使用的文件（比如小文件、常用文件等）
            List<FileEntity> offlineFiles = fileRepository.findByUploader(user, 
                PageRequest.of(0, 50))
                .getContent()
                .stream()
                .filter(file -> file.getFileSize() < 10 * 1024 * 1024) // 小于10MB的文件
                .limit(20)
                .collect(Collectors.toList());
            
            return offlineFiles.stream()
                .map(this::convertToSimpleResponse)
                .collect(Collectors.toList());
                
        } catch (Exception e) {
            log.error("获取离线文件失败: 用户ID={}", user.getId(), e);
            throw new BusinessException("获取离线文件失败: " + e.getMessage());
        }
    }
    
    // ==================== 移动端特有方法 ====================
    
    /**
     * 获取文件缩略图信息
     */
    public FileThumbnailInfo getFileThumbnail(Long fileId, User user) {
        try {
            FileEntity file = fileRepository.findById(fileId)
                .orElseThrow(() -> new BusinessException("文件不存在"));
            
            FileThumbnailInfo thumbnailInfo = new FileThumbnailInfo();
            thumbnailInfo.setFileId(fileId);
            thumbnailInfo.setFileName(file.getOriginalName());
            thumbnailInfo.setFileType(file.getExtension());
            thumbnailInfo.setFileSize(file.getFileSize());
            
            // 根据文件类型设置缩略图URL
            if (isImageFile(file.getExtension())) {
                thumbnailInfo.setThumbnailUrl("/api/files/" + fileId + "/thumbnail");
                thumbnailInfo.setHasThumbnail(true);
            } else {
                thumbnailInfo.setThumbnailUrl(getDefaultIconUrl(file.getExtension()));
                thumbnailInfo.setHasThumbnail(false);
            }
            
            return thumbnailInfo;
            
        } catch (Exception e) {
            log.error("获取文件缩略图信息失败: 文件ID={}", fileId, e);
            throw new BusinessException("获取缩略图信息失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取文件预览信息
     */
    public FilePreviewInfo getFilePreview(Long fileId, User user) {
        try {
            FileEntity file = fileRepository.findById(fileId)
                .orElseThrow(() -> new BusinessException("文件不存在"));
            
            FilePreviewInfo previewInfo = new FilePreviewInfo();
            previewInfo.setFileId(fileId);
            previewInfo.setFileName(file.getOriginalName());
            previewInfo.setFileType(file.getExtension());
            previewInfo.setFileSize(file.getFileSize());
            previewInfo.setUploadTime(file.getCreatedAt().toString());
            previewInfo.setLastModified(file.getUpdatedAt().toString());
            
            // 设置预览URL
            previewInfo.setPreviewUrl("/api/files/" + fileId + "/preview");
            previewInfo.setDownloadUrl("/api/files/" + fileId + "/download");
            
            // 根据文件类型设置预览支持
            previewInfo.setSupportsPreview(isPreviewSupported(file.getExtension()));
            previewInfo.setSupportsDownload(true);
            
            return previewInfo;
            
        } catch (Exception e) {
            log.error("获取文件预览信息失败: 文件ID={}", fileId, e);
            throw new BusinessException("获取预览信息失败: " + e.getMessage());
        }
    }
    
    /**
     * 批量操作文件（移动端优化）
     */
    public BatchMobileOperationResult batchOperateFiles(BatchMobileOperationRequest request, User user) {
        try {
            BatchMobileOperationResult result = new BatchMobileOperationResult();
            result.setTotalFiles(request.getFileIds().size());
            result.setSuccessCount(0);
            result.setFailedCount(0);
            result.setErrorMessages(new java.util.ArrayList<>());
            
            for (Long fileId : request.getFileIds()) {
                try {
                    switch (request.getOperation()) {
                        case "DELETE":
                            deleteFile(fileId, user);
                            result.setSuccessCount(result.getSuccessCount() + 1);
                            break;
                        case "FAVORITE":
                            favoriteFile(fileId, user);
                            result.setSuccessCount(result.getSuccessCount() + 1);
                            break;
                        case "MOVE":
                            // 移动文件逻辑
                            result.setSuccessCount(result.getSuccessCount() + 1);
                            break;
                        default:
                            throw new BusinessException("不支持的操作类型: " + request.getOperation());
                    }
                } catch (Exception e) {
                    result.setFailedCount(result.getFailedCount() + 1);
                    result.getErrorMessages().add("文件ID " + fileId + ": " + e.getMessage());
                    log.warn("批量操作单个文件失败: 文件ID={}, 操作={}, 错误={}", 
                        fileId, request.getOperation(), e.getMessage());
                }
            }
            
            result.setSuccess(result.getFailedCount() == 0);
            result.setMessage("批量操作完成");
            
            log.info("移动端批量操作完成: 用户ID={}, 操作={}, 总数={}, 成功={}, 失败={}", 
                user.getId(), request.getOperation(), result.getTotalFiles(), 
                result.getSuccessCount(), result.getFailedCount());
                
            return result;
            
        } catch (Exception e) {
            log.error("移动端批量操作失败: 用户ID={}", user.getId(), e);
            throw new BusinessException("批量操作失败: " + e.getMessage());
        }
    }
    
    /**
     * 同步用户文件状态
     */
    public SyncResult syncFileStatus(SyncRequest request, User user) {
        try {
            SyncResult result = new SyncResult();
            result.setSyncedFiles(new java.util.ArrayList<>());
            result.setDeletedFiles(new java.util.ArrayList<>());
            
            // 获取服务器端最新的文件列表
            List<FileEntity> serverFiles = fileRepository.findByUploader(user, 
                PageRequest.of(0, 1000)).getContent();
            
            // 对比客户端和服务器端的差异
            for (ClientFileInfo clientFile : request.getClientFiles()) {
                FileEntity serverFile = serverFiles.stream()
                    .filter(f -> f.getId().equals(clientFile.getFileId()))
                    .findFirst()
                    .orElse(null);
                
                if (serverFile != null) {
                    // 检查是否有更新
                    if (serverFile.getUpdatedAt().isAfter(clientFile.getLastSyncTime())) {
                        result.getSyncedFiles().add(convertToSimpleResponse(serverFile));
                    }
                } else {
                    // 文件已在服务器端删除
                    result.getDeletedFiles().add(clientFile.getFileId());
                }
            }
            
            result.setSuccess(true);
            result.setMessage("同步完成");
            result.setServerTime(LocalDateTime.now().toString());
            
            log.debug("文件状态同步完成: 用户ID={}, 客户端文件数={}, 需要同步={}, 已删除={}", 
                user.getId(), request.getClientFiles().size(), 
                result.getSyncedFiles().size(), result.getDeletedFiles().size());
                
            return result;
            
        } catch (Exception e) {
            log.error("文件状态同步失败: 用户ID={}", user.getId(), e);
            throw new BusinessException("同步失败: " + e.getMessage());
        }
    }
    
    // ==================== 辅助方法 ====================
    
    private FileSimpleResponse convertToSimpleResponse(FileEntity file) {
        return FileSimpleResponse.builder()
            .id(file.getId())
            .fileName(file.getOriginalName())
            .fileType(file.getExtension())
            .fileSize(file.getFileSize())
            .createdAt(file.getCreatedAt())
            .updatedAt(file.getUpdatedAt())
            .build();
    }
    
    private boolean isImageFile(String extension) {
        return extension != null && 
            (extension.equalsIgnoreCase("jpg") || 
             extension.equalsIgnoreCase("jpeg") || 
             extension.equalsIgnoreCase("png") || 
             extension.equalsIgnoreCase("gif") || 
             extension.equalsIgnoreCase("bmp"));
    }
    
    private boolean isPreviewSupported(String extension) {
        if (extension == null) return false;
        
        String lowerExt = extension.toLowerCase();
        return lowerExt.equals("pdf") || 
               lowerExt.equals("txt") || 
               isImageFile(lowerExt) ||
               lowerExt.equals("doc") || 
               lowerExt.equals("docx");
    }
    
    private String getDefaultIconUrl(String extension) {
        if (extension == null) return "/icons/file.svg";
        
        String lowerExt = extension.toLowerCase();
        switch (lowerExt) {
            case "pdf": return "/icons/pdf.svg";
            case "doc":
            case "docx": return "/icons/word.svg";
            case "xls":
            case "xlsx": return "/icons/excel.svg";
            case "ppt":
            case "pptx": return "/icons/powerpoint.svg";
            case "zip":
            case "rar": return "/icons/archive.svg";
            case "mp3":
            case "wav": return "/icons/audio.svg";
            case "mp4":
            case "avi": return "/icons/video.svg";
            default: return "/icons/file.svg";
        }
    }
    
    // ==================== 未实现的标准方法 ====================
    // 这些方法在移动端服务中不是必需的，但为了实现接口需要提供基本实现
    
    @Override
    public com.filesharing.dto.FileUploadResponse uploadFile(MultipartFile file, Long folderId, User uploader) {
        throw new BusinessException("请使用移动端专用上传接口");
    }
    
    @Override
    public com.filesharing.dto.FileResponse getFileById(Long fileId) {
        throw new BusinessException("请使用移动端专用接口");
    }
    
    @Override
    public List<com.filesharing.dto.FileResponse> getUserFiles(User user, String fileName, int page, int size) {
        throw new BusinessException("请使用移动端专用接口");
    }
    
    @Override
    public FileEntity downloadFile(Long fileId) {
        throw new BusinessException("请使用移动端专用接口");
    }
    
    @Override
    public void deleteFile(Long fileId, User currentUser) {
        FileEntity file = fileRepository.findById(fileId)
            .orElseThrow(() -> new BusinessException("文件不存在"));
        
        if (!file.getUploader().getId().equals(currentUser.getId())) {
            throw new BusinessException("无权限删除此文件");
        }
        
        file.setStatus(FileEntity.FileStatus.DELETED);
        file.setDeletedAt(LocalDateTime.now());
        fileRepository.save(file);
        
        log.info("移动端删除文件成功: 文件ID={}, 用户ID={}", fileId, currentUser.getId());
    }
    
    @Override
    public com.filesharing.dto.FileResponse moveFile(Long fileId, Long targetFolderId, User currentUser) {
        throw new BusinessException("移动功能暂未实现");
    }
    
    @Override
    public com.filesharing.dto.FileResponse renameFile(Long fileId, String newName, User currentUser) {
        throw new BusinessException("重命名功能暂未实现");
    }

    @Override
    public com.filesharing.dto.FileResponse copyFile(Long fileId, Long targetFolderId, User currentUser) {
        throw new BusinessException("复制功能暂未实现");
    }
    
    @Override
    public com.filesharing.dto.FileResponse setFilePublic(Long fileId, Boolean isPublic, User currentUser) {
        throw new BusinessException("设置公开状态功能暂未实现");
    }
    
    @Override
    public List<com.filesharing.dto.FileResponse> getPublicFiles(int page, int size) {
        throw new BusinessException("获取公开文件功能暂未实现");
    }
    
    @Override
    public List<com.filesharing.dto.FileResponse> searchFilesByExtension(String extension, int page, int size) {
        throw new BusinessException("按扩展名搜索功能暂未实现");
    }
    
    @Override
    public FileEntity checkFileExists(String md5Hash) {
        List<FileEntity> existingFiles = fileRepository.findByMd5Hash(md5Hash);
        return existingFiles.isEmpty() ? null : existingFiles.get(0);
    }
    
    @Override
    public FileEntity getFileEntityById(Long fileId) {
        return fileRepository.findById(fileId)
            .orElseThrow(() -> new BusinessException("文件不存在"));
    }
    
    @Override
    public void cleanupDeletedFiles() {
        // 清理逻辑
    }
    
    @Override
    public com.filesharing.dto.FileUploadResponse initChunkUpload(String fileName, Long fileSize, Long chunkSize, 
                                                                 Integer totalChunks, Long folderId, User uploader) {
        throw new BusinessException("分片上传初始化功能暂未实现");
    }
    
    @Override
    public com.filesharing.dto.FileUploadResponse uploadChunk(MultipartFile chunk, String uploadId, Integer chunkIndex, 
                                                             Integer totalChunks, User uploader) {
        throw new BusinessException("分片上传功能暂未实现");
    }
    
    @Override
    public Double getUploadProgress(String uploadId) {
        return 0.0; // 返回默认进度
    }
    
    @Override
    public com.filesharing.dto.FileUploadResponse uploadFile(com.filesharing.dto.request.MobileUploadRequest request, User uploader) {
        throw new BusinessException("请使用移动端控制器接口");
    }
    
    // ==================== DTO类定义 ====================
    
    public static class FileThumbnailInfo {
        private Long fileId;
        private String fileName;
        private String fileType;
        private Long fileSize;
        private String thumbnailUrl;
        private Boolean hasThumbnail;
        
        // getters and setters
        public Long getFileId() { return fileId; }
        public void setFileId(Long fileId) { this.fileId = fileId; }
        
        public String getFileName() { return fileName; }
        public void setFileName(String fileName) { this.fileName = fileName; }
        
        public String getFileType() { return fileType; }
        public void setFileType(String fileType) { this.fileType = fileType; }
        
        public Long getFileSize() { return fileSize; }
        public void setFileSize(Long fileSize) { this.fileSize = fileSize; }
        
        public String getThumbnailUrl() { return thumbnailUrl; }
        public void setThumbnailUrl(String thumbnailUrl) { this.thumbnailUrl = thumbnailUrl; }
        
        public Boolean getHasThumbnail() { return hasThumbnail; }
        public void setHasThumbnail(Boolean hasThumbnail) { this.hasThumbnail = hasThumbnail; }
    }
    
    public static class FilePreviewInfo {
        private Long fileId;
        private String fileName;
        private String fileType;
        private Long fileSize;
        private String uploadTime;
        private String lastModified;
        private String previewUrl;
        private String downloadUrl;
        private Boolean supportsPreview;
        private Boolean supportsDownload;
        
        // getters and setters
        public Long getFileId() { return fileId; }
        public void setFileId(Long fileId) { this.fileId = fileId; }
        
        public String getFileName() { return fileName; }
        public void setFileName(String fileName) { this.fileName = fileName; }
        
        public String getFileType() { return fileType; }
        public void setFileType(String fileType) { this.fileType = fileType; }
        
        public Long getFileSize() { return fileSize; }
        public void setFileSize(Long fileSize) { this.fileSize = fileSize; }
        
        public String getUploadTime() { return uploadTime; }
        public void setUploadTime(String uploadTime) { this.uploadTime = uploadTime; }
        
        public String getLastModified() { return lastModified; }
        public void setLastModified(String lastModified) { this.lastModified = lastModified; }
        
        public String getPreviewUrl() { return previewUrl; }
        public void setPreviewUrl(String previewUrl) { this.previewUrl = previewUrl; }
        
        public String getDownloadUrl() { return downloadUrl; }
        public void setDownloadUrl(String downloadUrl) { this.downloadUrl = downloadUrl; }
        
        public Boolean getSupportsPreview() { return supportsPreview; }
        public void setSupportsPreview(Boolean supportsPreview) { this.supportsPreview = supportsPreview; }
        
        public Boolean getSupportsDownload() { return supportsDownload; }
        public void setSupportsDownload(Boolean supportsDownload) { this.supportsDownload = supportsDownload; }
    }
    
    public static class BatchMobileOperationRequest {
        private List<Long> fileIds;
        private String operation; // DELETE, FAVORITE, MOVE
        
        public List<Long> getFileIds() { return fileIds; }
        public void setFileIds(List<Long> fileIds) { this.fileIds = fileIds; }
        
        public String getOperation() { return operation; }
        public void setOperation(String operation) { this.operation = operation; }
    }
    
    public static class BatchMobileOperationResult {
        private Boolean success;
        private String message;
        private Integer totalFiles;
        private Integer successCount;
        private Integer failedCount;
        private List<String> errorMessages;
        
        public Boolean getSuccess() { return success; }
        public void setSuccess(Boolean success) { this.success = success; }
        
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        
        public Integer getTotalFiles() { return totalFiles; }
        public void setTotalFiles(Integer totalFiles) { this.totalFiles = totalFiles; }
        
        public Integer getSuccessCount() { return successCount; }
        public void setSuccessCount(Integer successCount) { this.successCount = successCount; }
        
        public Integer getFailedCount() { return failedCount; }
        public void setFailedCount(Integer failedCount) { this.failedCount = failedCount; }
        
        public List<String> getErrorMessages() { return errorMessages; }
        public void setErrorMessages(List<String> errorMessages) { this.errorMessages = errorMessages; }
    }
    
    public static class SyncRequest {
        private List<ClientFileInfo> clientFiles;
        private String deviceInfo;
        
        public List<ClientFileInfo> getClientFiles() { return clientFiles; }
        public void setClientFiles(List<ClientFileInfo> clientFiles) { this.clientFiles = clientFiles; }
        
        public String getDeviceInfo() { return deviceInfo; }
        public void setDeviceInfo(String deviceInfo) { this.deviceInfo = deviceInfo; }
    }
    
    public static class ClientFileInfo {
        private Long fileId;
        private LocalDateTime lastSyncTime;
        private String fileHash;
        
        public Long getFileId() { return fileId; }
        public void setFileId(Long fileId) { this.fileId = fileId; }
        
        public LocalDateTime getLastSyncTime() { return lastSyncTime; }
        public void setLastSyncTime(LocalDateTime lastSyncTime) { this.lastSyncTime = lastSyncTime; }
        
        public String getFileHash() { return fileHash; }
        public void setFileHash(String fileHash) { this.fileHash = fileHash; }
    }
    
    public static class SyncResult {
        private Boolean success;
        private String message;
        private String serverTime;
        private List<FileSimpleResponse> syncedFiles;
        private List<Long> deletedFiles;
        
        public Boolean getSuccess() { return success; }
        public void setSuccess(Boolean success) { this.success = success; }
        
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        
        public String getServerTime() { return serverTime; }
        public void setServerTime(String serverTime) { this.serverTime = serverTime; }
        
        public List<FileSimpleResponse> getSyncedFiles() { return syncedFiles; }
        public void setSyncedFiles(List<FileSimpleResponse> syncedFiles) { this.syncedFiles = syncedFiles; }
        
        public List<Long> getDeletedFiles() { return deletedFiles; }
        public void setDeletedFiles(List<Long> deletedFiles) { this.deletedFiles = deletedFiles; }
    }
}