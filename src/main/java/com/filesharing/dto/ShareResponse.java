package com.filesharing.dto;

import lombok.Data;
import lombok.Builder;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

/**
 * 分享信息响应DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShareResponse {
    
    private Long id;
    private String shareKey;
    private String title;
    private String description;
    private String shareType;
    private Object sharedContent; // 可能是FileResponse或FolderResponse
    private String sharerName;
    private Long sharerId;
    private String expireTime;
    private Integer maxAccessCount;
    private Integer currentAccessCount;
    private String status;
    private Boolean allowDownload;
    private Boolean requiresPassword;
    private String accessUrl;
    private String shortLink;
    private String downloadUrl;
    private String createdAt;
    private String updatedAt;
}