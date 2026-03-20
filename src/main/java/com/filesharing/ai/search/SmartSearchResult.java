package com.filesharing.ai.search;

import lombok.Data;
import java.util.List;

/**
 * 智能搜索结果
 */
@Data
public class SmartSearchResult {
    
    /**
     * 搜索是否成功
     */
    private Boolean success;
    
    /**
     * 错误信息
     */
    private String error;
    
    /**
     * 搜索关键词
     */
    private String query;
    
    /**
     * 搜索结果总数
     */
    private Long totalResults;
    
    /**
     * 搜索结果列表
     */
    private List<SearchResultItem> results;
    
    /**
     * 搜索建议
     */
    private List<String> suggestions;
    
    /**
     * 搜索耗时（毫秒）
     */
    private Long searchTime;
    
    /**
     * 搜索结果项
     */
    @Data
    public static class SearchResultItem {
        private Long id;
        private String name;
        private String type; // FILE, FOLDER, DOCUMENT
        private String path;
        private Double relevanceScore;
        private List<String> matchedKeywords;
        private String snippet;
        private String fileType;
        private Long fileSize;
        private String owner;
        private String createdAt;
    }
}