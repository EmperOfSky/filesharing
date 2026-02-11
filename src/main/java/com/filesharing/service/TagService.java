package com.filesharing.service;

import com.filesharing.entity.Tag;
import com.filesharing.entity.User;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.Set;

/**
 * 标签服务接口
 */
public interface TagService {
    
    // ==================== 标签管理 ====================
    
    /**
     * 创建标签
     */
    Tag createTag(TagCreateRequest request, User user);
    
    /**
     * 更新标签
     */
    Tag updateTag(Long tagId, TagUpdateRequest request, User user);
    
    /**
     * 删除标签
     */
    void deleteTag(Long tagId, User user);
    
    /**
     * 获取用户标签列表
     */
    Page<TagInfo> getUserTags(User user, String keyword, boolean includeSystem, int page, int size);
    
    /**
     * 获取系统标签
     */
    List<TagInfo> getSystemTags();
    
    /**
     * 获取热门标签
     */
    Page<TagInfo> getPopularTags(User user, int page, int size);
    
    /**
     * 获取标签云数据
     */
    List<TagCloudItem> getTagCloud(User user, int limit);
    
    // ==================== 文件标签操作 ====================
    
    /**
     * 为文件添加标签
     */
    void addTagToFile(Long fileId, Long tagId, User user);
    
    /**
     * 为文件批量添加标签
     */
    BatchTagResult batchAddTagsToFile(Long fileId, List<Long> tagIds, User user);
    
    /**
     * 从文件移除标签
     */
    void removeTagFromFile(Long fileId, Long tagId, User user);
    
    /**
     * 清除文件的所有标签
     */
    void clearFileTags(Long fileId, User user);
    
    /**
     * 获取文件的标签
     */
    List<TagInfo> getFileTags(Long fileId);
    
    /**
     * 获取具有相同标签的文件
     */
    List<FileTagInfo> getFilesWithSameTags(Long fileId, int limit);
    
    // ==================== 文件夹标签操作 ====================
    
    /**
     * 为文件夹添加标签
     */
    void addTagToFolder(Long folderId, Long tagId, User user);
    
    /**
     * 从文件夹移除标签
     */
    void removeTagFromFolder(Long folderId, Long tagId, User user);
    
    /**
     * 获取文件夹的标签
     */
    List<TagInfo> getFolderTags(Long folderId);
    
    // ==================== 标签搜索 ====================
    
    /**
     * 根据标签搜索文件
     */
    Page<FileTagInfo> searchFilesByTags(List<Long> tagIds, User user, int page, int size);
    
    /**
     * 根据标签组合搜索文件夹
     */
    Page<FolderTagInfo> searchFoldersByTags(List<Long> tagIds, User user, int page, int size);
    
    /**
     * 获取标签使用统计
     */
    List<TagUsageStat> getTagUsageStatistics(User user);
    
    // ==================== DTO类定义 ====================
    
    /**
     * 标签创建请求
     */
    class TagCreateRequest {
        private String tagName;
        private String color;
        private String description;
        
        public TagCreateRequest() {}
        
        public TagCreateRequest(String tagName, String color, String description) {
            this.tagName = tagName;
            this.color = color;
            this.description = description;
        }
        
        // getters and setters
        public String getTagName() { return tagName; }
        public void setTagName(String tagName) { this.tagName = tagName; }
        
        public String getColor() { return color; }
        public void setColor(String color) { this.color = color; }
        
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
    }
    
    /**
     * 标签更新请求
     */
    class TagUpdateRequest {
        private String tagName;
        private String color;
        private String description;
        
        public TagUpdateRequest() {}
        
        // getters and setters
        public String getTagName() { return tagName; }
        public void setTagName(String tagName) { this.tagName = tagName; }
        
        public String getColor() { return color; }
        public void setColor(String color) { this.color = color; }
        
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
    }
    
    /**
     * 标签信息
     */
    class TagInfo {
        private Long id;
        private String tagName;
        private String color;
        private String description;
        private Boolean isSystemTag;
        private Integer usageCount;
        private String createdAt;
        
        public TagInfo() {}
        
        public TagInfo(Long id, String tagName, String color, String description, 
                      Boolean isSystemTag, Integer usageCount, String createdAt) {
            this.id = id;
            this.tagName = tagName;
            this.color = color;
            this.description = description;
            this.isSystemTag = isSystemTag;
            this.usageCount = usageCount;
            this.createdAt = createdAt;
        }
        
        // getters and setters
        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        
        public String getTagName() { return tagName; }
        public void setTagName(String tagName) { this.tagName = tagName; }
        
        public String getColor() { return color; }
        public void setColor(String color) { this.color = color; }
        
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        
        public Boolean getIsSystemTag() { return isSystemTag; }
        public void setIsSystemTag(Boolean isSystemTag) { this.isSystemTag = isSystemTag; }
        
        public Integer getUsageCount() { return usageCount; }
        public void setUsageCount(Integer usageCount) { this.usageCount = usageCount; }
        
        public String getCreatedAt() { return createdAt; }
        public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
    }
    
    /**
     * 标签云项目
     */
    class TagCloudItem {
        private String tagName;
        private String color;
        private Integer usageCount;
        private Integer fontSize;
        
        public TagCloudItem() {}
        
        public TagCloudItem(String tagName, String color, Integer usageCount) {
            this.tagName = tagName;
            this.color = color;
            this.usageCount = usageCount;
            // 根据使用次数计算字体大小
            this.fontSize = Math.min(24, 12 + usageCount / 2);
        }
        
        // getters and setters
        public String getTagName() { return tagName; }
        public void setTagName(String tagName) { this.tagName = tagName; }
        
        public String getColor() { return color; }
        public void setColor(String color) { this.color = color; }
        
        public Integer getUsageCount() { return usageCount; }
        public void setUsageCount(Integer usageCount) { this.usageCount = usageCount; }
        
        public Integer getFontSize() { return fontSize; }
        public void setFontSize(Integer fontSize) { this.fontSize = fontSize; }
    }
    
    /**
     * 文件标签信息
     */
    class FileTagInfo {
        private Long fileId;
        private String fileName;
        private String fileType;
        private Long fileSize;
        private List<TagInfo> tags;
        private String createdAt;
        
        public FileTagInfo() {}
        
        // getters and setters
        public Long getFileId() { return fileId; }
        public void setFileId(Long fileId) { this.fileId = fileId; }
        
        public String getFileName() { return fileName; }
        public void setFileName(String fileName) { this.fileName = fileName; }
        
        public String getFileType() { return fileType; }
        public void setFileType(String fileType) { this.fileType = fileType; }
        
        public Long getFileSize() { return fileSize; }
        public void setFileSize(Long fileSize) { this.fileSize = fileSize; }
        
        public List<TagInfo> getTags() { return tags; }
        public void setTags(List<TagInfo> tags) { this.tags = tags; }
        
        public String getCreatedAt() { return createdAt; }
        public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
    }
    
    /**
     * 文件夹标签信息
     */
    class FolderTagInfo {
        private Long folderId;
        private String folderName;
        private String folderPath;
        private List<TagInfo> tags;
        private String createdAt;
        
        public FolderTagInfo() {}
        
        // getters and setters
        public Long getFolderId() { return folderId; }
        public void setFolderId(Long folderId) { this.folderId = folderId; }
        
        public String getFolderName() { return folderName; }
        public void setFolderName(String folderName) { this.folderName = folderName; }
        
        public String getFolderPath() { return folderPath; }
        public void setFolderPath(String folderPath) { this.folderPath = folderPath; }
        
        public List<TagInfo> getTags() { return tags; }
        public void setTags(List<TagInfo> tags) { this.tags = tags; }
        
        public String getCreatedAt() { return createdAt; }
        public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
    }
    
    /**
     * 批量标签操作结果
     */
    class BatchTagResult {
        private Integer totalTags;
        private Integer successCount;
        private Integer failureCount;
        private List<String> errorMessages;
        
        public BatchTagResult() {
            this.errorMessages = new java.util.ArrayList<>();
        }
        
        public BatchTagResult(Integer totalTags, Integer successCount, Integer failureCount) {
            this.totalTags = totalTags;
            this.successCount = successCount;
            this.failureCount = failureCount;
            this.errorMessages = new java.util.ArrayList<>();
        }
        
        // getters and setters
        public Integer getTotalTags() { return totalTags; }
        public void setTotalTags(Integer totalTags) { this.totalTags = totalTags; }
        
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
    
    /**
     * 标签使用统计
     */
    class TagUsageStat {
        private String tagName;
        private String color;
        private Integer fileCount;
        private Integer folderCount;
        private Integer totalCount;
        private Double percentage;
        
        public TagUsageStat() {}
        
        public TagUsageStat(String tagName, String color, Integer fileCount, Integer folderCount) {
            this.tagName = tagName;
            this.color = color;
            this.fileCount = fileCount;
            this.folderCount = folderCount;
            this.totalCount = fileCount + folderCount;
        }
        
        // getters and setters
        public String getTagName() { return tagName; }
        public void setTagName(String tagName) { this.tagName = tagName; }
        
        public String getColor() { return color; }
        public void setColor(String color) { this.color = color; }
        
        public Integer getFileCount() { return fileCount; }
        public void setFileCount(Integer fileCount) { this.fileCount = fileCount; }
        
        public Integer getFolderCount() { return folderCount; }
        public void setFolderCount(Integer folderCount) { this.folderCount = folderCount; }
        
        public Integer getTotalCount() { return totalCount; }
        public void setTotalCount(Integer totalCount) { this.totalCount = totalCount; }
        
        public Double getPercentage() { return percentage; }
        public void setPercentage(Double percentage) { this.percentage = percentage; }
    }
}