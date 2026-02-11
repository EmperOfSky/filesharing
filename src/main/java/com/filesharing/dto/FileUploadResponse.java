package com.filesharing.dto;

import lombok.Data;
import lombok.Builder;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

/**
 * 文件上传响应DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FileUploadResponse {
    
    private Long id;
    private String originalName;
    private String storageName;
    private Long fileSize;
    private String contentType;
    private String downloadUrl;
    private String previewUrl;
    private Boolean isNewlyUploaded;
    private String message;
}