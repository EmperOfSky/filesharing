package com.filesharing.repository;

import com.filesharing.entity.RecycleBin;
import com.filesharing.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 回收站Repository接口
 */
@Repository
public interface RecycleBinRepository extends JpaRepository<RecycleBin, Long> {
    
    /**
     * 根据用户查找回收站项目（按删除时间倒序）
     */
    Page<RecycleBin> findByDeletedByOrderByDeletedAtDesc(User user, Pageable pageable);
    
    /**
     * 根据用户和项目类型查找
     */
    @Query("SELECT rb FROM RecycleBin rb WHERE rb.deletedBy = :user AND rb.itemType = :itemType " +
           "ORDER BY rb.deletedAt DESC")
    Page<RecycleBin> findByDeletedByAndItemType(@Param("user") User user, 
                                               @Param("itemType") RecycleBin.ItemType itemType, 
                                               Pageable pageable);
    
    /**
     * 根据项目ID查找回收站记录
     */
    @Query("SELECT rb FROM RecycleBin rb WHERE rb.itemId = :itemId AND rb.itemType = :itemType")
    List<RecycleBin> findByItemIdAndItemType(@Param("itemId") Long itemId, 
                                            @Param("itemType") RecycleBin.ItemType itemType);
    
    /**
    * 查找可恢复的项目
    */
    @Query("SELECT rb FROM RecycleBin rb WHERE rb.deletedBy = :user AND rb.isRecoverable = true " +
           "ORDER BY rb.deletedAt DESC")
    Page<RecycleBin> findRecoverableItems(@Param("user") User user, Pageable pageable);
    
    /**
     * 查找已过期的回收站项目
     */
    @Query("SELECT rb FROM RecycleBin rb WHERE rb.expireAt < :currentTime")
    List<RecycleBin> findExpiredItems(@Param("currentTime") LocalDateTime currentTime);
    
    /**
     * 查找即将过期的项目（24小时内）
     */
    @Query("SELECT rb FROM RecycleBin rb WHERE rb.expireAt BETWEEN :startTime AND :endTime")
    List<RecycleBin> findItemsExpiringSoon(@Param("startTime") LocalDateTime startTime, 
                                          @Param("endTime") LocalDateTime endTime);
    
    /**
     * 统计用户回收站项目数量
     */
    @Query("SELECT COUNT(rb) FROM RecycleBin rb WHERE rb.deletedBy = :user")
    Long countByDeletedBy(@Param("user") User user);
    
    /**
     * 统计用户各类回收站项目数量
     */
    @Query("SELECT rb.itemType, COUNT(rb) FROM RecycleBin rb WHERE rb.deletedBy = :user " +
           "GROUP BY rb.itemType")
    List<Object[]> countItemsByType(@Param("user") User user);
    
    /**
     * 获取回收站统计信息
     */
    @Query("SELECT " +
           "COUNT(rb) as totalItems, " +
           "SUM(CASE WHEN rb.itemType = 'FILE' THEN 1 ELSE 0 END) as fileCount, " +
           "SUM(CASE WHEN rb.itemType = 'FOLDER' THEN 1 ELSE 0 END) as folderCount, " +
           "SUM(CASE WHEN rb.expireAt < :now THEN 1 ELSE 0 END) as expiredCount " +
           "FROM RecycleBin rb WHERE rb.deletedBy = :user")
    Object[] getRecycleBinStats(@Param("user") User user, @Param("now") LocalDateTime now);
    
    /**
     * 查找特定时间段内删除的项目
     */
    @Query("SELECT rb FROM RecycleBin rb WHERE rb.deletedBy = :user " +
           "AND rb.deletedAt BETWEEN :startTime AND :endTime ORDER BY rb.deletedAt DESC")
    Page<RecycleBin> findByDeletedByAndDeletedAtBetween(@Param("user") User user,
                                                       @Param("startTime") LocalDateTime startTime,
                                                       @Param("endTime") LocalDateTime endTime,
                                                       Pageable pageable);
    
    /**
     * 查找包含特定关键词的回收站项目
     */
    @Query("SELECT rb FROM RecycleBin rb WHERE rb.deletedBy = :user " +
           "AND (rb.originalName LIKE %:keyword% OR rb.deleteReason LIKE %:keyword%) " +
           "ORDER BY rb.deletedAt DESC")
    Page<RecycleBin> searchByKeyword(@Param("user") User user, 
                                    @Param("keyword") String keyword, 
                                    Pageable pageable);
}