package com.filesharing.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserCollaborationStatsResponse {
    private Long userId;
    private String username;
    private Long totalProjects;
    private Long totalDocuments;
    private Long totalComments;
    private Long projectsOwned;
    private Long projectsParticipated;
}