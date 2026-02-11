package com.filesharing.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * 回收站记录实体
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "recycle_bin")
public class RecycleBin {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /**
     * 被删除的项目ID（文件或文件夹）
     */
    @Column(name = "item_id", nullable = false)
    private Long itemId;
    
    /**
     * 项目类型：FILE, FOLDER
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "item_type", length = 20)
    private ItemType itemType;
    
    /**
     * 原始名称
     */
    @Column(name = "original_name", length = 255)
    private String originalName;
    
    /**
     * 原始路径
     */
    @Column(name = "original_path", length = 1000)
    private String originalPath;
    
    /**
     * 删除前的父级ID
     */
    @Column(name = "original_parent_id")
    private Long originalParentId;
    
    /**
     * 文件大小（如果是文件）
     */
    @Column(name = "file_size")
    private Long fileSize;
    
    /**
     * 文件类型（如果是文件）
     */
    @Column(name = "file_type", length = 100)
    private String fileType;
    
    /**
     * 删除用户
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "deleted_by", nullable = false)
    private User deletedBy;
    
    /**
     * 删除时间
     */
    @Column(name = "deleted_at", nullable = false)
    private LocalDateTime deletedAt;
    
    /**
     * 过期时间（自动清理时间）
     */
    @Column(name = "expire_at")
    private LocalDateTime expireAt;
    
    /**
     * 是否可以恢复
     */
    @Column(name = "is_recoverable")
    private Boolean isRecoverable = true;
    
    /**
     * 恢复目标位置ID
     */
    @Column(name = "restore_target_id")
    private Long restoreTargetId;
    
    /**
     * 删除原因
     */
    @Column(name = "delete_reason", length = 500)
    private String deleteReason;
    
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
}