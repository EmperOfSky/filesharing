package com.filesharing.dto;

import lombok.Data;
import lombok.Builder;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

/**
 * 文件信息响应DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FileResponse {
    
    private Long id;
    private String originalName;
    private String storageName;
    private Long fileSize;
    private String contentType;
    private String extension;
    private String status;
    private Boolean isPublic;
    private Integer downloadCount;
    private String uploaderName;
    private Long uploaderId;
    private String folderName;
    private Long folderId;
    private String createdAt;
    private String updatedAt;
}