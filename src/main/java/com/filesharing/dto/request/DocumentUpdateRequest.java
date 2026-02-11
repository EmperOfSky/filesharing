package com.filesharing.dto.request;

import lombok.Data;
import javax.validation.constraints.Size;

@Data
public class DocumentUpdateRequest {
    @Size(max = 200, message = "文档标题长度不能超过200个字符")
    private String title;
    
    @Size(max = 1000, message = "文档描述长度不能超过1000个字符")
    private String description;
    
    private String content;
}