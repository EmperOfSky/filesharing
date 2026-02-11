package com.filesharing.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * 批量操作任务实体
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "batch_operations")
public class BatchOperation {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /**
     * 操作类型：UPLOAD, DELETE, MOVE, COPY, RENAME
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "operation_type", length = 20)
    private OperationType operationType;
    
    /**
     * 操作状态：PENDING, PROCESSING, COMPLETED, FAILED, CANCELLED
     */
    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private OperationStatus status = OperationStatus.PENDING;
    
    /**
     * 执行用户
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    /**
     * 操作描述
     */
    @Column(name = "description", length = 500)
    private String description;
    
    /**
     * 操作参数（JSON格式）
     */
    @Column(name = "operation_params", columnDefinition = "TEXT")
    private String operationParams;
    
    /**
     * 总项目数
     */
    @Column(name = "total_items")
    private Integer totalItems = 0;
    
    /**
     * 已处理项目数
     */
    @Column(name = "processed_items")
    private Integer processedItems = 0;
    
    /**
     * 成功项目数
     */
    @Column(name = "success_items")
    private Integer successItems = 0;
    
    /**
     * 失败项目数
     */
    @Column(name = "failed_items")
    private Integer failedItems = 0;
    
    /**
     * 错误信息（JSON格式）
     */
    @Column(name = "error_details", columnDefinition = "TEXT")
    private String errorDetails;
    
    /**
     * 进度百分比
     */
    @Column(name = "progress_percentage")
    private Double progressPercentage = 0.0;
    
    /**
     * 是否可取消
     */
    @Column(name = "is_cancellable")
    private Boolean isCancellable = true;
    
    /**
     * 开始时间
     */
    @Column(name = "started_at")
    private LocalDateTime startedAt;
    
    /**
     * 完成时间
     */
    @Column(name = "completed_at")
    private LocalDateTime completedAt;
    
    /**
     * 预计完成时间
     */
    @Column(name = "estimated_completion")
    private LocalDateTime estimatedCompletion;
    
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
     * 操作类型枚举
     */
    public enum OperationType {
        UPLOAD,     // 批量上传
        DELETE,     // 批量删除
        MOVE,       // 批量移动
        COPY,       // 批量复制
        RENAME,     // 批量重命名
        COMPRESS,   // 批量压缩
        EXTRACT     // 批量解压
    }
    
    /**
     * 操作状态枚举
     */
    public enum OperationStatus {
        PENDING,        // 待处理
        PROCESSING,     // 处理中
        COMPLETED,      // 已完成
        FAILED,         // 失败
        CANCELLED       // 已取消
    }
}