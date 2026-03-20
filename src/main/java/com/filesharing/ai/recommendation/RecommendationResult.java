package com.filesharing.ai.recommendation;

import lombok.Data;
import java.util.List;

/**
 * 智能推荐结果
 */
@Data
public class RecommendationResult {
    
    /**
     * 推荐是否成功
     */
    private Boolean success;
    
    /**
     * 错误信息
     */
    private String error;
    
    /**
     * 推荐类型
     */
    private RecommendationType type;
    
    /**
     * 推荐项目列表
     */
    private List<RecommendedItem> items;
    
    /**
     * 推荐理由
     */
    private String reason;
    
    /**
     * 置信度
     */
    private Double confidence;
    
    /**
     * 推荐类型枚举
     */
    public enum RecommendationType {
        FILE_SIMILARITY,    // 相似文件推荐
        TAG_BASED,          // 基于标签推荐
        USAGE_PATTERN,      // 基于使用模式推荐
        COLLABORATIVE,      // 协同过滤推荐
        CONTENT_BASED       // 基于内容推荐
    }
    
    /**
     * 推荐项目
     */
    @Data
    public static class RecommendedItem {
        private Long itemId;
        private String itemName;
        private String itemType; // FILE, FOLDER, DOCUMENT
        private Double similarityScore;
        private List<String> commonTags;
        private String reason;
    }
}