package com.filesharing.dto;

import lombok.Data;

/**
 * 项目统计信息DTO
 */
@Data
public class ProjectStatsDTO {
    
    private Long projectId;
    private String projectName;
    private Long totalMembers;
    private Long totalDocuments;
    private Long totalComments;
    private Long activeEditors; // 当前正在编辑的用户数
    private Long totalVersions; // 文档总版本数
    private Double averageDocumentLength; // 平均文档长度
}