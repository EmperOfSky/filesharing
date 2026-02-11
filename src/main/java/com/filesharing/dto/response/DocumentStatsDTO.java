package com.filesharing.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentStatsDTO {
    private Long documentId;
    private Long viewCount;
    private Long editCount;
    private Long commentCount;
    private Long shareCount;
    private java.time.LocalDateTime lastEditedAt;
    private Integer versionCount;
}