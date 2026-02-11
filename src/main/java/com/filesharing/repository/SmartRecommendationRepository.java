package com.filesharing.repository;

import com.filesharing.entity.SmartRecommendation;
import com.filesharing.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface SmartRecommendationRepository extends JpaRepository<SmartRecommendation, Long> {
    
    /**
     * 根据用户查找推荐
     */
    Page<SmartRecommendation> findByUserOrderByCreatedAtDesc(User user, Pageable pageable);
    
    /**
     * 查找用户未查看的推荐
     */
    Page<SmartRecommendation> findByUserAndIsViewedFalseOrderByRelevanceScoreDesc(User user, Pageable pageable);
    
    /**
     * 根据推荐类型查找
     */
    Page<SmartRecommendation> findByUserAndRecommendationTypeOrderByCreatedAtDesc(User user, 
                                                                                 SmartRecommendation.RecommendationType recommendationType, 
                                                                                 Pageable pageable);
    
    /**
     * 查找过期但未查看的推荐
     */
    @Query("SELECT sr FROM SmartRecommendation sr WHERE sr.user = :user AND sr.isViewed = false AND sr.expireAt < :now")
    Page<SmartRecommendation> findExpiredUnviewedRecommendations(@Param("user") User user, 
                                                               @Param("now") LocalDateTime now, 
                                                               Pageable pageable);
    
    /**
     * 统计用户推荐采纳率
     */
    @Query("SELECT COUNT(sr), SUM(CASE WHEN sr.isAdopted = true THEN 1 ELSE 0 END) FROM SmartRecommendation sr WHERE sr.user = :user")
    Object[] getUserRecommendationStats(@Param("user") User user);
    
    /**
     * 查找高相关度的推荐
     */
    @Query("SELECT sr FROM SmartRecommendation sr WHERE sr.user = :user AND sr.relevanceScore >= :minScore ORDER BY sr.relevanceScore DESC")
    Page<SmartRecommendation> findHighRelevanceRecommendations(@Param("user") User user, 
                                                              @Param("minScore") Double minScore, 
                                                              Pageable pageable);
    
    /**
     * 统计各类推荐的数量
     */
    @Query("SELECT sr.recommendationType, COUNT(sr) FROM SmartRecommendation sr WHERE sr.user = :user GROUP BY sr.recommendationType")
    List<Object[]> getRecommendationTypeStats(@Param("user") User user);
    
    /**
     * 查找最近被采纳的推荐
     */
    @Query("SELECT sr FROM SmartRecommendation sr WHERE sr.user = :user AND sr.isAdopted = true ORDER BY sr.adoptedAt DESC")
    Page<SmartRecommendation> findRecentlyAdopted(@Param("user") User user, Pageable pageable);
    
    /**
     * 删除过期的推荐
     */
    @Query("DELETE FROM SmartRecommendation sr WHERE sr.expireAt < :now")
    void deleteExpiredRecommendations(@Param("now") LocalDateTime now);
}