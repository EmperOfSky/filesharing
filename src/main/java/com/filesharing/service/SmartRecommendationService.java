package com.filesharing.service;

import com.filesharing.entity.SmartRecommendation;
import com.filesharing.entity.User;
import org.springframework.data.domain.Page;

import java.util.List;

/**
 * 智能推荐服务接口
 */
public interface SmartRecommendationService {
    
    /**
     * 生成个性化推荐
     */
    List<SmartRecommendation> generatePersonalizedRecommendations(User user);
    
    /**
     * 获取用户推荐列表
     */
    Page<SmartRecommendation> getUserRecommendations(User user, int page, int size);
    
    /**
     * 标记推荐为已查看
     */
    void markRecommendationViewed(Long recommendationId, User user);
    
    /**
     * 标记推荐为已采纳
     */
    void markRecommendationAdopted(Long recommendationId, User user);
    
    /**
     * 清理过期推荐
     */
    void cleanupExpiredRecommendations();
    
    /**
     * 获取推荐分析数据
     */
    RecommendationAnalytics getRecommendationAnalytics(User user);
    
    /**
     * 获取相似项推荐
     */
    List<SmartRecommendation> getSimilarItemsRecommendation(Long itemId, 
        SmartRecommendation.RecommendationType type, User user);
    
    /**
     * 推荐分析数据传输对象
     */
    class RecommendationAnalytics {
        private Long totalRecommendations;
        private Long viewedRecommendations;
        private Long adoptedRecommendations;
        private Double viewRate;
        private Double adoptionRate;
        private java.util.Map<SmartRecommendation.RecommendationType, Long> typeDistribution;
        private java.util.Map<SmartRecommendation.SourceType, Long> sourceDistribution;
        
        // 构造函数
        public RecommendationAnalytics() {}
        
        public RecommendationAnalytics(Long total, Long viewed, Long adopted) {
            this.totalRecommendations = total;
            this.viewedRecommendations = viewed;
            this.adoptedRecommendations = adopted;
            this.viewRate = total > 0 ? (double) viewed / total * 100 : 0.0;
            this.adoptionRate = viewed > 0 ? (double) adopted / viewed * 100 : 0.0;
        }
        
        // Getters and Setters
        public Long getTotalRecommendations() { return totalRecommendations; }
        public void setTotalRecommendations(Long totalRecommendations) { this.totalRecommendations = totalRecommendations; }
        
        public Long getViewedRecommendations() { return viewedRecommendations; }
        public void setViewedRecommendations(Long viewedRecommendations) { this.viewedRecommendations = viewedRecommendations; }
        
        public Long getAdoptedRecommendations() { return adoptedRecommendations; }
        public void setAdoptedRecommendations(Long adoptedRecommendations) { this.adoptedRecommendations = adoptedRecommendations; }
        
        public Double getViewRate() { return viewRate; }
        public void setViewRate(Double viewRate) { this.viewRate = viewRate; }
        
        public Double getAdoptionRate() { return adoptionRate; }
        public void setAdoptionRate(Double adoptionRate) { this.adoptionRate = adoptionRate; }
        
        public java.util.Map<SmartRecommendation.RecommendationType, Long> getTypeDistribution() { return typeDistribution; }
        public void setTypeDistribution(java.util.Map<SmartRecommendation.RecommendationType, Long> typeDistribution) { this.typeDistribution = typeDistribution; }
        
        public java.util.Map<SmartRecommendation.SourceType, Long> getSourceDistribution() { return sourceDistribution; }
        public void setSourceDistribution(java.util.Map<SmartRecommendation.SourceType, Long> sourceDistribution) { this.sourceDistribution = sourceDistribution; }
    }
}