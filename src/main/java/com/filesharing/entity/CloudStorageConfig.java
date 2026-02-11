package com.filesharing.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * 云存储配置实体
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "cloud_storage_configs")
public class CloudStorageConfig {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /**
     * 配置名称
     */
    @Column(name = "config_name", length = 100, unique = true)
    private String configName;
    
    /**
     * 云服务商类型：ALIYUN_OSS, AWS_S3, TENCENT_COS, QINIU_KODO
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "provider_type", length = 30)
    private ProviderType providerType;
    
    /**
     * 访问密钥ID
     */
    @Column(name = "access_key_id", length = 100)
    private String accessKeyId;
    
    /**
     * 访问密钥Secret
     */
    @Column(name = "access_key_secret", length = 200)
    private String accessKeySecret;
    
    /**
     * 存储桶名称
     */
    @Column(name = "bucket_name", length = 100)
    private String bucketName;
    
    /**
     * 地域/区域
     */
    @Column(name = "region", length = 50)
    private String region;
    
    /**
     * 自定义域名（CDN加速域名）
     */
    @Column(name = "custom_domain", length = 200)
    private String customDomain;
    
    /**
     * 基础路径前缀
     */
    @Column(name = "base_path", length = 200)
    private String basePath;
    
    /**
     * 是否启用
     */
    @Column(name = "is_enabled")
    private Boolean isEnabled = false;
    
    /**
     * 是否为默认存储
     */
    @Column(name = "is_default")
    private Boolean isDefault = false;
    
    /**
     * 存储容量限制（字节）
     */
    @Column(name = "storage_limit")
    private Long storageLimit;
    
    /**
     * 已使用存储量（字节）
     */
    @Column(name = "used_storage")
    private Long usedStorage = 0L;
    
    /**
     * 文件大小限制（字节）
     */
    @Column(name = "file_size_limit")
    private Long fileSizeLimit;
    
    /**
     * 支持的文件类型
     */
    @Column(name = "allowed_file_types", length = 500)
    private String allowedFileTypes;
    
    /**
     * 配置描述
     */
    @Column(name = "description", length = 500)
    private String description;
    
    /**
     * 连接测试状态
     */
    @Column(name = "connection_status", length = 20)
    private String connectionStatus;
    
    /**
     * 最后测试时间
     */
    @Column(name = "last_test_time")
    private LocalDateTime lastTestTime;
    
    /**
     * 测试结果信息
     */
    @Column(name = "test_result", length = 1000)
    private String testResult;
    
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
     * 云服务商类型枚举
     */
    public enum ProviderType {
        ALIYUN_OSS,     // 阿里云OSS
        AWS_S3,         // AWS S3
        TENCENT_COS,    // 腾讯云COS
        QINIU_KODO,     // 七牛云Kodo
        MINIO,          // MinIO
        LOCAL           // 本地存储
    }
}