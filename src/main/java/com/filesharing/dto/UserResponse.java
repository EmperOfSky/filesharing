package com.filesharing.dto;

import lombok.Data;
import lombok.Builder;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

/**
 * 用户信息响应DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {
    
    private Long id;
    private String username;
    private String email;
    private String nickname;
    private String avatar;
    private Long storageQuota;
    private Long usedStorage;
    private String status;
    private String role;
    private String lastLoginTime;
    private String createdAt;
}