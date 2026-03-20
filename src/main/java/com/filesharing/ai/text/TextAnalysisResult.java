package com.filesharing.ai.text;

import lombok.Data;
import java.util.List;

/**
 * 文本分析结果
 */
@Data
public class TextAnalysisResult {
    
    /**
     * 分析是否成功
     */
    private Boolean success;
    
    /**
     * 错误信息
     */
    private String error;
    
    /**
     * 文本类别
     */
    private String category;
    
    /**
     * 置信度
     */
    private Double confidence;
    
    /**
     * 关键词列表
     */
    private List<String> keywords;
    
    /**
     * 情感分析结果
     */
    private SentimentResult sentiment;
    
    /**
     * 语言检测结果
     */
    private LanguageDetectionResult language;
    
    /**
     * 文本摘要
     */
    private String summary;
    
    /**
     * 推荐标签
     */
    private List<String> recommendedTags;
    
    /**
     * 情感分析结果
     */
    @Data
    public static class SentimentResult {
        private String polarity; // POSITIVE, NEGATIVE, NEUTRAL
        private Double score;
        private String emotion; // happy, sad, angry, etc.
    }
    
    /**
     * 语言检测结果
     */
    @Data
    public static class LanguageDetectionResult {
        private String language;
        private String languageCode;
        private Double confidence;
    }
}