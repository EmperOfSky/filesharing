package com.filesharing.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * AI分析记录实体
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "ai_analysis_records")
public class AIAnalysisRecord {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /**
     * 关联的AI模型
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "model_id", nullable = false)
    private AIModel aiModel;
    
    /**
     * 分析目标类型：FILE, FOLDER, USER_BEHAVIOR
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", length = 30, nullable = false)
    private TargetType targetType;
    
    /**
     * 目标ID
     */
    @Column(name = "target_id")
    private Long targetId;
    
    /**
     * 分析输入内容
     */
    @Column(name = "input_content", columnDefinition = "TEXT")
    private String inputContent;
    
    /**
     * 分析结果（JSON格式）
     */
    @Column(name = "analysis_result", columnDefinition = "TEXT")
    private String analysisResult;
    
    /**
     * 置信度分数
     */
    @Column(name = "confidence_score")
    private Double confidenceScore;
    
    /**
     * 处理状态：PENDING, PROCESSING, SUCCESS, FAILED
     */
    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private ProcessStatus processStatus = ProcessStatus.PENDING;
    
    /**
     * 错误信息
     */
    @Column(name = "error_message", length = 1000)
    private String errorMessage;
    
    /**
     * 处理耗时（毫秒）
     */
    @Column(name = "processing_time")
    private Long processingTime;
    
    /**
     * 请求用户
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "requested_by")
    private User requestedBy;
    
    /**
     * 创建时间
     */
    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    /**
     * 完成时间
     */
    @Column(name = "completed_at")
    private LocalDateTime completedAt;
    
    /**
     * 目标类型枚举
     */
    public enum TargetType {
        FILE,           // 文件分析
        FOLDER,         // 文件夹分析
        USER_BEHAVIOR,  // 用户行为分析
        CONTENT        // 内容分析
    }
    
    /**
     * 处理状态枚举
     */
    public enum ProcessStatus {
        PENDING,        // 待处理
        PROCESSING,     // 处理中
        SUCCESS,        // 成功
        FAILED          // 失败
    }
}