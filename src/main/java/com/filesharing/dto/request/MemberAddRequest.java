package com.filesharing.dto.request;

import lombok.Data;
import javax.validation.constraints.Email;
import javax.validation.constraints.NotNull;

@Data
public class MemberAddRequest {
    @Email(message = "邮箱格式不正确")
    @NotNull(message = "邮箱不能为空")
    private String email;
    
    @NotNull(message = "角色不能为空")
    private String role; // ADMIN, EDITOR, VIEWER
}