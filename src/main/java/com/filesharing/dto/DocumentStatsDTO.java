package com.filesharing.dto;

import lombok.Data;

/**
 * 文档统计信息DTO
 */
@Data
public class DocumentStatsDTO {
    
    private Long documentId;
    private String documentName;
    private Long versionCount;
    private Long editCount;
    private Long commentCount;
    private Long editorCount; // 编辑过此文档的用户数
    private String currentContentLength; // 当前内容长度
    private String averageVersionSize; // 平均版本大小
}