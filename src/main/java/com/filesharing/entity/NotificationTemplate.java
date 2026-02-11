package com.filesharing.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * 通知模板实体
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "notification_templates")
public class NotificationTemplate {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /**
     * 模板名称
     */
    @Column(name = "template_name", length = 100, unique = true)
    private String templateName;
    
    /**
     * 模板标题
     */
    @Column(name = "title_template", length = 200)
    private String titleTemplate;
    
    /**
     * 模板内容
     */
    @Column(name = "content_template", columnDefinition = "TEXT")
    private String contentTemplate;
    
    /**
     * 通知类型
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "notification_type", length = 30)
    private Notification.NotificationType notificationType;
    
    /**
     * 是否启用
     */
    @Column(name = "is_enabled")
    private Boolean isEnabled = true;
    
    /**
     * 支持的发送渠道
     */
    @Column(name = "supported_channels", length = 100)
    private String supportedChannels;
    
    /**
     * 模板变量说明
     */
    @Column(name = "variables_description", columnDefinition = "TEXT")
    private String variablesDescription;
    
    /**
     * 创建人
     */
    @Column(name = "created_by", length = 50)
    private String createdBy;
    
    /**
     * 创建时间
     */
    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    /**
     * 更新时间
     */
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    /**
     * 版本号
     */
    @Column(name = "version")
    private Integer version = 1;
}