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
public class FileSimpleResponse {
    private Long id;
    private String fileName;
    private String fileType;
    private Long fileSize;
    private String fileIcon;
    private Boolean isFavorite;
    private Boolean isOfflineAvailable;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String folderPath;
}