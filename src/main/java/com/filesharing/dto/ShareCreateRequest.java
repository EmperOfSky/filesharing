package com.filesharing.dto;

import lombok.Data;
import lombok.Builder;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.time.LocalDateTime;

/**
 * 分享创建请求DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShareCreateRequest {
    
    @NotNull(message = "分享内容ID不能为空")
    private Long contentId;
    
    @NotBlank(message = "分享类型不能为空")
    private String shareType; // FILE 或 FOLDER
    
    @Size(max = 200, message = "分享标题长度不能超过200个字符")
    private String title;
    
    @Size(max = 500, message = "分享描述长度不能超过500个字符")
    private String description;
    
    @Size(max = 100, message = "访问密码长度不能超过100个字符")
    private String password;
    
    private LocalDateTime expireTime;
    
    private Integer maxAccessCount;
    
    private Boolean allowDownload = true;
}