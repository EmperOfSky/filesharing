package com.filesharing.service;

import com.filesharing.ai.correction.CorrectionResult;
import com.filesharing.ai.image.ImageAnalysisResult;
import com.filesharing.ai.qa.QAResponse;
import com.filesharing.ai.recommendation.RecommendationResult;
import com.filesharing.ai.search.SmartSearchResult;
import com.filesharing.ai.text.TextAnalysisResult;
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
     * 文本内容分析
     */
    TextAnalysisResult analyzeTextContent(String content, User user);
    
    /**
     * 图像内容分析
     */
    ImageAnalysisResult analyzeImage(MultipartFile image, User user);
    
    /**
     * 推荐相似文件
     */
    RecommendationResult recommendSimilarFiles(Long fileId, User user);
    
    /**
     * 智能标签推荐
     */
    List<String> recommendTags(String content, String fileType, User user);
    
    /**
     * 智能搜索
     */
    SmartSearchResult smartSearch(String query, User user);
    
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
     * 文档智能摘要生成
     */
    String generateDocumentSummary(String documentContent, int summaryLength, String language);
    
    /**
     * 智能问答系统
     */
    QAResponse answerQuestion(String question, String context, User user);
    
    /**
     * 文本纠错服务
     */
    CorrectionResult correctText(String text, String language);
    
    /**
     * 记录AI分析日志
     */
    void recordAIAnalysis(Long modelId, String targetType, Long targetId, 
                         String input, String result, Double confidence, User user);
}