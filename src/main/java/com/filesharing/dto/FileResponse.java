package com.filesharing.dto;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

/**
 * 文件响应DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FileResponse {
    private Long id;
    private String originalName;
    private String storageName;
    private String filePath;
    private Long fileSize;
    private String contentType;
    private String extension;
    private String md5Hash;
    private String status;
    private Boolean isPublic;
    private Integer downloadCount;
    private Integer previewCount;
    private Integer shareCount;
    private LocalDateTime lastDownloadAt;
    private LocalDateTime lastPreviewAt;
    private Long uploaderId;
    private String uploaderName;
    private Long folderId;
    private String folderName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime deletedAt;
}