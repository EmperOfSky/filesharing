package com.filesharing.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * AI模型配置实体
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "ai_models")
public class AIModel {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /**
     * 模型名称
     */
    @Column(name = "model_name", length = 100, nullable = false, unique = true)
    private String modelName;
    
    /**
     * 模型提供商：OPENAI, BAIDU, ALIBABA, CUSTOM
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "provider", length = 30, nullable = false)
    private Provider provider;
    
    /**
     * 模型类型：TEXT_CLASSIFICATION, IMAGE_RECOGNITION, CONTENT_ANALYSIS, RECOMMENDATION
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "model_type", length = 30, nullable = false)
    private ModelType modelType;
    
    /**
     * API端点URL
     */
    @Column(name = "api_endpoint", length = 500)
    private String apiEndpoint;
    
    /**
     * API密钥
     */
    @Column(name = "api_key", length = 200)
    private String apiKey;
    
    /**
     * 模型版本
     */
    @Column(name = "model_version", length = 50)
    private String modelVersion;
    
    /**
     * 配置参数（JSON格式）
     */
    @Column(name = "config_params", columnDefinition = "TEXT")
    private String configParams;
    
    /**
     * 是否启用
     */
    @Column(name = "is_enabled")
    private Boolean isEnabled = true;
    
    /**
     * 最大请求频率（每分钟）
     */
    @Column(name = "max_requests_per_minute")
    private Integer maxRequestsPerMinute = 60;
    
    /**
     * 当前使用次数
     */
    @Column(name = "usage_count")
    private Long usageCount = 0L;
    
    /**
     * 描述信息
     */
    @Column(length = 500)
    private String description;
    
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
     * 模型提供商枚举
     */
    public enum Provider {
        OPENAI,     // OpenAI
        BAIDU,      // 百度AI
        ALIBABA,    // 阿里云AI
        CUSTOM      // 自定义模型
    }
    
    /**
     * 模型类型枚举
     */
    public enum ModelType {
        TEXT_CLASSIFICATION,    // 文本分类
        IMAGE_RECOGNITION,      // 图像识别
        CONTENT_ANALYSIS,       // 内容分析
        RECOMMENDATION,         // 推荐系统
        SMART_SEARCH,           // 智能搜索
        AUTOMATIC_TAGGING       // 自动打标签
    }
}