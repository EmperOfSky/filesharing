package com.filesharing.dto;

import com.filesharing.entity.ProjectMember;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 项目成员响应DTO
 */
@Data
public class MemberResponseDTO {
    
    private Long id;
    private UserInfoDTO user;
    private ProjectMember.MemberRole role;
    private String permissions;
    private LocalDateTime joinedAt;
    private LocalDateTime lastActiveAt;
    
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
}