package com.filesharing.repository;

import com.filesharing.entity.Tag;
import com.filesharing.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 标签Repository接口
 */
@Repository
public interface TagRepository extends JpaRepository<Tag, Long> {
    
    /**
     * 根据标签名称查找标签
     */
    Optional<Tag> findByTagName(String tagName);
    
    /**
     * 根据用户查找标签（包括系统标签）
     */
    @Query("SELECT t FROM Tag t WHERE t.createdBy = :user OR t.isSystemTag = true " +
           "ORDER BY t.usageCount DESC, t.tagName ASC")
    Page<Tag> findByUserOrSystem(@Param("user") User user, Pageable pageable);
    
    /**
     * 查找系统标签
     */
    @Query("SELECT t FROM Tag t WHERE t.isSystemTag = true ORDER BY t.tagName ASC")
    List<Tag> findSystemTags();
    
    /**
     * 查找用户自定义标签
     */
    @Query("SELECT t FROM Tag t WHERE t.createdBy = :user AND t.isSystemTag = false " +
           "ORDER BY t.usageCount DESC, t.tagName ASC")
    Page<Tag> findUserTags(@Param("user") User user, Pageable pageable);
    
    /**
     * 根据标签名称模糊查找
     */
    @Query("SELECT t FROM Tag t WHERE (t.createdBy = :user OR t.isSystemTag = true) " +
           "AND t.tagName LIKE %:keyword% ORDER BY t.usageCount DESC")
    Page<Tag> findByTagNameContaining(@Param("user") User user, 
                                     @Param("keyword") String keyword, 
                                     Pageable pageable);
    
    /**
     * 查找最常用的标签
     */
    @Query("SELECT t FROM Tag t WHERE t.createdBy = :user OR t.isSystemTag = true " +
           "ORDER BY t.usageCount DESC")
    Page<Tag> findPopularTags(@Param("user") User user, Pageable pageable);
    
    /**
     * 检查标签名称是否存在
     */
    @Query("SELECT CASE WHEN COUNT(t) > 0 THEN true ELSE false END FROM Tag t " +
           "WHERE t.tagName = :tagName AND (t.createdBy = :user OR t.isSystemTag = true)")
    boolean existsByTagNameAndUser(@Param("tagName") String tagName, @Param("user") User user);
    
    /**
     * 统计用户标签使用情况
     */
    @Query("SELECT t.tagName, COUNT(ft) FROM Tag t LEFT JOIN t.fileTags ft " +
           "WHERE t.createdBy = :user OR t.isSystemTag = true " +
           "GROUP BY t.id, t.tagName ORDER BY COUNT(ft) DESC")
    List<Object[]> getTagUsageStats(@Param("user") User user);
    
    /**
     * 查找未使用的标签
     */
    @Query("SELECT t FROM Tag t WHERE t.usageCount = 0 AND t.createdBy = :user")
    Page<Tag> findUnusedTags(@Param("user") User user, Pageable pageable);
    
    /**
     * 获取标签云数据
     */
    @Query("SELECT t.tagName, t.color, t.usageCount FROM Tag t " +
           "WHERE (t.createdBy = :user OR t.isSystemTag = true) AND t.usageCount > 0 " +
           "ORDER BY t.usageCount DESC")
    List<Object[]> getTagCloudData(@Param("user") User user, Pageable pageable);
}