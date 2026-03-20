package com.filesharing.dto.response;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserStatistics {
    private Long userId;
    private String username;
    private Long totalUploads;
    private Long totalDownloads;
    private Long totalPreviews;
    private Long totalShares;
    private Long storageUsed;
    private Long storageQuota;
    private String favoriteFileType;
    private Integer userLevel;
    private Long userPoints;
    private String lastActiveTime;
}