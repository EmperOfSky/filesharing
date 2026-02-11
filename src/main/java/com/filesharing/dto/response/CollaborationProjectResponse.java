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
public class CollaborationProjectResponse {
    private Long id;
    private String projectName;
    private String description;
    private String tags;
    private String status;
    private UserSimpleResponse owner;
    private Long memberCount;
    private Long documentCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Boolean currentUserIsOwner;
    private Boolean currentUserIsMember;
}