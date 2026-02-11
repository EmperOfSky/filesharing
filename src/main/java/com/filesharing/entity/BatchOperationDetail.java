package com.filesharing.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * 批量操作详情实体
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "batch_operation_details")
public class BatchOperationDetail {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /**
     * 关联的批量操作
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "batch_operation_id", nullable = false)
    private BatchOperation batchOperation;
    
    /**
     * 项目ID（文件或文件夹ID）
     */
    @Column(name = "item_id")
    private Long itemId;
    
    /**
     * 项目类型：FILE, FOLDER
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "item_type", length = 20)
    private ItemType itemType;
    
    /**
     * 项目名称
     */
    @Column(name = "item_name", length = 255)
    private String itemName;
    
    /**
     * 操作状态：PENDING, PROCESSING, SUCCESS, FAILED
     */
    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private ItemStatus itemStatus = ItemStatus.PENDING;
    
    /**
     * 处理结果消息
     */
    @Column(name = "result_message", length = 500)
    private String resultMessage;
    
    /**
     * 错误信息
     */
    @Column(name = "error_message", length = 1000)
    private String errorMessage;
    
    /**
     * 处理开始时间
     */
    @Column(name = "started_at")
    private LocalDateTime startedAt;
    
    /**
     * 处理完成时间
     */
    @Column(name = "completed_at")
    private LocalDateTime completedAt;
    
    /**
     * 处理耗时（毫秒）
     */
    @Column(name = "processing_time")
    private Long processingTime;
    
    /**
     * 创建时间
     */
    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    /**
     * 项目类型枚举
     */
    public enum ItemType {
        FILE,       // 文件
        FOLDER      // 文件夹
    }
    
    /**
     * 项目状态枚举
     */
    public enum ItemStatus {
        PENDING,        // 待处理
        PROCESSING,     // 处理中
        SUCCESS,        // 成功
        FAILED          // 失败
    }
}