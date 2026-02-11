package com.filesharing.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FolderSimpleResponse {
    private Long id;
    private String name;
    private String icon;
    private Long fileCount;
    private Long subFolderCount;
    private Boolean isQuickAccess;
    private LocalDateTime createdAt;
    private Long parentId;
    private String fullPath;
}