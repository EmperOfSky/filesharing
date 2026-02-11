package com.filesharing.service;

import com.filesharing.dto.SearchRequest;
import com.filesharing.dto.SearchResponse;
import com.filesharing.entity.User;
import org.springframework.data.domain.Page;

import java.util.List;

/**
 * 搜索服务接口
 */
public interface SearchService {
    
    /**
     * 执行文件搜索
     */
    SearchResponse searchFiles(SearchRequest request, User user, String clientIp, String userAgent);
    
    /**
     * 搜索文件名
     */
    Page<SearchResult> searchByFileName(String keyword, User user, int page, int size);
    
    /**
     * 搜索文件内容（需要集成全文搜索引擎）
     */
    Page<SearchResult> searchByContent(String keyword, User user, int page, int size);
    
    /**
     * 组合搜索（文件名+内容）
     */
    Page<SearchResult> searchAll(String keyword, User user, int page, int size);
    
    /**
     * 高级搜索（带过滤条件）
     */
    Page<SearchResult> advancedSearch(SearchRequest request, User user, int page, int size);
    
    /**
     * 获取搜索建议
     */
    List<String> getSearchSuggestions(String partialKeyword, User user);
    
    /**
     * 获取热门搜索词
     */
    List<HotKeyword> getHotKeywords(int limit);
    
    /**
     * 获取搜索历史
     */
    Page<SearchHistory> getSearchHistory(User user, int page, int size);
    
    /**
     * 清理过期搜索记录
     */
    void cleanupExpiredSearchRecords();
    
    /**
     * 获取搜索统计信息
     */
    SearchStatistics getSearchStatistics();
    
    /**
     * 搜索结果DTO
     */
    class SearchResult {
        private Long fileId;
        private String fileName;
        private String fileType;
        private Long fileSize;
        private String filePath;
        private String uploaderName;
        private String folderPath;
        private String highlight; // 高亮显示的匹配内容
        private Double score; // 相关性评分
        private String createdAt;
        
        // 构造函数
        public SearchResult() {}
        
        public SearchResult(Long fileId, String fileName, String fileType, Long fileSize, 
                          String filePath, String uploaderName, String folderPath) {
            this.fileId = fileId;
            this.fileName = fileName;
            this.fileType = fileType;
            this.fileSize = fileSize;
            this.filePath = filePath;
            this.uploaderName = uploaderName;
            this.folderPath = folderPath;
        }
        
        // getters and setters
        public Long getFileId() { return fileId; }
        public void setFileId(Long fileId) { this.fileId = fileId; }
        
        public String getFileName() { return fileName; }
        public void setFileName(String fileName) { this.fileName = fileName; }
        
        public String getFileType() { return fileType; }
        public void setFileType(String fileType) { this.fileType = fileType; }
        
        public Long getFileSize() { return fileSize; }
        public void setFileSize(Long fileSize) { this.fileSize = fileSize; }
        
        public String getFilePath() { return filePath; }
        public void setFilePath(String filePath) { this.filePath = filePath; }
        
        public String getUploaderName() { return uploaderName; }
        public void setUploaderName(String uploaderName) { this.uploaderName = uploaderName; }
        
        public String getFolderPath() { return folderPath; }
        public void setFolderPath(String folderPath) { this.folderPath = folderPath; }
        
        public String getHighlight() { return highlight; }
        public void setHighlight(String highlight) { this.highlight = highlight; }
        
        public Double getScore() { return score; }
        public void setScore(Double score) { this.score = score; }
        
        public String getCreatedAt() { return createdAt; }
        public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
    }
    
    /**
     * 热门关键词DTO
     */
    class HotKeyword {
        private String keyword;
        private Long searchCount;
        private Double trend; // 趋势变化
        
        // 构造函数
        public HotKeyword() {}
        
        public HotKeyword(String keyword, Long searchCount) {
            this.keyword = keyword;
            this.searchCount = searchCount;
        }
        
        // getters and setters
        public String getKeyword() { return keyword; }
        public void setKeyword(String keyword) { this.keyword = keyword; }
        
        public Long getSearchCount() { return searchCount; }
        public void setSearchCount(Long searchCount) { this.searchCount = searchCount; }
        
        public Double getTrend() { return trend; }
        public void setTrend(Double trend) { this.trend = trend; }
    }
    
    /**
     * 搜索历史DTO
     */
    class SearchHistory {
        private Long id;
        private String keyword;
        private String searchType;
        private Integer resultCount;
        private Long searchDuration;
        private String searchTime;
        
        // 构造函数
        public SearchHistory() {}
        
        public SearchHistory(Long id, String keyword, String searchType, 
                           Integer resultCount, Long searchDuration, String searchTime) {
            this.id = id;
            this.keyword = keyword;
            this.searchType = searchType;
            this.resultCount = resultCount;
            this.searchDuration = searchDuration;
            this.searchTime = searchTime;
        }
        
        // getters and setters
        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        
        public String getKeyword() { return keyword; }
        public void setKeyword(String keyword) { this.keyword = keyword; }
        
        public String getSearchType() { return searchType; }
        public void setSearchType(String searchType) { this.searchType = searchType; }
        
        public Integer getResultCount() { return resultCount; }
        public void setResultCount(Integer resultCount) { this.resultCount = resultCount; }
        
        public Long getSearchDuration() { return searchDuration; }
        public void setSearchDuration(Long searchDuration) { this.searchDuration = searchDuration; }
        
        public String getSearchTime() { return searchTime; }
        public void setSearchTime(String searchTime) { this.searchTime = searchTime; }
    }
    
    /**
     * 搜索统计信息
     */
    class SearchStatistics {
        private Long totalSearches;
        private Long successfulSearches;
        private Double successRate;
        private Double averageDuration;
        private List<SearchTypeStat> typeStats;
        private List<DailySearchStat> dailyStats;
        
        // 构造函数
        public SearchStatistics() {}
        
        public SearchStatistics(Long totalSearches, Long successfulSearches, Double averageDuration) {
            this.totalSearches = totalSearches;
            this.successfulSearches = successfulSearches;
            this.successRate = totalSearches > 0 ? (double) successfulSearches / totalSearches * 100 : 0.0;
            this.averageDuration = averageDuration;
        }
        
        // 内部类：搜索类型统计
        public static class SearchTypeStat {
            private String searchType;
            private Long count;
            private Double percentage;
            
            public SearchTypeStat() {}
            
            public SearchTypeStat(String searchType, Long count, Double percentage) {
                this.searchType = searchType;
                this.count = count;
                this.percentage = percentage;
            }
            
            // getters and setters
            public String getSearchType() { return searchType; }
            public void setSearchType(String searchType) { this.searchType = searchType; }
            
            public Long getCount() { return count; }
            public void setCount(Long count) { this.count = count; }
            
            public Double getPercentage() { return percentage; }
            public void setPercentage(Double percentage) { this.percentage = percentage; }
        }
        
        // 内部类：每日搜索统计
        public static class DailySearchStat {
            private String date;
            private Long searchCount;
            private Long successCount;
            private Double avgDuration;
            
            public DailySearchStat() {}
            
            public DailySearchStat(String date, Long searchCount, Long successCount, Double avgDuration) {
                this.date = date;
                this.searchCount = searchCount;
                this.successCount = successCount;
                this.avgDuration = avgDuration;
            }
            
            // getters and setters
            public String getDate() { return date; }
            public void setDate(String date) { this.date = date; }
            
            public Long getSearchCount() { return searchCount; }
            public void setSearchCount(Long searchCount) { this.searchCount = searchCount; }
            
            public Long getSuccessCount() { return successCount; }
            public void setSuccessCount(Long successCount) { this.successCount = successCount; }
            
            public Double getAvgDuration() { return avgDuration; }
            public void setAvgDuration(Double avgDuration) { this.avgDuration = avgDuration; }
        }
        
        // getters and setters
        public Long getTotalSearches() { return totalSearches; }
        public void setTotalSearches(Long totalSearches) { this.totalSearches = totalSearches; }
        
        public Long getSuccessfulSearches() { return successfulSearches; }
        public void setSuccessfulSearches(Long successfulSearches) { this.successfulSearches = successfulSearches; }
        
        public Double getSuccessRate() { return successRate; }
        public void setSuccessRate(Double successRate) { this.successRate = successRate; }
        
        public Double getAverageDuration() { return averageDuration; }
        public void setAverageDuration(Double averageDuration) { this.averageDuration = averageDuration; }
        
        public List<SearchTypeStat> getTypeStats() { return typeStats; }
        public void setTypeStats(List<SearchTypeStat> typeStats) { this.typeStats = typeStats; }
        
        public List<DailySearchStat> getDailyStats() { return dailyStats; }
        public void setDailyStats(List<DailySearchStat> dailyStats) { this.dailyStats = dailyStats; }
    }
}