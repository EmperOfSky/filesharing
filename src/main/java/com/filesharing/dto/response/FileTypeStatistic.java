package com.filesharing.dto.response;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FileTypeStatistic {
    private String fileType;
    private Long fileCount;
    private Long totalSize;
    private Double percentage;
    private Long downloadCount;
}