package com.filesharing.dto.response;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SearchSuggestion {
    private String text;
    private String type; // file, folder, tag
    private Double relevance;
    private Long id;
}