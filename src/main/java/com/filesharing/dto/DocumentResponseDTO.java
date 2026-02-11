package com.filesharing.dto;

import com.filesharing.entity.CollaborativeDocument;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 协作文档响应DTO
 */
@Data
public class DocumentResponseDTO {
    
    private Long id;
    private String documentName;
    private String content;
    private CollaborativeDocument.DocumentType documentType;
    private UserInfoDTO owner;
    private ProjectInfoDTO project;
    private List<String> tags;
    private Integer versionNumber;
    private Long editCount;
    private List<UserInfoDTO> editingUsers;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime lastEditAt;
    
    /**
     * 用户信息内部类
     */
    @Data
    public static class UserInfoDTO {
        private Long id;
        private String username;
        private String email;
        private String avatar;
    }
    
    /**
     * 项目信息内部类
     */
    @Data
    public static class ProjectInfoDTO {
        private Long id;
        private String projectName;
    }
}