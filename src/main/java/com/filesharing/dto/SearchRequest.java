package com.filesharing.dto;

import lombok.Data;
import lombok.Builder;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * 搜索请求DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SearchRequest {
    
    @NotBlank(message = "搜索关键词不能为空")
    private String keyword;
    
    @NotNull(message = "搜索类型不能为空")
    private SearchType searchType;
    
    private Integer page = 0;
    private Integer size = 10;
    
    // 过滤条件
    private List<String> fileTypes;
    private Long minSize;
    private Long maxSize;
    private String uploader;
    private String folderPath;
    private String dateFrom;
    private String dateTo;
    
    /**
     * 搜索类型枚举
     */
    public enum SearchType {
        FILENAME,   // 文件名搜索
        CONTENT,    // 文件内容搜索
        TAG,        // 标签搜索
        ALL         // 全部搜索
    }
}