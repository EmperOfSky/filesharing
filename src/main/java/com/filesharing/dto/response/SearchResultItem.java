package com.filesharing.dto.response;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SearchResultItem {
    private Long id;
    private String name;
    private String type; // file or folder
    private String path;
    private Long size;
    private String fileType;
    private String uploader;
    private String createdAt;
    private Double score;
    private String highlight;
}