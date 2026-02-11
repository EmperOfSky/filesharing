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
    
    // 其他DTO类定义...
    class FileRanking { /* 文件排行 */ }
    class FileBasicInfo { /* 文件基本信息 */ }
    class FileTypeDistribution { /* 文件类型分布 */ }
    class FileSizeDistribution { /* 文件大小分布 */ }
    class UserBehaviorDetail { /* 用户行为详情 */ }
    class UserRanking { /* 用户排行 */ }
    class UserLevelDistribution { /* 用户等级分布 */ }
    class DailyTrend { /* 日趋势 */ }
    class SystemOverview { /* 系统概览 */ }
    class SystemLoadTrend { /* 系统负载趋势 */ }
    class SystemHealthReport { /* 系统健康报告 */ }
    class PerformanceTrend { /* 性能趋势 */ }
    class SystemAlert { /* 系统告警 */ }
    class DailyStats { /* 日常统计 */ }
}