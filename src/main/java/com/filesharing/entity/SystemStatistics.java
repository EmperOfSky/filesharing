package com.filesharing.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * 系统统计实体
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "system_statistics")
public class SystemStatistics {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /**
     * 统计日期
     */
    @Column(name = "stat_date")
    private LocalDateTime statDate;
    
    /**
     * 总用户数
     */
    @Column(name = "total_users")
    private Long totalUsers = 0L;
    
    /**
     * 新增用户数
     */
    @Column(name = "new_users")
    private Long newUsers = 0L;
    
    /**
     * 活跃用户数
     */
    @Column(name = "active_users")
    private Long activeUsers = 0L;
    
    /**
     * 总文件数
     */
    @Column(name = "total_files")
    private Long totalFiles = 0L;
    
    /**
     * 新增文件数
     */
    @Column(name = "new_files")
    private Long newFiles = 0L;
    
    /**
     * 总文件夹数
     */
    @Column(name = "total_folders")
    private Long totalFolders = 0L;
    
    /**
     * 总存储使用量（字节）
     */
    @Column(name = "total_storage_used")
    private Long totalStorageUsed = 0L;
    
    /**
     * 总存储配额（字节）
     */
    @Column(name = "total_storage_quota")
    private Long totalStorageQuota = 0L;
    
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
     * 平均文件大小（字节）
     */
    @Column(name = "average_file_size")
    private Long averageFileSize = 0L;
    
    /**
     * 最受欢迎的文件类型
     */
    @Column(name = "popular_file_types", length = 500)
    private String popularFileTypes;
    
    /**
     * 系统负载指标
     */
    @Column(name = "system_load")
    private Double systemLoad = 0.0;
    
    /**
     * CPU使用率
     */
    @Column(name = "cpu_usage")
    private Double cpuUsage = 0.0;
    
    /**
     * 内存使用率
     */
    @Column(name = "memory_usage")
    private Double memoryUsage = 0.0;
    
    /**
     * 磁盘使用率
     */
    @Column(name = "disk_usage")
    private Double diskUsage = 0.0;
    
    /**
     * 网络流量（字节）
     */
    @Column(name = "network_traffic")
    private Long networkTraffic = 0L;
    
    /**
     * 错误请求数
     */
    @Column(name = "error_requests")
    private Long errorRequests = 0L;
    
    /**
     * 成功请求数
     */
    @Column(name = "success_requests")
    private Long successRequests = 0L;
    
    /**
     * 平均响应时间（毫秒）
     */
    @Column(name = "avg_response_time")
    private Double avgResponseTime = 0.0;
    
    /**
     * 按小时的详细统计（JSON格式）
     */
    @Column(name = "hourly_stats", columnDefinition = "TEXT")
    private String hourlyStats;
    
    /**
     * 按文件类型的详细统计（JSON格式）
     */
    @Column(name = "file_type_stats", columnDefinition = "TEXT")
    private String fileTypeStats;
    
    /**
     * 按操作类型的详细统计（JSON格式）
     */
    @Column(name = "operation_stats", columnDefinition = "TEXT")
    private String operationStats;
    
    /**
     * 统计创建时间
     */
    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;
}