package com.filesharing.dto.response;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

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
    private LocalDateTime lastEditedAt;
    private Integer versionCount;
}