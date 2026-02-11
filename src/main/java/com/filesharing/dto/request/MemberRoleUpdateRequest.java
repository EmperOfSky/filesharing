package com.filesharing.dto.request;

import lombok.Data;
import javax.validation.constraints.NotNull;

@Data
public class MemberRoleUpdateRequest {
    @NotNull(message = "角色不能为空")
    private String role; // ADMIN, EDITOR, VIEWER
}