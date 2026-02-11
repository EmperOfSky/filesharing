package com.filesharing.dto;

import lombok.Data;
import lombok.Builder;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 搜索响应DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SearchResponse {
    
    private List<SearchResult> results;
    private Long totalResults;
    private Integer currentPage;
    private Integer totalPages;
    private Long searchDuration; // 搜索耗时（毫秒）
    private String searchKeyword;
    private String searchType;
    private List<Suggestion> suggestions; // 搜索建议
    
    /**
     * 搜索结果内部类
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SearchResult {
        private Long fileId;
        private String fileName;
        private String fileType;
        private Long fileSize;
        private String fileIcon;
        private String previewUrl;
        private String downloadUrl;
        private String uploaderName;
        private String folderPath;
        private String highlight; // 高亮匹配内容
        private Double relevanceScore; // 相关性评分
        private String createdAt;
        private String updatedAt;
    }
    
    /**
     * 搜索建议内部类
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Suggestion {
        private String keyword;
        private Long resultCount;
        private String type; // 建议类型
    }
}