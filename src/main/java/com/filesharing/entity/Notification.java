package com.filesharing.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * 消息通知实体
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "notifications")
public class Notification {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /**
     * 接收通知的用户
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    /**
     * 通知标题
     */
    @Column(name = "title", length = 200)
    private String title;
    
    /**
     * 通知内容
     */
    @Column(name = "content", length = 1000)
    private String content;
    
    /**
     * 通知类型：SYSTEM, SHARE, DOWNLOAD, PREVIEW, SECURITY
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "notification_type", length = 30)
    private NotificationType notificationType;
    
    /**
     * 通知优先级：LOW, NORMAL, HIGH, URGENT
     */
    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private Priority priority = Priority.NORMAL;
    
    /**
     * 关联的文件ID
     */
    @Column(name = "related_file_id")
    private Long relatedFileId;
    
    /**
     * 关联的分享记录ID
     */
    @Column(name = "related_share_id")
    private Long relatedShareId;
    
    /**
     * 关联的文件夹ID
     */
    @Column(name = "related_folder_id")
    private Long relatedFolderId;
    
    /**
     * 是否已读
     */
    @Column(name = "is_read")
    private Boolean isRead = false;
    
    /**
     * 是否已发送
     */
    @Column(name = "is_sent")
    private Boolean isSent = false;
    
    /**
     * 发送渠道：EMAIL, SMS, IN_APP, PUSH
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "send_channel", length = 20)
    private SendChannel sendChannel = SendChannel.IN_APP;
    
    /**
     * 发送时间
     */
    @Column(name = "sent_time")
    private LocalDateTime sentTime;
    
    /**
     * 阅读时间
     */
    @Column(name = "read_time")
    private LocalDateTime readTime;
    
    /**
     * 失效时间
     */
    @Column(name = "expire_time")
    private LocalDateTime expireTime;
    
    /**
     * 重试次数
     */
    @Column(name = "retry_count")
    private Integer retryCount = 0;
    
    /**
     * 最大重试次数
     */
    @Column(name = "max_retry_count")
    private Integer maxRetryCount = 3;
    
    /**
     * 发送结果
     */
    @Column(name = "send_result", length = 500)
    private String sendResult;
    
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
     * 通知类型枚举
     */
    public enum NotificationType {
        SYSTEM,         // 系统通知
        SHARE,          // 分享提醒
        DOWNLOAD,       // 下载通知
        PREVIEW,        // 预览提醒
        SECURITY,       // 安全警告
        STORAGE,        // 存储提醒
        ACTIVITY        // 活动通知
    }
    
    /**
     * 优先级枚举
     */
    public enum Priority {
        LOW,            // 低优先级
        NORMAL,         // 普通优先级
        HIGH,           // 高优先级
        URGENT          // 紧急优先级
    }
    
    /**
     * 发送渠道枚举
     */
    public enum SendChannel {
        EMAIL,          // 邮件
        SMS,            // 短信
        IN_APP,         // 应用内通知
        PUSH            // 推送通知
    }
}