package com.filesharing.service;

import com.filesharing.entity.AIModel;
import com.filesharing.entity.User;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

/**
 * AI服务接口
 */
public interface AIService {
    
    /**
     * 文本内容智能分类
     */
    Map<String, Object> classifyTextContent(String content, User user);
    
    /**
     * 文件内容智能分析
     */
    Map<String, Object> analyzeFileContent(MultipartFile file, User user);
    
    /**
     * 图像内容识别
     */
    Map<String, Object> recognizeImage(MultipartFile image, User user);
    
    /**
     * 智能标签推荐
     */
    List<String> recommendTags(String content, String fileType, User user);
    
    /**
     * 智能搜索优化
     */
    List<Map<String, Object>> smartSearch(String query, User user);
    
    /**
     * 用户行为分析
     */
    Map<String, Object> analyzeUserBehavior(User user);
    
    /**
     * 内容相似度分析
     */
    Double calculateSimilarity(String content1, String content2);
    
    /**
     * 自动摘要生成
     */
    String generateSummary(String content, int maxLength);
    
    /**
     * 情感分析
     */
    Map<String, Object> sentimentAnalysis(String content);
    
    /**
     * 关键词提取
     */
    List<String> extractKeywords(String content, int maxKeywords);
    
    /**
     * 获取AI模型列表
     */
    List<AIModel> getAvailableModels();
    
    /**
     * 测试AI模型连接
     */
    boolean testModelConnection(Long modelId);
    
    /**
     * 记录AI分析日志
     */
    void recordAIAnalysis(Long modelId, String targetType, Long targetId, 
                         String input, String result, Double confidence, User user);
}