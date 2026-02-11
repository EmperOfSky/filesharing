package com.filesharing.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * 用户行为统计实体
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "user_behavior_stats")
public class UserBehaviorStatistics {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /**
     * 关联的用户
     */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    /**
     * 总上传文件数
     */
    @Column(name = "total_uploads")
    private Long totalUploads = 0L;
    
    /**
     * 总下载文件数
     */
    @Column(name = "total_downloads")
    private Long totalDownloads = 0L;
    
    /**
     * 总预览文件数
     */
    @Column(name = "total_previews")
    private Long totalPreviews = 0L;
    
    /**
     * 总创建文件夹数
     */
    @Column(name = "total_folders_created")
    private Long totalFoldersCreated = 0L;
    
    /**
     * 总分享次数
     */
    @Column(name = "total_shares")
    private Long totalShares = 0L;
    
    /**
     * 总存储使用量（字节）
     */
    @Column(name = "total_storage_used")
    private Long totalStorageUsed = 0L;
    
    /**
     * 平均文件大小（字节）
     */
    @Column(name = "average_file_size")
    private Long averageFileSize = 0L;
    
    /**
     * 最常使用的文件类型
     */
    @Column(name = "favorite_file_type", length = 50)
    private String favoriteFileType;
    
    /**
     * 活跃天数
     */
    @Column(name = "active_days")
    private Long activeDays = 0L;
    
    /**
     * 最后活跃时间
     */
    @Column(name = "last_active_time")
    private LocalDateTime lastActiveTime;
    
    /**
     * 首次活跃时间
     */
    @Column(name = "first_active_time")
    private LocalDateTime firstActiveTime;
    
    /**
     * 用户等级（基于活跃度计算）
     */
    @Column(name = "user_level")
    private Integer userLevel = 1;
    
    /**
     * 用户积分
     */
    @Column(name = "user_points")
    private Long userPoints = 0L;
    
    /**
     * 每日活跃统计（JSON格式）
     */
    @Column(name = "daily_activity", columnDefinition = "TEXT")
    private String dailyActivity;
    
    /**
     * 文件类型分布统计（JSON格式）
     */
    @Column(name = "file_type_distribution", columnDefinition = "TEXT")
    private String fileTypeDistribution;
    
    /**
     * 操作类型分布统计（JSON格式）
     */
    @Column(name = "operation_distribution", columnDefinition = "TEXT")
    private String operationDistribution;
    
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
     * 统计周期开始时间
     */
    @Column(name = "period_start")
    private LocalDateTime periodStart;
    
    /**
     * 统计周期结束时间
     */
    @Column(name = "period_end")
    private LocalDateTime periodEnd;
}