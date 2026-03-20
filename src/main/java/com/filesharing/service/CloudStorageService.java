package com.filesharing.service;

import com.filesharing.entity.CloudStorageConfig;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.List;

/**
 * 云存储服务接口
 */
public interface CloudStorageService {
    
    // ==================== 配置管理 ====================
    
    /**
     * 创建云存储配置
     */
    CloudStorageConfig createStorageConfig(CloudStorageConfig config);
    
    /**
     * 更新云存储配置
     */
    CloudStorageConfig updateStorageConfig(Long configId, CloudStorageConfig config);
    
    /**
     * 删除云存储配置
     */
    void deleteStorageConfig(Long configId);
    
    /**
     * 测试云存储连接
     */
    ConnectionTestResult testConnection(Long configId);
    
    /**
     * 获取所有存储配置
     */
    List<StorageConfigInfo> getAllStorageConfigs();
    
    /**
     * 获取启用的存储配置
     */
    List<StorageConfigInfo> getEnabledStorageConfigs();
    
    /**
     * 获取默认存储配置
     */
    CloudStorageConfig getDefaultStorageConfig();
    
    /**
     * 设置默认存储配置
     */
    void setDefaultStorageConfig(Long configId);
    
    // ==================== 文件操作 ====================
    
    /**
     * 上传文件到云存储
     */
    UploadResult uploadFile(MultipartFile file, Long localFileId, Long configId);
    
    /**
     * 上传文件流到云存储
     */
    UploadResult uploadFileStream(InputStream inputStream, String fileName, Long fileSize, 
                                Long localFileId, Long configId);
    
    /**
     * 下载云端文件
     */
    DownloadResult downloadFile(String cloudKey, Long configId);
    
    /**
     * 删除云端文件
     */
    DeleteResult deleteFile(String cloudKey, Long configId);
    
    /**
     * 获取文件访问URL
     */
    String getFileUrl(String cloudKey, Long configId);
    
    /**
     * 获取带签名的临时访问URL
     */
    String getSignedUrl(String cloudKey, Long configId, int expireMinutes);
    
    // ==================== 批量操作 ====================
    
    /**
     * 批量上传文件
     */
    BatchUploadResult batchUpload(List<MultipartFile> files, Long configId);
    
    /**
     * 批量删除文件
     */
    BatchDeleteResult batchDelete(List<String> cloudKeys, Long configId);
    
    // ==================== 存储管理 ====================
    
    /**
     * 获取存储使用情况
     */
    StorageUsage getStorageUsage(Long configId);
    
    /**
     * 获取所有存储使用统计
     */
    List<StorageUsage> getAllStorageUsage();
    
    /**
     * 清理过期文件
     */
    CleanupResult cleanupExpiredFiles(Long configId, int daysOld);
    
    /**
     * 迁移文件到其他存储
     */
    MigrationResult migrateFiles(Long sourceConfigId, Long targetConfigId);

    CloudStorageConfig getStorageConfigById(Long configId);

    // ==================== DTO类定义 ====================
    
    /**
     * 连接测试结果
     */
    class ConnectionTestResult {
        private Boolean success;
        private String message;
        private String endpoint;
        private Long responseTime;
        
        public ConnectionTestResult() {}
        
        public ConnectionTestResult(Boolean success, String message) {
            this.success = success;
            this.message = message;
        }
        
        // getters and setters
        public Boolean getSuccess() { return success; }
        public void setSuccess(Boolean success) { this.success = success; }
        
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        
        public String getEndpoint() { return endpoint; }
        public void setEndpoint(String endpoint) { this.endpoint = endpoint; }
        
        public Long getResponseTime() { return responseTime; }
        public void setResponseTime(Long responseTime) { this.responseTime = responseTime; }
    }
    
    /**
     * 存储配置信息
     */
    class StorageConfigInfo {
        private Long id;
        private String configName;
        private String providerType;
        private String bucketName;
        private String region;
        private Boolean isEnabled;
        private Boolean isDefault;
        private String connectionStatus;
        private String description;
        
        public StorageConfigInfo() {}
        
        // getters and setters
        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        
        public String getConfigName() { return configName; }
        public void setConfigName(String configName) { this.configName = configName; }
        
        public String getProviderType() { return providerType; }
        public void setProviderType(String providerType) { this.providerType = providerType; }
        
        public String getBucketName() { return bucketName; }
        public void setBucketName(String bucketName) { this.bucketName = bucketName; }
        
        public String getRegion() { return region; }
        public void setRegion(String region) { this.region = region; }
        
        public Boolean getIsEnabled() { return isEnabled; }
        public void setIsEnabled(Boolean isEnabled) { this.isEnabled = isEnabled; }
        
        public Boolean getIsDefault() { return isDefault; }
        public void setIsDefault(Boolean isDefault) { this.isDefault = isDefault; }
        
        public String getConnectionStatus() { return connectionStatus; }
        public void setConnectionStatus(String connectionStatus) { this.connectionStatus = connectionStatus; }
        
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
    }
    
    /**
     * 上传结果
     */
    class UploadResult {
        private Boolean success;
        private String cloudKey;
        private String url;
        private String etag;
        private String message;
        private Long fileSize;
        private String storageClass;
        
        public UploadResult() {}
        
        public UploadResult(Boolean success, String message) {
            this.success = success;
            this.message = message;
        }
        
        // getters and setters
        public Boolean getSuccess() { return success; }
        public void setSuccess(Boolean success) { this.success = success; }
        
        public String getCloudKey() { return cloudKey; }
        public void setCloudKey(String cloudKey) { this.cloudKey = cloudKey; }
        
        public String getUrl() { return url; }
        public void setUrl(String url) { this.url = url; }
        
        public String getEtag() { return etag; }
        public void setEtag(String etag) { this.etag = etag; }
        
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        
        public Long getFileSize() { return fileSize; }
        public void setFileSize(Long fileSize) { this.fileSize = fileSize; }
        
        public String getStorageClass() { return storageClass; }
        public void setStorageClass(String storageClass) { this.storageClass = storageClass; }
    }
    
    /**
     * 下载结果
     */
    class DownloadResult {
        private Boolean success;
        private InputStream inputStream;
        private String fileName;
        private Long fileSize;
        private String contentType;
        private String message;
        
        public DownloadResult() {}
        
        public DownloadResult(Boolean success, String message) {
            this.success = success;
            this.message = message;
        }
        
        // getters and setters
        public Boolean getSuccess() { return success; }
        public void setSuccess(Boolean success) { this.success = success; }
        
        public InputStream getInputStream() { return inputStream; }
        public void setInputStream(InputStream inputStream) { this.inputStream = inputStream; }
        
        public String getFileName() { return fileName; }
        public void setFileName(String fileName) { this.fileName = fileName; }
        
        public Long getFileSize() { return fileSize; }
        public void setFileSize(Long fileSize) { this.fileSize = fileSize; }
        
        public String getContentType() { return contentType; }
        public void setContentType(String contentType) { this.contentType = contentType; }
        
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
    }
    
    /**
     * 删除结果
     */
    class DeleteResult {
        private Boolean success;
        private String message;
        private Integer deletedCount;
        
        public DeleteResult() {}
        
        public DeleteResult(Boolean success, String message) {
            this.success = success;
            this.message = message;
        }
        
        // getters and setters
        public Boolean getSuccess() { return success; }
        public void setSuccess(Boolean success) { this.success = success; }
        
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        
        public Integer getDeletedCount() { return deletedCount; }
        public void setDeletedCount(Integer deletedCount) { this.deletedCount = deletedCount; }
    }
    
    /**
     * 批量上传结果
     */
    class BatchUploadResult {
        private Integer totalFiles;
        private Integer successCount;
        private Integer failureCount;
        private List<UploadResult> results;
        private String message;
        
        public BatchUploadResult() {
            this.results = new java.util.ArrayList<>();
        }
        
        // getters and setters
        public Integer getTotalFiles() { return totalFiles; }
        public void setTotalFiles(Integer totalFiles) { this.totalFiles = totalFiles; }
        
        public Integer getSuccessCount() { return successCount; }
        public void setSuccessCount(Integer successCount) { this.successCount = successCount; }
        
        public Integer getFailureCount() { return failureCount; }
        public void setFailureCount(Integer failureCount) { this.failureCount = failureCount; }
        
        public List<UploadResult> getResults() { return results; }
        public void setResults(List<UploadResult> results) { this.results = results; }
        
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
    }
    
    /**
     * 批量删除结果
     */
    class BatchDeleteResult {
        private Integer totalFiles;
        private Integer successCount;
        private Integer failureCount;
        private List<DeleteResult> results;
        private String message;
        
        public BatchDeleteResult() {
            this.results = new java.util.ArrayList<>();
        }
        
        // getters and setters
        public Integer getTotalFiles() { return totalFiles; }
        public void setTotalFiles(Integer totalFiles) { this.totalFiles = totalFiles; }
        
        public Integer getSuccessCount() { return successCount; }
        public void setSuccessCount(Integer successCount) { this.successCount = successCount; }
        
        public Integer getFailureCount() { return failureCount; }
        public void setFailureCount(Integer failureCount) { this.failureCount = failureCount; }
        
        public List<DeleteResult> getResults() { return results; }
        public void setResults(List<DeleteResult> results) { this.results = results; }
        
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
    }
    
    /**
     * 存储使用情况
     */
    class StorageUsage {
        private Long configId;
        private String configName;
        private String providerType;
        private Long totalLimit;
        private Long usedStorage;
        private Long availableStorage;
        private Double usagePercentage;
        private Long fileCount;
        
        public StorageUsage() {}
        
        public StorageUsage(Long configId, String configName, String providerType, 
                          Long totalLimit, Long usedStorage, Long fileCount) {
            this.configId = configId;
            this.configName = configName;
            this.providerType = providerType;
            this.totalLimit = totalLimit;
            this.usedStorage = usedStorage;
            this.fileCount = fileCount;
            this.availableStorage = totalLimit != null ? totalLimit - usedStorage : null;
            this.usagePercentage = totalLimit != null && totalLimit > 0 ? 
                (double) usedStorage / totalLimit * 100 : 0.0;
        }
        
        // getters and setters
        public Long getConfigId() { return configId; }
        public void setConfigId(Long configId) { this.configId = configId; }
        
        public String getConfigName() { return configName; }
        public void setConfigName(String configName) { this.configName = configName; }
        
        public String getProviderType() { return providerType; }
        public void setProviderType(String providerType) { this.providerType = providerType; }
        
        public Long getTotalLimit() { return totalLimit; }
        public void setTotalLimit(Long totalLimit) { this.totalLimit = totalLimit; }
        
        public Long getUsedStorage() { return usedStorage; }
        public void setUsedStorage(Long usedStorage) { this.usedStorage = usedStorage; }
        
        public Long getAvailableStorage() { return availableStorage; }
        public void setAvailableStorage(Long availableStorage) { this.availableStorage = availableStorage; }
        
        public Double getUsagePercentage() { return usagePercentage; }
        public void setUsagePercentage(Double usagePercentage) { this.usagePercentage = usagePercentage; }
        
        public Long getFileCount() { return fileCount; }
        public void setFileCount(Long fileCount) { this.fileCount = fileCount; }
        
        // 兼容性setter方法
        public void setTotalSpace(Long totalSpace) { this.totalLimit = totalSpace; }
        public void setUsedSpace(Long usedSpace) { this.usedStorage = usedSpace; }
        public void setAvailableSpace(Long availableSpace) { this.availableStorage = availableSpace; }
    }
    
    /**
     * 清理结果
     */
    class CleanupResult {
        private Integer totalFiles;
        private Integer cleanedFiles;
        private Long freedSpace;
        private String message;
        
        public CleanupResult() {}
        
        public CleanupResult(Integer totalFiles, Integer cleanedFiles, Long freedSpace) {
            this.totalFiles = totalFiles;
            this.cleanedFiles = cleanedFiles;
            this.freedSpace = freedSpace;
        }
        
        // getters and setters
        public Integer getTotalFiles() { return totalFiles; }
        public void setTotalFiles(Integer totalFiles) { this.totalFiles = totalFiles; }
        
        public Integer getCleanedFiles() { return cleanedFiles; }
        public void setCleanedFiles(Integer cleanedFiles) { this.cleanedFiles = cleanedFiles; }
        
        public Long getFreedSpace() { return freedSpace; }
        public void setFreedSpace(Long freedSpace) { this.freedSpace = freedSpace; }
        
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
    }
    
    /**
     * 迁移结果
     */
    class MigrationResult {
        private Integer totalFiles;
        private Integer migratedFiles;
        private Integer failedFiles;
        private Long transferredSize;
        private String message;
        
        public MigrationResult() {}
        
        public MigrationResult(Integer totalFiles, Integer migratedFiles, Integer failedFiles, 
                             Long transferredSize) {
            this.totalFiles = totalFiles;
            this.migratedFiles = migratedFiles;
            this.failedFiles = failedFiles;
            this.transferredSize = transferredSize;
        }
        
        // getters and setters
        public Integer getTotalFiles() { return totalFiles; }
        public void setTotalFiles(Integer totalFiles) { this.totalFiles = totalFiles; }
        
        public Integer getMigratedFiles() { return migratedFiles; }
        public void setMigratedFiles(Integer migratedFiles) { this.migratedFiles = migratedFiles; }
        
        public Integer getFailedFiles() { return failedFiles; }
        public void setFailedFiles(Integer failedFiles) { this.failedFiles = failedFiles; }
        
        public Long getTransferredSize() { return transferredSize; }
        public void setTransferredSize(Long transferredSize) { this.transferredSize = transferredSize; }
        
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
    }
}