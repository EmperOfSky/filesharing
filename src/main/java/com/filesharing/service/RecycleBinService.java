package com.filesharing.service;

import com.filesharing.entity.User;
import org.springframework.data.domain.Page;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 回收站服务接口
 */
public interface RecycleBinService {
    
    /**
     * 将文件移至回收站
     */
    void moveToRecycleBin(Long fileId, User user, String deleteReason);
    
    /**
     * 将文件夹移至回收站
     */
    void moveFolderToRecycleBin(Long folderId, User user, String deleteReason);
    
    /**
     * 恢复回收站项目
     */
    RestoreResult restoreItem(Long recycleBinId, User user);
    
    /**
     * 恢复到指定位置
     */
    RestoreResult restoreToLocation(Long recycleBinId, Long targetFolderId, User user);
    
    /**
     * 彻底删除回收站项目
     */
    void permanentlyDelete(Long recycleBinId, User user);
    
    /**
     * 清空回收站
     */
    void emptyRecycleBin(User user);
    
    /**
     * 获取用户的回收站项目
     */
    Page<RecycleBinItem> getUserRecycleBin(User user, String itemType, int page, int size);
    
    /**
     * 搜索回收站项目
     */
    Page<RecycleBinItem> searchRecycleBin(User user, String keyword, int page, int size);
    
    /**
     * 获取回收站统计信息
     */
    RecycleBinStats getRecycleBinStats(User user);
    
    /**
     * 清理过期的回收站项目
     */
    void cleanupExpiredItems();
    
    /**
     * 获取即将过期的项目提醒
     */
    List<ExpiringItem> getExpiringItemsReminder(User user, int hours);
    
    /**
     * 批量操作回收站项目
     */
    BatchOperationResult batchRestore(List<Long> recycleBinIds, User user);
    
    /**
     * 批量永久删除
     */
    BatchOperationResult batchPermanentlyDelete(List<Long> recycleBinIds, User user);
    
    /**
     * 回收站项目DTO
     */
    class RecycleBinItem {
        private Long id;
        private Long itemId;
        private String itemType;
        private String originalName;
        private String originalPath;
        private Long fileSize;
        private String fileType;
        private String deletedByName;
        private String deletedAt;
        private String expireAt;
        private Boolean isRecoverable;
        private String deleteReason;
        
        // 构造函数和getter/setter
        public RecycleBinItem() {}
        
        public RecycleBinItem(Long id, Long itemId, String itemType, String originalName, 
                            String originalPath, Long fileSize, String fileType, 
                            String deletedByName, String deletedAt, String expireAt, 
                            Boolean isRecoverable, String deleteReason) {
            this.id = id;
            this.itemId = itemId;
            this.itemType = itemType;
            this.originalName = originalName;
            this.originalPath = originalPath;
            this.fileSize = fileSize;
            this.fileType = fileType;
            this.deletedByName = deletedByName;
            this.deletedAt = deletedAt;
            this.expireAt = expireAt;
            this.isRecoverable = isRecoverable;
            this.deleteReason = deleteReason;
        }
        
        // getters and setters
        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        
        public Long getItemId() { return itemId; }
        public void setItemId(Long itemId) { this.itemId = itemId; }
        
        public String getItemType() { return itemType; }
        public void setItemType(String itemType) { this.itemType = itemType; }
        
        public String getOriginalName() { return originalName; }
        public void setOriginalName(String originalName) { this.originalName = originalName; }
        
        public String getOriginalPath() { return originalPath; }
        public void setOriginalPath(String originalPath) { this.originalPath = originalPath; }
        
        public Long getFileSize() { return fileSize; }
        public void setFileSize(Long fileSize) { this.fileSize = fileSize; }
        
        public String getFileType() { return fileType; }
        public void setFileType(String fileType) { this.fileType = fileType; }
        
        public String getDeletedByName() { return deletedByName; }
        public void setDeletedByName(String deletedByName) { this.deletedByName = deletedByName; }
        
        public String getDeletedAt() { return deletedAt; }
        public void setDeletedAt(String deletedAt) { this.deletedAt = deletedAt; }
        
        public String getExpireAt() { return expireAt; }
        public void setExpireAt(String expireAt) { this.expireAt = expireAt; }
        
        public Boolean getIsRecoverable() { return isRecoverable; }
        public void setIsRecoverable(Boolean isRecoverable) { this.isRecoverable = isRecoverable; }
        
        public String getDeleteReason() { return deleteReason; }
        public void setDeleteReason(String deleteReason) { this.deleteReason = deleteReason; }
    }
    
    /**
     * 恢复结果DTO
     */
    class RestoreResult {
        private Boolean success;
        private String message;
        private Long restoredItemId;
        private String restoredItemType;
        private String restorePath;
        
        // 构造函数和getter/setter
        public RestoreResult() {}
        
        public RestoreResult(Boolean success, String message) {
            this.success = success;
            this.message = message;
        }
        
        public RestoreResult(Boolean success, String message, Long restoredItemId, 
                           String restoredItemType, String restorePath) {
            this.success = success;
            this.message = message;
            this.restoredItemId = restoredItemId;
            this.restoredItemType = restoredItemType;
            this.restorePath = restorePath;
        }
        
        // getters and setters
        public Boolean getSuccess() { return success; }
        public void setSuccess(Boolean success) { this.success = success; }
        
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        
        public Long getRestoredItemId() { return restoredItemId; }
        public void setRestoredItemId(Long restoredItemId) { this.restoredItemId = restoredItemId; }
        
        public String getRestoredItemType() { return restoredItemType; }
        public void setRestoredItemType(String restoredItemType) { this.restoredItemType = restoredItemType; }
        
        public String getRestorePath() { return restorePath; }
        public void setRestorePath(String restorePath) { this.restorePath = restorePath; }
    }
    
    /**
     * 回收站统计DTO
     */
    class RecycleBinStats {
        private Long totalItems;
        private Long fileCount;
        private Long folderCount;
        private Long expiredCount;
        private Long recoverableCount;
        private String oldestItemDate;
        private String newestItemDate;
        
        // 构造函数和getter/setter
        public RecycleBinStats() {}
        
        public RecycleBinStats(Long totalItems, Long fileCount, Long folderCount, 
                             Long expiredCount, Long recoverableCount) {
            this.totalItems = totalItems;
            this.fileCount = fileCount;
            this.folderCount = folderCount;
            this.expiredCount = expiredCount;
            this.recoverableCount = recoverableCount;
        }
        
        // getters and setters
        public Long getTotalItems() { return totalItems; }
        public void setTotalItems(Long totalItems) { this.totalItems = totalItems; }
        
        public Long getFileCount() { return fileCount; }
        public void setFileCount(Long fileCount) { this.fileCount = fileCount; }
        
        public Long getFolderCount() { return folderCount; }
        public void setFolderCount(Long folderCount) { this.folderCount = folderCount; }
        
        public Long getExpiredCount() { return expiredCount; }
        public void setExpiredCount(Long expiredCount) { this.expiredCount = expiredCount; }
        
        public Long getRecoverableCount() { return recoverableCount; }
        public void setRecoverableCount(Long recoverableCount) { this.recoverableCount = recoverableCount; }
        
        public String getOldestItemDate() { return oldestItemDate; }
        public void setOldestItemDate(String oldestItemDate) { this.oldestItemDate = oldestItemDate; }
        
        public String getNewestItemDate() { return newestItemDate; }
        public void setNewestItemDate(String newestItemDate) { this.newestItemDate = newestItemDate; }
    }
    
    /**
     * 即将过期项目DTO
     */
    class ExpiringItem {
        private Long id;
        private String itemName;
        private String itemType;
        private String expireTime;
        private Long hoursLeft;
        
        // 构造函数和getter/setter
        public ExpiringItem() {}
        
        public ExpiringItem(Long id, String itemName, String itemType, String expireTime, Long hoursLeft) {
            this.id = id;
            this.itemName = itemName;
            this.itemType = itemType;
            this.expireTime = expireTime;
            this.hoursLeft = hoursLeft;
        }
        
        // getters and setters
        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        
        public String getItemName() { return itemName; }
        public void setItemName(String itemName) { this.itemName = itemName; }
        
        public String getItemType() { return itemType; }
        public void setItemType(String itemType) { this.itemType = itemType; }
        
        public String getExpireTime() { return expireTime; }
        public void setExpireTime(String expireTime) { this.expireTime = expireTime; }
        
        public Long getHoursLeft() { return hoursLeft; }
        public void setHoursLeft(Long hoursLeft) { this.hoursLeft = hoursLeft; }
    }
    
    /**
     * 项目类型枚举
     */
    enum ItemType {
        FILE, FOLDER
    }
    
    /**
     * 回收站项目信息DTO
     */
    class RecycleItemInfo {
        private Long id;
        private Long itemId;
        private String itemType;
        private String originalName;
        private String originalPath;
        private Long fileSize;
        private String fileType;
        private String deletedByName;
        private LocalDateTime deletedAt;
        private LocalDateTime expireAt;
        private Boolean isRecoverable;
        private String deleteReason;
        
        public RecycleItemInfo() {}
        
        // getters and setters
        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        
        public Long getItemId() { return itemId; }
        public void setItemId(Long itemId) { this.itemId = itemId; }
        
        public String getItemType() { return itemType; }
        public void setItemType(String itemType) { this.itemType = itemType; }
        
        public String getOriginalName() { return originalName; }
        public void setOriginalName(String originalName) { this.originalName = originalName; }
        
        public String getOriginalPath() { return originalPath; }
        public void setOriginalPath(String originalPath) { this.originalPath = originalPath; }
        
        public Long getFileSize() { return fileSize; }
        public void setFileSize(Long fileSize) { this.fileSize = fileSize; }
        
        public String getFileType() { return fileType; }
        public void setFileType(String fileType) { this.fileType = fileType; }
        
        public String getDeletedByName() { return deletedByName; }
        public void setDeletedByName(String deletedByName) { this.deletedByName = deletedByName; }
        
        public LocalDateTime getDeletedAt() { return deletedAt; }
        public void setDeletedAt(LocalDateTime deletedAt) { this.deletedAt = deletedAt; }
        
        public LocalDateTime getExpireAt() { return expireAt; }
        public void setExpireAt(LocalDateTime expireAt) { this.expireAt = expireAt; }
        
        public Boolean getIsRecoverable() { return isRecoverable; }
        public void setIsRecoverable(Boolean isRecoverable) { this.isRecoverable = isRecoverable; }
        
        public String getDeleteReason() { return deleteReason; }
        public void setDeleteReason(String deleteReason) { this.deleteReason = deleteReason; }
    }
    
    /**
     * 批量操作结果DTO
     */
    class BatchOperationResult {
        private Integer totalProcessed;
        private Integer successCount;
        private Integer failureCount;
        private List<String> errorMessages;
        
        // 构造函数和getter/setter
        public BatchOperationResult() {
            this.errorMessages = new java.util.ArrayList<>();
        }
        
        public BatchOperationResult(Integer totalProcessed, Integer successCount, Integer failureCount) {
            this.totalProcessed = totalProcessed;
            this.successCount = successCount;
            this.failureCount = failureCount;
            this.errorMessages = new java.util.ArrayList<>();
        }
        
        // getters and setters
        public Integer getTotalProcessed() { return totalProcessed; }
        public void setTotalProcessed(Integer totalProcessed) { this.totalProcessed = totalProcessed; }
        
        public Integer getSuccessCount() { return successCount; }
        public void setSuccessCount(Integer successCount) { this.successCount = successCount; }
        
        public Integer getFailureCount() { return failureCount; }
        public void setFailureCount(Integer failureCount) { this.failureCount = failureCount; }
        
        public List<String> getErrorMessages() { return errorMessages; }
        public void setErrorMessages(List<String> errorMessages) { this.errorMessages = errorMessages; }
        
        public void addErrorMessage(String message) {
            this.errorMessages.add(message);
        }
    }
}