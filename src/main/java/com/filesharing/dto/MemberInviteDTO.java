package com.filesharing.dto;

import com.filesharing.entity.ProjectMember;
import lombok.Data;
import javax.validation.constraints.NotNull;

/**
 * 项目成员邀请DTO
 */
@Data
public class MemberInviteDTO {
    
    @NotNull(message = "用户ID不能为空")
    private Long userId;
    
    @NotNull(message = "角色不能为空")
    private ProjectMember.MemberRole role;
    
    private String permissions = "READ";
}