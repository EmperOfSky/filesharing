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
public class CollaborativeDocumentResponse {
    private Long id;
    private String title;
    private String description;
    private String documentType;
    private String status;
    private String content;
    private UserSimpleResponse createdBy;
    private UserSimpleResponse lastEditedBy;
    private Long projectId;
    private String projectName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime lastEditedAt;
    private Boolean isLocked;
    private UserSimpleResponse lockedBy;
    private Long commentCount;
    private Boolean canEdit;
    private Boolean canDelete;
}