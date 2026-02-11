package com.filesharing.service;

import com.filesharing.dto.PreviewResponse;
import com.filesharing.entity.FileEntity;
import com.filesharing.entity.User;
import org.springframework.core.io.Resource;

import java.util.List;

/**
 * 文件预览服务接口
 */
public interface PreviewService {
    
    /**
     * 预览文件
     */
    PreviewResponse previewFile(Long fileId, User user, String deviceType, String userAgent, String ipAddress);
    
    /**
     * 获取文件预览内容
     */
    Resource getPreviewContent(Long fileId, String previewType);
    
    /**
     * 获取文本文件预览内容
     */
    String getTextPreview(FileEntity file);
    
    /**
     * 获取PDF文件预览内容
     */
    byte[] getPdfPreview(FileEntity file);
    
    /**
     * 获取图片预览内容
     */
    Resource getImagePreview(FileEntity file, Integer width, Integer height);
    
    /**
     * 获取Office文档预览内容（转换为PDF）
     */
    byte[] getOfficePreview(FileEntity file);
    
    /**
     * 获取音频文件预览信息
     */
    PreviewResponse getAudioPreview(FileEntity file);
    
    /**
     * 获取视频文件预览信息
     */
    PreviewResponse getVideoPreview(FileEntity file);
    
    /**
     * 获取文件预览统计
     */
    FilePreviewStatistics getFilePreviewStatistics(Long fileId);
    
    /**
     * 获取用户预览统计
     */
    UserPreviewStatistics getUserPreviewStatistics(Long userId);
    
    /**
     * 获取热门预览文件
     */
    List<PreviewResponse> getPopularPreviews(int limit);
    
    /**
     * 记录预览日志
     */
    void recordPreview(PreviewRecordDto previewRecord);
    
    /**
     * 预览记录DTO内部类
     */
    class PreviewRecordDto {
        private Long fileId;
        private Long userId;
        private String previewType;
        private String deviceType;
        private String userAgent;
        private String ipAddress;
        private Integer durationSeconds;
        private Boolean isSuccess;
        private String errorMessage;
        
        // 构造函数、getter和setter
        public PreviewRecordDto() {}
        
        public PreviewRecordDto(Long fileId, Long userId, String previewType, String deviceType, 
                              String userAgent, String ipAddress) {
            this.fileId = fileId;
            this.userId = userId;
            this.previewType = previewType;
            this.deviceType = deviceType;
            this.userAgent = userAgent;
            this.ipAddress = ipAddress;
            this.isSuccess = true;
        }
        
        // getters and setters
        public Long getFileId() { return fileId; }
        public void setFileId(Long fileId) { this.fileId = fileId; }
        
        public Long getUserId() { return userId; }
        public void setUserId(Long userId) { this.userId = userId; }
        
        public String getPreviewType() { return previewType; }
        public void setPreviewType(String previewType) { this.previewType = previewType; }
        
        public String getDeviceType() { return deviceType; }
        public void setDeviceType(String deviceType) { this.deviceType = deviceType; }
        
        public String getUserAgent() { return userAgent; }
        public void setUserAgent(String userAgent) { this.userAgent = userAgent; }
        
        public String getIpAddress() { return ipAddress; }
        public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }
        
        public Integer getDurationSeconds() { return durationSeconds; }
        public void setDurationSeconds(Integer durationSeconds) { this.durationSeconds = durationSeconds; }
        
        public Boolean getIsSuccess() { return isSuccess; }
        public void setIsSuccess(Boolean isSuccess) { this.isSuccess = isSuccess; }
        
        public String getErrorMessage() { return errorMessage; }
        public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    }
    
    /**
     * 文件预览统计内部类
     */
    class FilePreviewStatistics {
        private Long totalPreviews;
        private Long uniqueUsers;
        private Double averageDuration;
        private List<DailyPreviewStat> dailyStats;
        
        // 构造函数、getter和setter
        public FilePreviewStatistics() {}
        
        public FilePreviewStatistics(Long totalPreviews, Long uniqueUsers, Double averageDuration) {
            this.totalPreviews = totalPreviews;
            this.uniqueUsers = uniqueUsers;
            this.averageDuration = averageDuration;
        }
        
        // 内部类：每日预览统计
        public static class DailyPreviewStat {
            private String date;
            private Long previewCount;
            
            public DailyPreviewStat() {}
            
            public DailyPreviewStat(String date, Long previewCount) {
                this.date = date;
                this.previewCount = previewCount;
            }
            
            public String getDate() { return date; }
            public void setDate(String date) { this.date = date; }
            
            public Long getPreviewCount() { return previewCount; }
            public void setPreviewCount(Long previewCount) { this.previewCount = previewCount; }
        }
        
        // getters and setters
        public Long getTotalPreviews() { return totalPreviews; }
        public void setTotalPreviews(Long totalPreviews) { this.totalPreviews = totalPreviews; }
        
        public Long getUniqueUsers() { return uniqueUsers; }
        public void setUniqueUsers(Long uniqueUsers) { this.uniqueUsers = uniqueUsers; }
        
        public Double getAverageDuration() { return averageDuration; }
        public void setAverageDuration(Double averageDuration) { this.averageDuration = averageDuration; }
        
        public List<DailyPreviewStat> getDailyStats() { return dailyStats; }
        public void setDailyStats(List<DailyPreviewStat> dailyStats) { this.dailyStats = dailyStats; }
    }
    
    /**
     * 用户预览统计内部类
     */
    class UserPreviewStatistics {
        private Long totalPreviews;
        private Long favoriteFileTypeCount;
        private List<FileTypeStat> fileTypeStats;
        private List<DailyActivityStat> dailyActivity;
        
        // 构造函数、getter和setter
        public UserPreviewStatistics() {}
        
        public UserPreviewStatistics(Long totalPreviews, Long favoriteFileTypeCount) {
            this.totalPreviews = totalPreviews;
            this.favoriteFileTypeCount = favoriteFileTypeCount;
        }
        
        // 内部类：文件类型统计
        public static class FileTypeStat {
            private String fileType;
            private Long count;
            private Double percentage;
            
            public FileTypeStat() {}
            
            public FileTypeStat(String fileType, Long count, Double percentage) {
                this.fileType = fileType;
                this.count = count;
                this.percentage = percentage;
            }
            
            public String getFileType() { return fileType; }
            public void setFileType(String fileType) { this.fileType = fileType; }
            
            public Long getCount() { return count; }
            public void setCount(Long count) { this.count = count; }
            
            public Double getPercentage() { return percentage; }
            public void setPercentage(Double percentage) { this.percentage = percentage; }
        }
        
        // 内部类：每日活动统计
        public static class DailyActivityStat {
            private String date;
            private Long previewCount;
            private Long fileCount;
            
            public DailyActivityStat() {}
            
            public DailyActivityStat(String date, Long previewCount, Long fileCount) {
                this.date = date;
                this.previewCount = previewCount;
                this.fileCount = fileCount;
            }
            
            public String getDate() { return date; }
            public void setDate(String date) { this.date = date; }
            
            public Long getPreviewCount() { return previewCount; }
            public void setPreviewCount(Long previewCount) { this.previewCount = previewCount; }
            
            public Long getFileCount() { return fileCount; }
            public void setFileCount(Long fileCount) { this.fileCount = fileCount; }
        }
        
        // getters and setters
        public Long getTotalPreviews() { return totalPreviews; }
        public void setTotalPreviews(Long totalPreviews) { this.totalPreviews = totalPreviews; }
        
        public Long getFavoriteFileTypeCount() { return favoriteFileTypeCount; }
        public void setFavoriteFileTypeCount(Long favoriteFileTypeCount) { this.favoriteFileTypeCount = favoriteFileTypeCount; }
        
        public List<FileTypeStat> getFileTypeStats() { return fileTypeStats; }
        public void setFileTypeStats(List<FileTypeStat> fileTypeStats) { this.fileTypeStats = fileTypeStats; }
        
        public List<DailyActivityStat> getDailyActivity() { return dailyActivity; }
        public void setDailyActivity(List<DailyActivityStat> dailyActivity) { this.dailyActivity = dailyActivity; }
    }
}