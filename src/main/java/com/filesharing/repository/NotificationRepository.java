package com.filesharing.repository;

import com.filesharing.entity.Notification;
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
 * 通知Repository接口
 */
@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {
    
    /**
     * 根据用户查找通知（按时间倒序）
     */
    Page<Notification> findByUserOrderByCreatedAtDesc(User user, Pageable pageable);
    
    /**
     * 根据用户和已读状态查找通知
     */
    Page<Notification> findByUserAndIsRead(User user, Boolean isRead, Pageable pageable);
    
    /**
     * 根据用户和通知类型查找通知
     */
    @Query("SELECT n FROM Notification n WHERE n.user = :user AND n.notificationType = :type " +
           "ORDER BY n.createdAt DESC")
    Page<Notification> findByUserAndNotificationType(@Param("user") User user, 
                                                    @Param("type") Notification.NotificationType type, 
                                                    Pageable pageable);
    
    /**
     * 查找未读通知数量
     */
    @Query("SELECT COUNT(n) FROM Notification n WHERE n.user = :user AND n.isRead = false")
    Long countUnreadNotifications(@Param("user") User user);
    
    /**
     * 查找指定时间范围内未发送的通知
     */
    @Query("SELECT n FROM Notification n WHERE n.isSent = false AND n.createdAt <= :beforeTime " +
           "ORDER BY n.priority DESC, n.createdAt ASC")
    List<Notification> findUnsentNotifications(@Param("beforeTime") LocalDateTime beforeTime);
    
    /**
     * 查找需要重试的通知
     */
    @Query("SELECT n FROM Notification n WHERE n.isSent = false AND n.retryCount < n.maxRetryCount " +
           "AND n.createdAt <= :beforeTime ORDER BY n.createdAt ASC")
    List<Notification> findRetryableNotifications(@Param("beforeTime") LocalDateTime beforeTime);
    
    /**
     * 查找已过期的通知
     */
    @Query("SELECT n FROM Notification n WHERE n.expireTime < :currentTime AND n.isRead = false")
    List<Notification> findExpiredNotifications(@Param("currentTime") LocalDateTime currentTime);
    
    /**
     * 统计用户各类通知数量
     */
    @Query("SELECT n.notificationType, COUNT(n) FROM Notification n WHERE n.user = :user " +
           "GROUP BY n.notificationType")
    List<Object[]> countNotificationsByType(@Param("user") User user);
    
    /**
     * 获取用户通知统计
     */
    @Query("SELECT " +
           "COUNT(n) as total, " +
           "SUM(CASE WHEN n.isRead = false THEN 1 ELSE 0 END) as unread, " +
           "SUM(CASE WHEN n.priority = 'URGENT' AND n.isRead = false THEN 1 ELSE 0 END) as urgent " +
           "FROM Notification n WHERE n.user = :user")
    Object[] getUserNotificationStats(@Param("user") User user);
    
    /**
     * 查找紧急未读通知
     */
    @Query("SELECT n FROM Notification n WHERE n.user = :user AND n.isRead = false " +
           "AND n.priority = 'URGENT' ORDER BY n.createdAt DESC")
    Page<Notification> findUrgentUnreadNotifications(@Param("user") User user, Pageable pageable);
    
    /**
     * 清理过期通知
     */
    @Query("SELECT n FROM Notification n WHERE n.expireTime < :beforeTime")
    List<Notification> findNotificationsToCleanup(@Param("beforeTime") LocalDateTime beforeTime);
    
    /**
     * 获取通知发送统计
     */
    @Query("SELECT n.sendChannel, COUNT(n), " +
           "SUM(CASE WHEN n.isSent = true THEN 1 ELSE 0 END) as sent " +
           "FROM Notification n WHERE n.createdAt >= :since GROUP BY n.sendChannel")
    List<Object[]> getSendStatistics(@Param("since") LocalDateTime since);
}