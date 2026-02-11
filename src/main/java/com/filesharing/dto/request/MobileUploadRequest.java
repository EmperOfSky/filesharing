package com.filesharing.dto.request;

import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.constraints.NotNull;

@Data
public class MobileUploadRequest {
    @NotNull(message = "文件不能为空")
    private MultipartFile file;
    
    private Long folderId;
    private String description;
    private Boolean isPublic = false;
    private String tags;
}