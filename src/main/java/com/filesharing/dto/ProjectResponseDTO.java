package com.filesharing.dto;

import com.filesharing.entity.CollaborationProject;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 协作项目响应DTO
 */
@Data
public class ProjectResponseDTO {
    
    private Long id;
    private String projectName;
    private String description;
    private UserInfoDTO owner;
    private CollaborationProject.ProjectStatus status;
    private List<String> tags;
    private Long memberCount;
    private Long documentCount;
    private Long commentCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime lastActivityAt;
    
    /**
     * 用户信息内部类
     */
    @Data
    public static class UserInfoDTO {
        private Long id;
        private String username;
        private String email;
    }
}