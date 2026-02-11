package com.filesharing.dto;

import com.filesharing.entity.CollaborationProject;
import lombok.Data;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import java.util.ArrayList;
import java.util.List;

/**
 * 协作项目创建DTO
 */
@Data
public class ProjectCreateDTO {
    
    @NotBlank(message = "项目名称不能为空")
    @Size(max = 100, message = "项目名称长度不能超过100个字符")
    private String projectName;
    
    @Size(max = 500, message = "项目描述长度不能超过500个字符")
    private String description;
    
    private CollaborationProject.ProjectStatus status = CollaborationProject.ProjectStatus.ACTIVE;
    
    @Size(max = 10, message = "标签数量不能超过10个")
    private List<String> tags = new ArrayList<>();
    
    // 默认构造函数
    public ProjectCreateDTO() {
        this.tags = new ArrayList<>();
    }
}