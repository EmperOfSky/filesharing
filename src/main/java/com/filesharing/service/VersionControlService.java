package com.filesharing.service;

import com.filesharing.entity.FileEntity;
import com.filesharing.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 文件版本控制服务接口
 */
public interface VersionControlService {
    
    // ==================== 版本创建 ====================
    
    /**
     * 创建新版本
     */
    FileVersionInfo createVersion(Long fileId, MultipartFile file, String description, 
                                String versionTag, User user);
    
    /**
     * 自动创建版本（当文件被修改时）
     */
    FileVersionInfo autoCreateVersion(FileEntity file, User user);
    
    /**
     * 从现有文件创建版本
     */
    FileVersionInfo createVersionFromExisting(Long fileId, String description, 
                                            String versionTag, User user);
    
    // ==================== 版本管理 ====================
    
    /**
     * 获取文件的所有版本
     */
    Page<FileVersionInfo> getFileVersions(Long fileId, int page, int size);
    
    /**
     * 获取当前版本
     */
    FileVersionInfo getCurrentVersion(Long fileId);
    
    /**
     * 根据版本号获取特定版本
     */
    FileVersionInfo getVersionByNumber(Long fileId, Integer versionNumber);
    
    /**
     * 恢复到指定版本
     */
    RestoreResult restoreToVersion(Long fileId, Integer versionNumber, String reason, User user);
    
    /**
     * 删除版本
     */
    void deleteVersion(Long fileId, Integer versionNumber, User user);
    
    /**
     * 批量删除旧版本
     */
    BatchDeleteResult batchDeleteOldVersions(Long fileId, Integer keepVersions, User user);
    
    // ==================== 版本比较 ====================
    
    /**
     * 比较两个版本
     */
    VersionDiff compareVersions(Long fileId, Integer version1, Integer version2);
    
    /**
     * 获取版本变更历史
     */
    List<VersionChange> getVersionHistory(Long fileId);
    
    // ==================== 版本查询 ====================
    
    /**
     * 根据标签查找版本
     */
    Page<FileVersionInfo> getVersionsByTag(Long fileId, String tag, int page, int size);
    
    /**
     * 根据时间范围查找版本
     */
    Page<FileVersionInfo> getVersionsByTimeRange(Long fileId, String startTime, 
                                               String endTime, int page, int size);
    
    /**
     * 获取版本统计信息
     */
    VersionStats getVersionStatistics(Long fileId);
    
    // ==================== 版本策略 ====================
    
    /**
     * 设置版本控制策略
     */
    void setVersionControlPolicy(Long fileId, VersionPolicy policy);
    
    /**
     * 获取版本控制策略
     */
    VersionPolicy getVersionControlPolicy(Long fileId);
    
    // ==================== DTO类定义 ====================
    
    /**
     * 文件版本信息
     */
    class FileVersionInfo {
        private Long id;
        private Integer versionNumber;
        private String versionDescription;
        private String storageName;
        private Long fileSize;
        private String contentType;
        private String md5Hash;
        private String modifiedByName;
        private String modifiedAt;
        private Boolean isCurrent;
        private String versionTag;
        private String notes;
        
        public FileVersionInfo() {}
        
        // getters and setters
        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        
        public Integer getVersionNumber() { return versionNumber; }
        public void setVersionNumber(Integer versionNumber) { this.versionNumber = versionNumber; }
        
        public String getVersionDescription() { return versionDescription; }
        public void setVersionDescription(String versionDescription) { this.versionDescription = versionDescription; }
        
        public String getStorageName() { return storageName; }
        public void setStorageName(String storageName) { this.storageName = storageName; }
        
        public Long getFileSize() { return fileSize; }
        public void setFileSize(Long fileSize) { this.fileSize = fileSize; }
        
        public String getContentType() { return contentType; }
        public void setContentType(String contentType) { this.contentType = contentType; }
        
        public String getMd5Hash() { return md5Hash; }
        public void setMd5Hash(String md5Hash) { this.md5Hash = md5Hash; }
        
        public String getModifiedByName() { return modifiedByName; }
        public void setModifiedByName(String modifiedByName) { this.modifiedByName = modifiedByName; }
        
        public String getModifiedAt() { return modifiedAt; }
        public void setModifiedAt(String modifiedAt) { this.modifiedAt = modifiedAt; }
        
        public Boolean getIsCurrent() { return isCurrent; }
        public void setIsCurrent(Boolean isCurrent) { this.isCurrent = isCurrent; }
        
        public String getVersionTag() { return versionTag; }
        public void setVersionTag(String versionTag) { this.versionTag = versionTag; }
        
        public String getNotes() { return notes; }
        public void setNotes(String notes) { this.notes = notes; }
    }
    
    /**
     * 恢复结果
     */
    class RestoreResult {
        private Boolean success;
        private String message;
        private Integer restoredVersion;
        private Long newFileVersionId;
        
        public RestoreResult() {}
        
        public RestoreResult(Boolean success, String message) {
            this.success = success;
            this.message = message;
        }
        
        // getters and setters
        public Boolean getSuccess() { return success; }
        public void setSuccess(Boolean success) { this.success = success; }
        
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        
        public Integer getRestoredVersion() { return restoredVersion; }
        public void setRestoredVersion(Integer restoredVersion) { this.restoredVersion = restoredVersion; }
        
        public Long getNewFileVersionId() { return newFileVersionId; }
        public void setNewFileVersionId(Long newFileVersionId) { this.newFileVersionId = newFileVersionId; }
    }
    
    /**
     * 批量删除结果
     */
    class BatchDeleteResult {
        private Integer totalVersions;
        private Integer deletedCount;
        private Integer retainedCount;
        private String message;
        
        public BatchDeleteResult() {}
        
        public BatchDeleteResult(Integer totalVersions, Integer deletedCount, Integer retainedCount) {
            this.totalVersions = totalVersions;
            this.deletedCount = deletedCount;
            this.retainedCount = retainedCount;
        }
        
        // getters and setters
        public Integer getTotalVersions() { return totalVersions; }
        public void setTotalVersions(Integer totalVersions) { this.totalVersions = totalVersions; }
        
        public Integer getDeletedCount() { return deletedCount; }
        public void setDeletedCount(Integer deletedCount) { this.deletedCount = deletedCount; }
        
        public Integer getRetainedCount() { return retainedCount; }
        public void setRetainedCount(Integer retainedCount) { this.retainedCount = retainedCount; }
        
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
    }
    
    /**
     * 版本差异
     */
    class VersionDiff {
        private Integer version1;
        private Integer version2;
        private Long sizeDifference;
        private String contentTypeChanged;
        private List<String> metadataChanges;
        private String summary;
        
        public VersionDiff() {
            this.metadataChanges = new java.util.ArrayList<>();
        }
        
        // getters and setters
        public Integer getVersion1() { return version1; }
        public void setVersion1(Integer version1) { this.version1 = version1; }
        
        public Integer getVersion2() { return version2; }
        public void setVersion2(Integer version2) { this.version2 = version2; }
        
        public Long getSizeDifference() { return sizeDifference; }
        public void setSizeDifference(Long sizeDifference) { this.sizeDifference = sizeDifference; }
        
        public String getContentTypeChanged() { return contentTypeChanged; }
        public void setContentTypeChanged(String contentTypeChanged) { this.contentTypeChanged = contentTypeChanged; }
        
        public List<String> getMetadataChanges() { return metadataChanges; }
        public void setMetadataChanges(List<String> metadataChanges) { this.metadataChanges = metadataChanges; }
        
        public String getSummary() { return summary; }
        public void setSummary(String summary) { this.summary = summary; }
    }
    
    /**
     * 版本变更记录
     */
    class VersionChange {
        private Integer versionNumber;
        private String description;
        private String modifiedByName;
        private String modifiedAt;
        private String changeType;
        private Long sizeChange;
        
        public VersionChange() {}
        
        // getters and setters
        public Integer getVersionNumber() { return versionNumber; }
        public void setVersionNumber(Integer versionNumber) { this.versionNumber = versionNumber; }
        
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        
        public String getModifiedByName() { return modifiedByName; }
        public void setModifiedByName(String modifiedByName) { this.modifiedByName = modifiedByName; }
        
        public String getModifiedAt() { return modifiedAt; }
        public void setModifiedAt(String modifiedAt) { this.modifiedAt = modifiedAt; }
        
        public String getChangeType() { return changeType; }
        public void setChangeType(String changeType) { this.changeType = changeType; }
        
        public Long getSizeChange() { return sizeChange; }
        public void setSizeChange(Long sizeChange) { this.sizeChange = sizeChange; }
    }
    
    /**
     * 版本统计信息
     */
    class VersionStats {
        private Long totalVersions;
        private Integer currentVersion;
        private Long totalSize;
        private List<TagStat> tagStats;
        private List<MonthlyStat> monthlyStats;
        
        public VersionStats() {
            this.tagStats = new java.util.ArrayList<>();
            this.monthlyStats = new java.util.ArrayList<>();
        }
        
        /**
         * 标签统计
         */
        public static class TagStat {
            private String tag;
            private Long count;
            private Double percentage;
            
            public TagStat() {}
            
            public TagStat(String tag, Long count) {
                this.tag = tag;
                this.count = count;
            }
            
            // getters and setters
            public String getTag() { return tag; }
            public void setTag(String tag) { this.tag = tag; }
            
            public Long getCount() { return count; }
            public void setCount(Long count) { this.count = count; }
            
            public Double getPercentage() { return percentage; }
            public void setPercentage(Double percentage) { this.percentage = percentage; }
        }
        
        /**
         * 月度统计
         */
        public static class MonthlyStat {
            private String month;
            private Long versionCount;
            private Long totalSize;
            
            public MonthlyStat() {}
            
            public MonthlyStat(String month, Long versionCount, Long totalSize) {
                this.month = month;
                this.versionCount = versionCount;
                this.totalSize = totalSize;
            }
            
            // getters and setters
            public String getMonth() { return month; }
            public void setMonth(String month) { this.month = month; }
            
            public Long getVersionCount() { return versionCount; }
            public void setVersionCount(Long versionCount) { this.versionCount = versionCount; }
            
            public Long getTotalSize() { return totalSize; }
            public void setTotalSize(Long totalSize) { this.totalSize = totalSize; }
        }
        
        // getters and setters
        public Long getTotalVersions() { return totalVersions; }
        public void setTotalVersions(Long totalVersions) { this.totalVersions = totalVersions; }
        
        public Integer getCurrentVersion() { return currentVersion; }
        public void setCurrentVersion(Integer currentVersion) { this.currentVersion = currentVersion; }
        
        public Long getTotalSize() { return totalSize; }
        public void setTotalSize(Long totalSize) { this.totalSize = totalSize; }
        
        public List<TagStat> getTagStats() { return tagStats; }
        public void setTagStats(List<TagStat> tagStats) { this.tagStats = tagStats; }
        
        public List<MonthlyStat> getMonthlyStats() { return monthlyStats; }
        public void setMonthlyStats(List<MonthlyStat> monthlyStats) { this.monthlyStats = monthlyStats; }
    }
    
    /**
     * 版本控制策略
     */
    class VersionPolicy {
        private Boolean enabled;
        private Integer maxVersions;
        private String retentionPolicy; // KEEP_ALL, KEEP_LATEST_N, TIME_BASED
        private Integer retentionDays;
        private Boolean autoVersionOnModify;
        private List<String> excludedFileTypes;
        
        public VersionPolicy() {
            this.excludedFileTypes = new java.util.ArrayList<>();
        }
        
        // getters and setters
        public Boolean getEnabled() { return enabled; }
        public void setEnabled(Boolean enabled) { this.enabled = enabled; }
        
        public Integer getMaxVersions() { return maxVersions; }
        public void setMaxVersions(Integer maxVersions) { this.maxVersions = maxVersions; }
        
        public String getRetentionPolicy() { return retentionPolicy; }
        public void setRetentionPolicy(String retentionPolicy) { this.retentionPolicy = retentionPolicy; }
        
        public Integer getRetentionDays() { return retentionDays; }
        public void setRetentionDays(Integer retentionDays) { this.retentionDays = retentionDays; }
        
        public Boolean getAutoVersionOnModify() { return autoVersionOnModify; }
        public void setAutoVersionOnModify(Boolean autoVersionOnModify) { this.autoVersionOnModify = autoVersionOnModify; }
        
        public List<String> getExcludedFileTypes() { return excludedFileTypes; }
        public void setExcludedFileTypes(List<String> excludedFileTypes) { this.excludedFileTypes = excludedFileTypes; }
    }
}