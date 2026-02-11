package com.filesharing.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProjectMemberResponse {
    private Long id;
    private UserSimpleResponse user;
    private String role;
    private String status;
    private String invitedByEmail;
    private Boolean canEdit;
    private Boolean canManage;
}