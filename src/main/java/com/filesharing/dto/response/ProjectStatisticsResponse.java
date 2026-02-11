package com.filesharing.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProjectStatisticsResponse {
    private Long projectId;
    private String projectName;
    private Long totalMembers;
    private Long totalDocuments;
    private Long totalComments;
    private Long activeDocuments;
    private Long lockedDocuments;
}