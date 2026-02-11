package com.filesharing.dto.request;

import lombok.Data;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

@Data
public class DocumentCreateRequest {
    @NotBlank(message = "文档标题不能为空")
    @Size(max = 200, message = "文档标题长度不能超过200个字符")
    private String title;
    
    @Size(max = 1000, message = "文档描述长度不能超过1000个字符")
    private String description;
    
    @NotNull(message = "文档类型不能为空")
    private String documentType; // TEXT, SPREADSHEET, PRESENTATION
    
    @NotNull(message = "所属项目ID不能为空")
    private Long projectId;
}