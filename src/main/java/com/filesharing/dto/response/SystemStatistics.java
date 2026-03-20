package com.filesharing.dto.response;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SystemStatistics {
    private Long totalUsers;
    private Long activeUsers;
    private Long totalFiles;
    private Long totalFolders;
    private Long totalStorageUsed;
    private Long totalStorageQuota;
    private Long totalDownloads;
    private Long totalPreviews;
    private Long totalShares;
    private Double averageFileSize;
    private String popularFileTypes;
    private Double systemLoad;
    private Double cpuUsage;
    private Double memoryUsage;
    private Double diskUsage;
    private Long networkTraffic;
    private String statDate;
}