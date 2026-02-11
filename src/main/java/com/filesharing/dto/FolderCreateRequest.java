package com.filesharing.dto;

import lombok.Data;
import lombok.Builder;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

/**
 * 文件夹创建请求DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FolderCreateRequest {
    
    @NotBlank(message = "文件夹名称不能为空")
    @Size(max = 100, message = "文件夹名称长度不能超过100个字符")
    private String name;
    
    @Size(max = 500, message = "文件夹描述长度不能超过500个字符")
    private String description;
    
    private Long parentId;
    
    private Boolean isPublic = false;
}