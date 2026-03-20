package com.filesharing.service;

import com.filesharing.entity.FileEntity;
import com.filesharing.entity.User;
import org.springframework.data.domain.Page;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 统计服务接口
 */
public interface StatisticsService {
    
    // ==================== 文件统计相关 ====================
    
    /**
     * 更新文件统计信息
     */
    void updateFileStatistics(FileEntity file, StatisticAction action);
    
    /**
     * 获取文件详细统计
     */
    FileStatsDetail getFileStatistics(Long fileId);
    
    /**
     * 获取热门文件排行榜
     */
    Page<FileRanking> getPopularFiles(int page, int size);
    
    /**
     * 获取最新上传文件
     */
    Page<FileBasicInfo> getLatestFiles(int page, int size);
    
    /**
     * 获取最活跃文件
     */
    Page<FileRanking> getMostActiveFiles(int page, int size);
    
    /**
     * 获取文件类型分布统计
     */
    List<FileTypeDistribution> getFileTypeDistribution();
    
    /**
     * 获取文件大小分布统计
     */
    List<FileSizeDistribution> getFileSizeDistribution();
    
    /**
     * 获取零下载文件列表
     */
    Page<FileBasicInfo> getZeroDownloadFiles(int page, int size);
    
    // ==================== 用户行为统计相关 ====================
    
    /**
     * 更新用户行为统计
     */
    void updateUserBehaviorStatistics(User user, BehaviorAction action, Object... params);
    
    /**
     * 获取用户详细行为统计
     */
    UserBehaviorDetail getUserBehaviorStatistics(Long userId);
    
    /**
     * 获取最活跃用户排行榜
     */
    Page<UserRanking> getMostActiveUsers(int page, int size);
    
    /**
     * 获取高贡献用户（上传最多）
     */
    Page<UserRanking> getTopContributors(int page, int size);
    
    /**
     * 获取存储使用大户
     */
    Page<UserRanking> getStorageHeavyUsers(int page, int size);
    
    /**
     * 获取用户等级分布
     */
    List<UserLevelDistribution> getUserLevelDistribution();
    
    /**
     * 获取用户增长趋势
     */
    List<DailyTrend> getUserGrowthTrend(int days);
    
    /**
     * 获取用户活跃度趋势
     */
    List<DailyTrend> getUserActivityTrend(int days);
    
    // ==================== 系统统计相关 ====================
    
    /**
     * 收集并保存系统统计
     */
    void collectSystemStatistics();
    
    /**
     * 获取系统概览统计
     */
    SystemOverview getSystemOverview();
    
    /**
     * 获取系统负载趋势
     */
    List<SystemLoadTrend> getSystemLoadTrend(int days);
    
    /**
     * 获取系统健康报告
     */
    SystemHealthReport getSystemHealthReport(int days);
    
    /**
     * 获取性能指标趋势
     */
    List<PerformanceTrend> getPerformanceTrend(int days);
    
    /**
     * 获取异常统计信息
     */
    Page<SystemAlert> getAbnormalStatistics(int page, int size);
    
    // ==================== 统计动作枚举 ====================
    
    enum StatisticAction {
        DOWNLOAD,       // 下载
        PREVIEW,        // 预览
        SHARE,          // 分享
        RATE            // 评分
    }
    
    enum BehaviorAction {
        UPLOAD,         // 上传文件
        DOWNLOAD,       // 下载文件
        PREVIEW,        // 预览文件
        CREATE_FOLDER,  // 创建文件夹
        SHARE,          // 分享文件
        LOGIN,          // 登录
        STORAGE_CHANGE  // 存储变化
    }
    
    // ==================== DTO类定义 ====================
    
    /**
     * 文件统计详情
     */
    class FileStatsDetail {
        private Long fileId;
        private String fileName;
        private String fileType;
        private Long totalDownloads;
        private Long totalPreviews;
        private Long totalShares;
        private Long uniqueDownloaders;
        private Long uniquePreviewers;
        private Double averageRating;
        private Double popularityScore;
        private String lastDownloadTime;
        private String lastPreviewTime;
        private DailyStats dailyStats;
        
        // 构造函数和getter/setter省略
        public FileStatsDetail() {}
        
        // getters and setters
        public Long getFileId() { return fileId; }
        public void setFileId(Long fileId) { this.fileId = fileId; }
        
        public String getFileName() { return fileName; }
        public void setFileName(String fileName) { this.fileName = fileName; }
        
        public String getFileType() { return fileType; }
        public void setFileType(String fileType) { this.fileType = fileType; }
        
        public Long getTotalDownloads() { return totalDownloads; }
        public void setTotalDownloads(Long totalDownloads) { this.totalDownloads = totalDownloads; }
        
        public Long getTotalPreviews() { return totalPreviews; }
        public void setTotalPreviews(Long totalPreviews) { this.totalPreviews = totalPreviews; }
        
        public Long getTotalShares() { return totalShares; }
        public void setTotalShares(Long totalShares) { this.totalShares = totalShares; }
        
        public Long getUniqueDownloaders() { return uniqueDownloaders; }
        public void setUniqueDownloaders(Long uniqueDownloaders) { this.uniqueDownloaders = uniqueDownloaders; }
        
        public Long getUniquePreviewers() { return uniquePreviewers; }
        public void setUniquePreviewers(Long uniquePreviewers) { this.uniquePreviewers = uniquePreviewers; }
        
        public Double getAverageRating() { return averageRating; }
        public void setAverageRating(Double averageRating) { this.averageRating = averageRating; }
        
        public Double getPopularityScore() { return popularityScore; }
        public void setPopularityScore(Double popularityScore) { this.popularityScore = popularityScore; }
        
        public String getLastDownloadTime() { return lastDownloadTime; }
        public void setLastDownloadTime(String lastDownloadTime) { this.lastDownloadTime = lastDownloadTime; }
        
        public String getLastPreviewTime() { return lastPreviewTime; }
        public void setLastPreviewTime(String lastPreviewTime) { this.lastPreviewTime = lastPreviewTime; }
        
        public DailyStats getDailyStats() { return dailyStats; }
        public void setDailyStats(DailyStats dailyStats) { this.dailyStats = dailyStats; }
    }
    
    /**
     * 文件排行
     */
    class FileRanking {
        private Long fileId;
        private String fileName;
        private Long downloadCount;
        private Long previewCount;
        private Long shareCount;
        
        public FileRanking() {}
        
        // getters and setters
        public Long getFileId() { return fileId; }
        public void setFileId(Long fileId) { this.fileId = fileId; }
        
        public String getFileName() { return fileName; }
        public void setFileName(String fileName) { this.fileName = fileName; }
        
        public Long getDownloadCount() { return downloadCount; }
        public void setDownloadCount(Long downloadCount) { this.downloadCount = downloadCount; }
        
        public Long getPreviewCount() { return previewCount; }
        public void setPreviewCount(Long previewCount) { this.previewCount = previewCount; }
        
        public Long getShareCount() { return shareCount; }
        public void setShareCount(Long shareCount) { this.shareCount = shareCount; }
    }
    
    /**
     * 文件基本信息
     */
    class FileBasicInfo {
        private Long fileId;
        private String fileName;
        private Long fileSize;
        private String fileType;
        private String createdAt;
        
        public FileBasicInfo() {}
        
        // getters and setters
        public Long getFileId() { return fileId; }
        public void setFileId(Long fileId) { this.fileId = fileId; }
        
        public String getFileName() { return fileName; }
        public void setFileName(String fileName) { this.fileName = fileName; }
        
        public Long getFileSize() { return fileSize; }
        public void setFileSize(Long fileSize) { this.fileSize = fileSize; }
        
        public String getFileType() { return fileType; }
        public void setFileType(String fileType) { this.fileType = fileType; }
        
        public String getCreatedAt() { return createdAt; }
        public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
    }
    
    /**
     * 文件类型分布
     */
    class FileTypeDistribution {
        private String fileType;
        private Long fileCount;
        private Long totalSize;
        
        public FileTypeDistribution() {}
        
        // getters and setters
        public String getFileType() { return fileType; }
        public void setFileType(String fileType) { this.fileType = fileType; }
        
        public Long getFileCount() { return fileCount; }
        public void setFileCount(Long fileCount) { this.fileCount = fileCount; }
        
        public Long getTotalSize() { return totalSize; }
        public void setTotalSize(Long totalSize) { this.totalSize = totalSize; }
    }
    
    /**
     * 文件大小分布
     */
    class FileSizeDistribution {
        private String sizeRange;
        private Long fileCount;
        
        public FileSizeDistribution() {}
        
        // getters and setters
        public String getSizeRange() { return sizeRange; }
        public void setSizeRange(String sizeRange) { this.sizeRange = sizeRange; }
        
        public Long getFileCount() { return fileCount; }
        public void setFileCount(Long fileCount) { this.fileCount = fileCount; }
    }
    
    /**
     * 用户行为详情
     */
    class UserBehaviorDetail {
        private Long userId;
        private String username;
        private Long totalLogins;
        private String lastLoginTime;
        
        public UserBehaviorDetail() {}
        
        // getters and setters
        public Long getUserId() { return userId; }
        public void setUserId(Long userId) { this.userId = userId; }
        
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        
        public Long getTotalLogins() { return totalLogins; }
        public void setTotalLogins(Long totalLogins) { this.totalLogins = totalLogins; }
        
        public String getLastLoginTime() { return lastLoginTime; }
        public void setLastLoginTime(String lastLoginTime) { this.lastLoginTime = lastLoginTime; }
    }
    
    /**
     * 用户排行
     */
    class UserRanking {
        private Long userId;
        private String username;
        private Long activityScore;
        private Long contributionScore;
        private Long storageUsed;
        private Long storageQuota;
        
        public UserRanking() {}
        
        // getters and setters
        public Long getUserId() { return userId; }
        public void setUserId(Long userId) { this.userId = userId; }
        
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        
        public Long getActivityScore() { return activityScore; }
        public void setActivityScore(Long activityScore) { this.activityScore = activityScore; }
        
        public Long getContributionScore() { return contributionScore; }
        public void setContributionScore(Long contributionScore) { this.contributionScore = contributionScore; }
        
        public Long getStorageUsed() { return storageUsed; }
        public void setStorageUsed(Long storageUsed) { this.storageUsed = storageUsed; }
        
        public Long getStorageQuota() { return storageQuota; }
        public void setStorageQuota(Long storageQuota) { this.storageQuota = storageQuota; }
    }
    
    /**
     * 用户等级分布
     */
    class UserLevelDistribution {
        private String level;
        private Long userCount;
        
        public UserLevelDistribution() {}
        
        // getters and setters
        public String getLevel() { return level; }
        public void setLevel(String level) { this.level = level; }
        
        public Long getUserCount() { return userCount; }
        public void setUserCount(Long userCount) { this.userCount = userCount; }
    }
    
    /**
     * 日趋势
     */
    class DailyTrend {
        private String date;
        private Long value;
        
        public DailyTrend() {}
        
        // getters and setters
        public String getDate() { return date; }
        public void setDate(String date) { this.date = date; }
        
        public Long getValue() { return value; }
        public void setValue(Long value) { this.value = value; }
    }
    
    /**
     * 系统概览
     */
    class SystemOverview {
        private Long totalUsers;
        private Long totalFiles;
        private Long totalFolders;
        private Long totalShares;
        private Long totalStorageUsed;
        
        public SystemOverview() {}
        
        // getters and setters
        public Long getTotalUsers() { return totalUsers; }
        public void setTotalUsers(Long totalUsers) { this.totalUsers = totalUsers; }
        
        public Long getTotalFiles() { return totalFiles; }
        public void setTotalFiles(Long totalFiles) { this.totalFiles = totalFiles; }
        
        public Long getTotalFolders() { return totalFolders; }
        public void setTotalFolders(Long totalFolders) { this.totalFolders = totalFolders; }
        
        public Long getTotalShares() { return totalShares; }
        public void setTotalShares(Long totalShares) { this.totalShares = totalShares; }
        
        public Long getTotalStorageUsed() { return totalStorageUsed; }
        public void setTotalStorageUsed(Long totalStorageUsed) { this.totalStorageUsed = totalStorageUsed; }
    }
    
    /**
     * 系统负载趋势
     */
    class SystemLoadTrend {
        private String date;
        private Double cpuUsage;
        private Double memoryUsage;
        private Double storageUsage;
        
        public SystemLoadTrend() {}
        
        // getters and setters
        public String getDate() { return date; }
        public void setDate(String date) { this.date = date; }
        
        public Double getCpuUsage() { return cpuUsage; }
        public void setCpuUsage(Double cpuUsage) { this.cpuUsage = cpuUsage; }
        
        public Double getMemoryUsage() { return memoryUsage; }
        public void setMemoryUsage(Double memoryUsage) { this.memoryUsage = memoryUsage; }
        
        public Double getStorageUsage() { return storageUsage; }
        public void setStorageUsage(Double storageUsage) { this.storageUsage = storageUsage; }
    }
    
    /**
     * 系统健康报告
     */
    class SystemHealthReport {
        private String reportDate;
        private String systemStatus;
        private Long totalUsers;
        private Long totalFiles;
        
        public SystemHealthReport() {}
        
        // getters and setters
        public String getReportDate() { return reportDate; }
        public void setReportDate(String reportDate) { this.reportDate = reportDate; }
        
        public String getSystemStatus() { return systemStatus; }
        public void setSystemStatus(String systemStatus) { this.systemStatus = systemStatus; }
        
        public Long getTotalUsers() { return totalUsers; }
        public void setTotalUsers(Long totalUsers) { this.totalUsers = totalUsers; }
        
        public Long getTotalFiles() { return totalFiles; }
        public void setTotalFiles(Long totalFiles) { this.totalFiles = totalFiles; }
    }
    
    /**
     * 性能趋势
     */
    class PerformanceTrend {
        private String date;
        private Double averageResponseTime;
        private Double errorRate;
        
        public PerformanceTrend() {}
        
        // getters and setters
        public String getDate() { return date; }
        public void setDate(String date) { this.date = date; }
        
        public Double getAverageResponseTime() { return averageResponseTime; }
        public void setAverageResponseTime(Double averageResponseTime) { this.averageResponseTime = averageResponseTime; }
        
        public Double getErrorRate() { return errorRate; }
        public void setErrorRate(Double errorRate) { this.errorRate = errorRate; }
    }
    
    /**
     * 系统告警
     */
    class SystemAlert {
        private String alertType;
        private String alertTitle;
        private String alertMessage;
        private String alertTime;
        private String severity;
        
        public SystemAlert() {}
        
        // getters and setters
        public String getAlertType() { return alertType; }
        public void setAlertType(String alertType) { this.alertType = alertType; }
        
        public String getAlertTitle() { return alertTitle; }
        public void setAlertTitle(String alertTitle) { this.alertTitle = alertTitle; }
        
        public String getAlertMessage() { return alertMessage; }
        public void setAlertMessage(String alertMessage) { this.alertMessage = alertMessage; }
        
        public String getAlertTime() { return alertTime; }
        public void setAlertTime(String alertTime) { this.alertTime = alertTime; }
        
        public String getSeverity() { return severity; }
        public void setSeverity(String severity) { this.severity = severity; }
    }
    
    /**
     * 日常统计
     */
    class DailyStats {
        private Long dailyDownloads;
        private Long dailyPreviews;
        private Long dailyShares;
        
        public DailyStats() {}
        
        // getters and setters
        public Long getDailyDownloads() { return dailyDownloads; }
        public void setDailyDownloads(Long dailyDownloads) { this.dailyDownloads = dailyDownloads; }
        
        public Long getDailyPreviews() { return dailyPreviews; }
        public void setDailyPreviews(Long dailyPreviews) { this.dailyPreviews = dailyPreviews; }
        
        public Long getDailyShares() { return dailyShares; }
        public void setDailyShares(Long dailyShares) { this.dailyShares = dailyShares; }
    }
    
    /**
     * 用户统计信息
     */
    class UserStatistics {
        private Long userId;
        private String username;
        private Long totalFiles;
        private Long totalFolders;
        private Long totalShares;
        private Long usedStorage;
        private Long storageQuota;
        private Double storageUsagePercentage;
        
        public UserStatistics() {}
        
        // getters and setters
        public Long getUserId() { return userId; }
        public void setUserId(Long userId) { this.userId = userId; }
        
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        
        public Long getTotalFiles() { return totalFiles; }
        public void setTotalFiles(Long totalFiles) { this.totalFiles = totalFiles; }
        
        public Long getTotalFolders() { return totalFolders; }
        public void setTotalFolders(Long totalFolders) { this.totalFolders = totalFolders; }
        
        public Long getTotalShares() { return totalShares; }
        public void setTotalShares(Long totalShares) { this.totalShares = totalShares; }
        
        public Long getUsedStorage() { return usedStorage; }
        public void setUsedStorage(Long usedStorage) { this.usedStorage = usedStorage; }
        
        public Long getStorageQuota() { return storageQuota; }
        public void setStorageQuota(Long storageQuota) { this.storageQuota = storageQuota; }
        
        public Double getStorageUsagePercentage() { return storageUsagePercentage; }
        public void setStorageUsagePercentage(Double storageUsagePercentage) { this.storageUsagePercentage = storageUsagePercentage; }
    }
    
    /**
     * 系统统计信息
     */
    class SystemStatistics {
        private Long totalUsers;
        private Long totalFiles;
        private Long totalFolders;
        private Long totalShares;
        private Long totalStorageUsed;
        
        public SystemStatistics() {}
        
        // getters and setters
        public Long getTotalUsers() { return totalUsers; }
        public void setTotalUsers(Long totalUsers) { this.totalUsers = totalUsers; }
        
        public Long getTotalFiles() { return totalFiles; }
        public void setTotalFiles(Long totalFiles) { this.totalFiles = totalFiles; }
        
        public Long getTotalFolders() { return totalFolders; }
        public void setTotalFolders(Long totalFolders) { this.totalFolders = totalFolders; }
        
        public Long getTotalShares() { return totalShares; }
        public void setTotalShares(Long totalShares) { this.totalShares = totalShares; }
        
        public Long getTotalStorageUsed() { return totalStorageUsed; }
        public void setTotalStorageUsed(Long totalStorageUsed) { this.totalStorageUsed = totalStorageUsed; }
    }
    
    /**
     * 文件类型统计
     */
    class FileTypeStatistic {
        private String fileType;
        private Long fileCount;
        private Long totalSize;
        
        public FileTypeStatistic() {}
        
        public FileTypeStatistic(String fileType, Long fileCount, Long totalSize) {
            this.fileType = fileType;
            this.fileCount = fileCount;
            this.totalSize = totalSize;
        }
        
        // getters and setters
        public String getFileType() { return fileType; }
        public void setFileType(String fileType) { this.fileType = fileType; }
        
        public Long getFileCount() { return fileCount; }
        public void setFileCount(Long fileCount) { this.fileCount = fileCount; }
        
        public Long getTotalSize() { return totalSize; }
        public void setTotalSize(Long totalSize) { this.totalSize = totalSize; }
    }
}