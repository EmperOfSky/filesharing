package com.filesharing.service;

import com.filesharing.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 批量操作服务接口
 */
public interface BatchOperationService {
    
    // ==================== 批量上传相关 ====================
    
    /**
     * 批量上传文件
     */
    BatchOperationResult batchUpload(List<MultipartFile> files, Long targetFolderId, User user);
    
    /**
     * 批量上传并解压
     */
    BatchOperationResult batchUploadAndExtract(List<MultipartFile> files, Long targetFolderId, User user);
    
    // ==================== 批量删除相关 ====================
    
    /**
     * 批量删除文件
     */
    BatchOperationResult batchDelete(List<Long> fileIds, User user);
    
    /**
     * 批量删除文件夹
     */
    BatchOperationResult batchDeleteFolders(List<Long> folderIds, User user);
    
    // ==================== 批量移动相关 ====================
    
    /**
     * 批量移动文件
     */
    BatchOperationResult batchMoveFiles(List<Long> fileIds, Long targetFolderId, User user);
    
    /**
     * 批量移动文件夹
     */
    BatchOperationResult batchMoveFolders(List<Long> folderIds, Long targetFolderId, User user);
    
    // ==================== 批量复制相关 ====================
    
    /**
     * 批量复制文件
     */
    BatchOperationResult batchCopyFiles(List<Long> fileIds, Long targetFolderId, User user);
    
    /**
     * 批量复制文件夹
     */
    BatchOperationResult batchCopyFolders(List<Long> folderIds, Long targetFolderId, User user);
    
    // ==================== 批量重命名相关 ====================
    
    /**
     * 批量重命名文件
     */
    BatchOperationResult batchRenameFiles(List<FileRenameInfo> renameInfos, User user);
    
    /**
     * 批量重命名文件夹
     */
    BatchOperationResult batchRenameFolders(List<FolderRenameInfo> renameInfos, User user);
    
    // ==================== 批量压缩相关 ====================
    
    /**
     * 批量压缩文件
     */
    BatchOperationResult batchCompress(List<Long> itemIds, String archiveName, User user);
    
    // ==================== 操作管理相关 ====================
    
    /**
     * 获取用户的批量操作列表
     */
    Page<BatchOperationInfo> getUserOperations(User user, String operationType, String status, int page, int size);
    
    /**
     * 获取批量操作详情
     */
    BatchOperationDetailInfo getOperationDetails(Long operationId, User user);
    
    /**
     * 取消批量操作
     */
    void cancelOperation(Long operationId, User user);
    
    /**
     * 重试失败的操作
     */
    void retryFailedOperation(Long operationId, User user);
    
    /**
     * 获取操作进度
     */
    OperationProgress getOperationProgress(Long operationId, User user);
    
    /**
     * 清理已完成的操作记录
     */
    void cleanupCompletedOperations(User user, int daysOld);
    
    /**
     * 获取用户操作统计
     */
    UserOperationStats getUserOperationStats(User user);
    
    // ==================== DTO类定义 ====================
    
    /**
     * 批量操作结果
     */
    class BatchOperationResult {
        private Long operationId;
        private String operationType;
        private Integer totalItems;
        private Integer successCount;
        private Integer failureCount;
        private String status;
        private String message;
        private List<String> errorMessages;
        
        // 构造函数和getter/setter
        public BatchOperationResult() {
            this.errorMessages = new java.util.ArrayList<>();
        }
        
        public BatchOperationResult(Long operationId, String operationType, Integer totalItems) {
            this.operationId = operationId;
            this.operationType = operationType;
            this.totalItems = totalItems;
            this.errorMessages = new java.util.ArrayList<>();
        }
        
        // getters and setters
        public Long getOperationId() { return operationId; }
        public void setOperationId(Long operationId) { this.operationId = operationId; }
        
        public String getOperationType() { return operationType; }
        public void setOperationType(String operationType) { this.operationType = operationType; }
        
        public Integer getTotalItems() { return totalItems; }
        public void setTotalItems(Integer totalItems) { this.totalItems = totalItems; }
        
        public Integer getSuccessCount() { return successCount; }
        public void setSuccessCount(Integer successCount) { this.successCount = successCount; }
        
        public Integer getFailureCount() { return failureCount; }
        public void setFailureCount(Integer failureCount) { this.failureCount = failureCount; }
        
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        
        public List<String> getErrorMessages() { return errorMessages; }
        public void setErrorMessages(List<String> errorMessages) { this.errorMessages = errorMessages; }
        
        public void addErrorMessage(String message) {
            this.errorMessages.add(message);
        }
    }
    
    /**
     * 文件重命名信息
     */
    class FileRenameInfo {
        private Long fileId;
        private String newName;
        
        public FileRenameInfo() {}
        
        public FileRenameInfo(Long fileId, String newName) {
            this.fileId = fileId;
            this.newName = newName;
        }
        
        public Long getFileId() { return fileId; }
        public void setFileId(Long fileId) { this.fileId = fileId; }
        
        public String getNewName() { return newName; }
        public void setNewName(String newName) { this.newName = newName; }
    }
    
    /**
     * 文件夹重命名信息
     */
    class FolderRenameInfo {
        private Long folderId;
        private String newName;
        
        public FolderRenameInfo() {}
        
        public FolderRenameInfo(Long folderId, String newName) {
            this.folderId = folderId;
            this.newName = newName;
        }
        
        public Long getFolderId() { return folderId; }
        public void setFolderId(Long folderId) { this.folderId = folderId; }
        
        public String getNewName() { return newName; }
        public void setNewName(String newName) { this.newName = newName; }
    }
    
    /**
     * 批量操作信息
     */
    class BatchOperationInfo {
        private Long id;
        private String operationType;
        private String status;
        private String description;
        private Integer totalItems;
        private Integer processedItems;
        private Integer successItems;
        private Integer failedItems;
        private Double progressPercentage;
        private String createdAt;
        private String startedAt;
        private String completedAt;
        
        // 构造函数和getter/setter
        public BatchOperationInfo() {}
        
        // getters and setters
        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        
        public String getOperationType() { return operationType; }
        public void setOperationType(String operationType) { this.operationType = operationType; }
        
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        
        public Integer getTotalItems() { return totalItems; }
        public void setTotalItems(Integer totalItems) { this.totalItems = totalItems; }
        
        public Integer getProcessedItems() { return processedItems; }
        public void setProcessedItems(Integer processedItems) { this.processedItems = processedItems; }
        
        public Integer getSuccessItems() { return successItems; }
        public void setSuccessItems(Integer successItems) { this.successItems = successItems; }
        
        public Integer getFailedItems() { return failedItems; }
        public void setFailedItems(Integer failedItems) { this.failedItems = failedItems; }
        
        public Double getProgressPercentage() { return progressPercentage; }
        public void setProgressPercentage(Double progressPercentage) { this.progressPercentage = progressPercentage; }
        
        public String getCreatedAt() { return createdAt; }
        public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
        
        public String getStartedAt() { return startedAt; }
        public void setStartedAt(String startedAt) { this.startedAt = startedAt; }
        
        public String getCompletedAt() { return completedAt; }
        public void setCompletedAt(String completedAt) { this.completedAt = completedAt; }
    }
    
    /**
     * 批量操作详情信息
     */
    class BatchOperationDetailInfo {
        private List<OperationItemDetail> items;
        private OperationSummary summary;
        
        public BatchOperationDetailInfo() {
            this.items = new java.util.ArrayList<>();
        }
        
        public BatchOperationDetailInfo(List<OperationItemDetail> items, OperationSummary summary) {
            this.items = items;
            this.summary = summary;
        }
        
        public List<OperationItemDetail> getItems() { return items; }
        public void setItems(List<OperationItemDetail> items) { this.items = items; }
        
        public OperationSummary getSummary() { return summary; }
        public void setSummary(OperationSummary summary) { this.summary = summary; }
    }
    
    /**
     * 操作项详情
     */
    class OperationItemDetail {
        private Long itemId;
        private String itemName;
        private String itemType;
        private String status;
        private String resultMessage;
        private String errorMessage;
        private String completedAt;
        private Long processingTime;
        
        // 构造函数和getter/setter
        public OperationItemDetail() {}
        
        // getters and setters
        public Long getItemId() { return itemId; }
        public void setItemId(Long itemId) { this.itemId = itemId; }
        
        public String getItemName() { return itemName; }
        public void setItemName(String itemName) { this.itemName = itemName; }
        
        public String getItemType() { return itemType; }
        public void setItemType(String itemType) { this.itemType = itemType; }
        
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        
        public String getResultMessage() { return resultMessage; }
        public void setResultMessage(String resultMessage) { this.resultMessage = resultMessage; }
        
        public String getErrorMessage() { return errorMessage; }
        public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
        
        public String getCompletedAt() { return completedAt; }
        public void setCompletedAt(String completedAt) { this.completedAt = completedAt; }
        
        public Long getProcessingTime() { return processingTime; }
        public void setProcessingTime(Long processingTime) { this.processingTime = processingTime; }
    }
    
    /**
     * 操作摘要
     */
    class OperationSummary {
        private Integer totalItems;
        private Integer successCount;
        private Integer failureCount;
        private Double averageProcessingTime;
        private String completionTime;
        
        public OperationSummary() {}
        
        public OperationSummary(Integer totalItems, Integer successCount, Integer failureCount) {
            this.totalItems = totalItems;
            this.successCount = successCount;
            this.failureCount = failureCount;
        }
        
        // getters and setters
        public Integer getTotalItems() { return totalItems; }
        public void setTotalItems(Integer totalItems) { this.totalItems = totalItems; }
        
        public Integer getSuccessCount() { return successCount; }
        public void setSuccessCount(Integer successCount) { this.successCount = successCount; }
        
        public Integer getFailureCount() { return failureCount; }
        public void setFailureCount(Integer failureCount) { this.failureCount = failureCount; }
        
        public Double getAverageProcessingTime() { return averageProcessingTime; }
        public void setAverageProcessingTime(Double averageProcessingTime) { this.averageProcessingTime = averageProcessingTime; }
        
        public String getCompletionTime() { return completionTime; }
        public void setCompletionTime(String completionTime) { this.completionTime = completionTime; }
    }
    
    /**
     * 操作进度
     */
    class OperationProgress {
        private Long operationId;
        private String status;
        private Integer totalItems;
        private Integer processedItems;
        private Integer successItems;
        private Integer failedItems;
        private Double progressPercentage;
        private String estimatedCompletion;
        private Boolean isCancellable;
        
        public OperationProgress() {}
        
        // getters and setters
        public Long getOperationId() { return operationId; }
        public void setOperationId(Long operationId) { this.operationId = operationId; }
        
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        
        public Integer getTotalItems() { return totalItems; }
        public void setTotalItems(Integer totalItems) { this.totalItems = totalItems; }
        
        public Integer getProcessedItems() { return processedItems; }
        public void setProcessedItems(Integer processedItems) { this.processedItems = processedItems; }
        
        public Integer getSuccessItems() { return successItems; }
        public void setSuccessItems(Integer successItems) { this.successItems = successItems; }
        
        public Integer getFailedItems() { return failedItems; }
        public void setFailedItems(Integer failedItems) { this.failedItems = failedItems; }
        
        public Double getProgressPercentage() { return progressPercentage; }
        public void setProgressPercentage(Double progressPercentage) { this.progressPercentage = progressPercentage; }
        
        public String getEstimatedCompletion() { return estimatedCompletion; }
        public void setEstimatedCompletion(String estimatedCompletion) { this.estimatedCompletion = estimatedCompletion; }
        
        public Boolean getIsCancellable() { return isCancellable; }
        public void setIsCancellable(Boolean isCancellable) { this.isCancellable = isCancellable; }
    }
    
    /**
     * 用户操作统计
     */
    class UserOperationStats {
        private Long totalOperations;
        private Long completedOperations;
        private Long failedOperations;
        private Double successRate;
        private Double averageProcessingTime;
        private List<OperationTypeStat> typeStats;
        
        public UserOperationStats() {}
        
        public UserOperationStats(Long totalOperations, Long completedOperations, Long failedOperations, 
                                Double averageProcessingTime) {
            this.totalOperations = totalOperations;
            this.completedOperations = completedOperations;
            this.failedOperations = failedOperations;
            this.successRate = totalOperations > 0 ? (double) completedOperations / totalOperations * 100 : 0.0;
            this.averageProcessingTime = averageProcessingTime;
        }
        
        /**
         * 操作类型统计
         */
        public static class OperationTypeStat {
            private String operationType;
            private Long count;
            private Double successRate;
            private Double averageTime;
            
            public OperationTypeStat() {}
            
            public OperationTypeStat(String operationType, Long count, Double successRate, Double averageTime) {
                this.operationType = operationType;
                this.count = count;
                this.successRate = successRate;
                this.averageTime = averageTime;
            }
            
            // getters and setters
            public String getOperationType() { return operationType; }
            public void setOperationType(String operationType) { this.operationType = operationType; }
            
            public Long getCount() { return count; }
            public void setCount(Long count) { this.count = count; }
            
            public Double getSuccessRate() { return successRate; }
            public void setSuccessRate(Double successRate) { this.successRate = successRate; }
            
            public Double getAverageTime() { return averageTime; }
            public void setAverageTime(Double averageTime) { this.averageTime = averageTime; }
        }
        
        // getters and setters
        public Long getTotalOperations() { return totalOperations; }
        public void setTotalOperations(Long totalOperations) { this.totalOperations = totalOperations; }
        
        public Long getCompletedOperations() { return completedOperations; }
        public void setCompletedOperations(Long completedOperations) { this.completedOperations = completedOperations; }
        
        public Long getFailedOperations() { return failedOperations; }
        public void setFailedOperations(Long failedOperations) { this.failedOperations = failedOperations; }
        
        public Double getSuccessRate() { return successRate; }
        public void setSuccessRate(Double successRate) { this.successRate = successRate; }
        
        public Double getAverageProcessingTime() { return averageProcessingTime; }
        public void setAverageProcessingTime(Double averageProcessingTime) { this.averageProcessingTime = averageProcessingTime; }
        
        public List<OperationTypeStat> getTypeStats() { return typeStats; }
        public void setTypeStats(List<OperationTypeStat> typeStats) { this.typeStats = typeStats; }
    }
}