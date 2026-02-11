package com.filesharing.dto.request;

import lombok.Data;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

@Data
public class ProjectCreateRequest {
    @NotBlank(message = "项目名称不能为空")
    @Size(max = 100, message = "项目名称长度不能超过100个字符")
    private String projectName;
    
    @Size(max = 500, message = "项目描述长度不能超过500个字符")
    private String description;
    
    @Size(max = 50, message = "项目标签长度不能超过50个字符")
    private String tags;
}