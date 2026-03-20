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
public class CollaborativeDocumentSnapshotResponse {
    private Long id;
    private Long documentId;
    private Integer versionNumber;
    private String title;
    private String commitMessage;
    private UserSimpleResponse createdBy;
    private LocalDateTime createdAt;
}
