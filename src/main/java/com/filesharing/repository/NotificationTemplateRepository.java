package com.filesharing.repository;

import com.filesharing.entity.NotificationTemplate;
import com.filesharing.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 通知模板Repository接口
 */
@Repository
public interface NotificationTemplateRepository extends JpaRepository<NotificationTemplate, Long> {
    
    /**
     * 根据模板名称查找模板
     */
    Optional<NotificationTemplate> findByTemplateName(String templateName);
    
    /**
     * 根据通知类型查找启用的模板
     */
    @Query("SELECT nt FROM NotificationTemplate nt WHERE nt.notificationType = :type AND nt.isEnabled = true")
    List<NotificationTemplate> findEnabledTemplatesByType(@Param("type") Notification.NotificationType type);
    
    /**
     * 查找所有启用的模板
     */
    @Query("SELECT nt FROM NotificationTemplate nt WHERE nt.isEnabled = true")
    List<NotificationTemplate> findAllEnabledTemplates();
    
    /**
     * 检查模板名称是否存在
     */
    boolean existsByTemplateName(String templateName);
    
    /**
     * 根据通知类型统计模板数量
     */
    @Query("SELECT nt.notificationType, COUNT(nt) FROM NotificationTemplate nt GROUP BY nt.notificationType")
    List<Object[]> countTemplatesByType();
    
    /**
     * 查找支持特定发送渠道的模板
     */
    @Query("SELECT nt FROM NotificationTemplate nt WHERE nt.isEnabled = true " +
           "AND nt.supportedChannels LIKE %:channel%")
    List<NotificationTemplate> findTemplatesSupportingChannel(@Param("channel") String channel);
}