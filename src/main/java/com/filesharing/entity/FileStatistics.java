package com.filesharing.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * 文件统计实体
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "file_statistics")
public class FileStatistics {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /**
     * 关联的文件
     */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "file_id", nullable = false)
    private FileEntity file;
    
    /**
     * 总下载次数
     */
    @Column(name = "total_downloads")
    private Long totalDownloads = 0L;
    
    /**
     * 总预览次数
     */
    @Column(name = "total_previews")
    private Long totalPreviews = 0L;
    
    /**
     * 总分享次数
     */
    @Column(name = "total_shares")
    private Long totalShares = 0L;
    
    /**
     * 唯一下载用户数
     */
    @Column(name = "unique_downloaders")
    private Long uniqueDownloaders = 0L;
    
    /**
     * 唯一预览用户数
     */
    @Column(name = "unique_previewers")
    private Long uniquePreviewers = 0L;
    
    /**
     * 平均评分
     */
    @Column(name = "average_rating")
    private Double averageRating = 0.0;
    
    /**
     * 评分次数
     */
    @Column(name = "rating_count")
    private Long ratingCount = 0L;
    
    /**
     * 文件热度评分（综合计算）
     */
    @Column(name = "popularity_score")
    private Double popularityScore = 0.0;
    
    /**
     * 最后下载时间
     */
    @Column(name = "last_download_time")
    private LocalDateTime lastDownloadTime;
    
    /**
     * 最后预览时间
     */
    @Column(name = "last_preview_time")
    private LocalDateTime lastPreviewTime;
    
    /**
     * 最后分享时间
     */
    @Column(name = "last_share_time")
    private LocalDateTime lastShareTime;
    
    /**
     * 统计更新时间
     */
    @CreationTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    /**
     * 按日统计的JSON数据
     */
    @Column(name = "daily_stats", columnDefinition = "TEXT")
    private String dailyStats;
    
    /**
     * 按周统计的JSON数据
     */
    @Column(name = "weekly_stats", columnDefinition = "TEXT")
    private String weeklyStats;
    
    /**
     * 按月统计的JSON数据
     */
    @Column(name = "monthly_stats", columnDefinition = "TEXT")
    private String monthlyStats;
}