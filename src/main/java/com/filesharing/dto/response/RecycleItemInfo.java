package com.filesharing.dto.response;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecycleItemInfo {
    private Long id;
    private String name;
    private String type; // FILE or FOLDER
    private Long size;
    private String deletedBy;
    private String deletedAt;
    private String originalPath;
}