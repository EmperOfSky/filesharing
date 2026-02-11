package com.filesharing.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * 云存储文件映射实体
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "cloud_file_mappings")
public class CloudFileMapping {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /**
     * 本地文件ID
     */
    @Column(name = "local_file_id")
    private Long localFileId;
    
    /**
     * 云存储配置ID
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "storage_config_id", nullable = false)
    private CloudStorageConfig storageConfig;
    
    /**
     * 云端文件Key/路径
     */
    @Column(name = "cloud_key", length = 500)
    private String cloudKey;
    
    /**
     * 云端文件URL
     */
    @Column(name = "cloud_url", length = 1000)
    private String cloudUrl;
    
    /**
     * 文件ETag（用于一致性检查）
     */
    @Column(name = "etag", length = 100)
    private String etag;
    
    /**
     * 文件版本ID
     */
    @Column(name = "version_id", length = 100)
    private String versionId;
    
    /**
     * 存储类别
     */
    @Column(name = "storage_class", length = 50)
    private String storageClass;
    
    /**
     * 是否已上传到云端
     */
    @Column(name = "is_uploaded")
    private Boolean isUploaded = false;
    
    /**
     * 上传时间
     */
    @Column(name = "uploaded_at")
    private LocalDateTime uploadedAt;
    
    /**
     * 上传尝试次数
     */
    @Column(name = "upload_attempts")
    private Integer uploadAttempts = 0;
    
    /**
     * 最大重试次数
     */
    @Column(name = "max_retry_attempts")
    private Integer maxRetryAttempts = 3;
    
    /**
     * 上传错误信息
     */
    @Column(name = "upload_error", length = 1000)
    private String uploadError;
    
    /**
     * 是否启用CDN加速
     */
    @Column(name = "cdn_enabled")
    private Boolean cdnEnabled = false;
    
    /**
     * CDN URL
     */
    @Column(name = "cdn_url", length = 1000)
    private String cdnUrl;
    
    /**
     * 文件生命周期状态
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "lifecycle_status", length = 30)
    private LifecycleStatus lifecycleStatus = LifecycleStatus.ACTIVE;
    
    /**
     * 创建时间
     */
    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    /**
     * 生命周期状态枚举
     */
    public enum LifecycleStatus {
        ACTIVE,         // 活跃状态
        INFREQUENT,     // 低频访问
        ARCHIVE,        // 归档存储
        DELETED         // 已删除
    }
}