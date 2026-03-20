package com.filesharing.dto.request;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import javax.validation.constraints.NotBlank;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MemberRoleUpdateRequest {
    
    @NotBlank(message = "角色不能为空")
    private String role;
    
    private String permissions;
}