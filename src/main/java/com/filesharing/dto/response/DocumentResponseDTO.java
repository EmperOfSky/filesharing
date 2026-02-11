package com.filesharing.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentResponseDTO {
    private Long id;
    private String documentName;
    private String content;
    private String documentType;
    private Integer version;
    private String status;
    private Long projectId;
    private String projectName;
    private Long createdById;
    private String createdByName;
    private Long lastEditedById;
    private String lastEditedByName;
    private java.time.LocalDateTime lastEditedAt;
    private java.time.LocalDateTime createdAt;
    private java.time.LocalDateTime updatedAt;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserInfoDTO {
        private Long id;
        private String username;
        private String email;
        private String nickname;
        private String avatar;
    }
}