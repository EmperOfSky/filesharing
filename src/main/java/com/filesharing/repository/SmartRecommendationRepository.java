package com.filesharing.repository;

import com.filesharing.entity.SmartRecommendation;
import com.filesharing.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 智能推荐Repository接口
 */
@Repository
public interface SmartRecommendationRepository extends JpaRepository<SmartRecommendation, Long> {
    
    /**
     * 查找用户未查看的推荐
     */
    Page<SmartRecommendation> findByUserAndIsViewedFalseOrderByCreatedAtDesc(User user, Pageable pageable);
    
    /**
     * 查找用户的所有推荐
     */
    List<SmartRecommendation> findByUser(User user);
    
    /**
     * 查找指定类型的推荐
     */
    List<SmartRecommendation> findByUserAndRecommendationType(User user, SmartRecommendation.RecommendationType type);
    
    /**
     * 查找已采纳的推荐
     */
    List<SmartRecommendation> findByUserAndIsAdoptedTrue(User user);
    
    /**
     * 查找指定时间段内的推荐
     */
    List<SmartRecommendation> findByUserAndCreatedAtBetween(User user, LocalDateTime startTime, LocalDateTime endTime);
    
    /**
     * 查找高相关度的推荐
     */
    @Query("SELECT sr FROM SmartRecommendation sr WHERE sr.user = :user AND sr.relevanceScore >= :minScore ORDER BY sr.relevanceScore DESC")
    List<SmartRecommendation> findHighRelevanceRecommendations(@Param("user") User user, @Param("minScore") Double minScore);
    
    /**
     * 统计用户推荐总数
     */
    long countByUser(User user);
    
    /**
     * 统计用户未查看的推荐数
     */
    long countByUserAndIsViewedFalse(User user);
    
    /**
     * 统计用户已采纳的推荐数
     */
    long countByUserAndIsAdoptedTrue(User user);
    
    /**
     * 按推荐类型统计
     */
    @Query("SELECT sr.recommendationType, COUNT(sr) FROM SmartRecommendation sr WHERE sr.user = :user GROUP BY sr.recommendationType")
    List<Object[]> countByRecommendationType(@Param("user") User user);
    
    /**
     * 按来源类型统计
     */
    @Query("SELECT sr.sourceType, COUNT(sr) FROM SmartRecommendation sr WHERE sr.user = :user GROUP BY sr.sourceType")
    List<Object[]> countBySourceType(@Param("user") User user);
    
    /**
     * 查找过期的推荐记录
     */
    List<SmartRecommendation> findByExpireAtBefore(LocalDateTime expireTime);
    
    /**
     * 删除过期的推荐记录
     */
    @Modifying
    @Query("DELETE FROM SmartRecommendation sr WHERE sr.expireAt < :expireTime")
    int deleteByExpireAtBefore(@Param("expireTime") LocalDateTime expireTime);
    
    /**
     * 批量更新推荐状态为已查看
     */
    @Modifying
    @Query("UPDATE SmartRecommendation sr SET sr.isViewed = true, sr.viewedAt = :viewTime WHERE sr.user = :user AND sr.id IN :ids")
    int batchMarkAsViewed(@Param("user") User user, @Param("ids") List<Long> ids, @Param("viewTime") LocalDateTime viewTime);
    
    /**
     * 获取用户推荐统计信息
     */
    @Query("SELECT " +
           "COUNT(sr) as total, " +
           "COUNT(CASE WHEN sr.isViewed = true THEN 1 END) as viewed, " +
           "COUNT(CASE WHEN sr.isAdopted = true THEN 1 END) as adopted, " +
           "AVG(sr.relevanceScore) as avgScore " +
           "FROM SmartRecommendation sr WHERE sr.user = :user")
    Object[] getUserRecommendationStats(@Param("user") User user);
    
    /**
     * 查找最常被采纳的推荐项
     */
    @Query("SELECT sr.itemId, sr.recommendationType, COUNT(sr) as adoptionCount " +
           "FROM SmartRecommendation sr WHERE sr.isAdopted = true " +
           "GROUP BY sr.itemId, sr.recommendationType " +
           "ORDER BY adoptionCount DESC")
    List<Object[]> findMostAdoptedItems();
    
    /**
     * 按时间段统计推荐效果
     */
    @Query(value = "SELECT DATE(sr.created_at) as date, " +
           "COUNT(*) as total, " +
           "SUM(CASE WHEN sr.is_viewed = true THEN 1 ELSE 0 END) as viewed, " +
           "SUM(CASE WHEN sr.is_adopted = true THEN 1 ELSE 0 END) as adopted " +
           "FROM smart_recommendations sr " +
           "WHERE sr.user_id = :#{#user.id} AND sr.created_at >= :startDate " +
           "GROUP BY DATE(sr.created_at) ORDER BY date DESC",
           nativeQuery = true)
    List<Object[]> getRecommendationTrendStats(@Param("user") User user, @Param("startDate") LocalDateTime startDate);
}