package com.filesharing.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 智能推荐实体
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "smart_recommendations")
public class SmartRecommendation {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /**
     * 推荐给的用户
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    /**
     * 推荐类型：FILE, FOLDER, TAG, COLLABORATION
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "recommendation_type", length = 30, nullable = false)
    private RecommendationType recommendationType;
    
    /**
     * 推荐项ID
     */
    @Column(name = "item_id")
    private Long itemId;
    
    /**
     * 推荐理由
     */
    @Column(name = "reason", length = 500)
    private String reason;
    
    /**
     * 相关度分数
     */
    @Column(name = "relevance_score")
    private Double relevanceScore;
    
    /**
     * 推荐来源：AI_MODEL, USER_BEHAVIOR, COLLABORATION_PATTERN
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", length = 30)
    private SourceType sourceType;
    
    /**
     * 来源模型ID
     */
    @Column(name = "source_model_id")
    private Long sourceModelId;
    
    /**
     * 是否已被查看
     */
    @Column(name = "is_viewed")
    private Boolean isViewed = false;
    
    /**
     * 是否已被采纳
     */
    @Column(name = "is_adopted")
    private Boolean isAdopted = false;
    
    /**
     * 查看时间
     */
    @Column(name = "viewed_at")
    private LocalDateTime viewedAt;
    
    /**
     * 采纳时间
     */
    @Column(name = "adopted_at")
    private LocalDateTime adoptedAt;
    
    /**
     * 标签列表（JSON格式）
     */
    @Column(name = "tags", columnDefinition = "TEXT")
    private String tags;
    
    /**
     * 创建时间
     */
    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    /**
     * 过期时间
     */
    @Column(name = "expire_at")
    private LocalDateTime expireAt;
    
    /**
     * 推荐类型枚举
     */
    public enum RecommendationType {
        FILE,           // 文件推荐
        FOLDER,         // 文件夹推荐
        TAG,            // 标签推荐
        COLLABORATION,  // 协作推荐
        SEARCH_RESULT   // 搜索结果推荐
    }
    
    /**
     * 来源类型枚举
     */
    public enum SourceType {
        AI_MODEL,               // AI模型推荐
        USER_BEHAVIOR,          // 用户行为分析
        COLLABORATION_PATTERN,  // 协作模式分析
        CONTENT_SIMILARITY      // 内容相似度分析
    }
}