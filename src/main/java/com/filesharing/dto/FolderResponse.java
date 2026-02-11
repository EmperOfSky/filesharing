package com.filesharing.dto;

import lombok.Data;
import lombok.Builder;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 文件夹信息响应DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FolderResponse {
    
    private Long id;
    private String name;
    private String description;
    private Long parentId;
    private String parentName;
    private Long ownerId;
    private String ownerName;
    private Boolean isPublic;
    private String folderPath;
    private Integer fileCount;
    private Integer subFolderCount;
    private List<FolderResponse> children;
    private String createdAt;
    private String updatedAt;
}