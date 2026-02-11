package com.filesharing.dto;

import com.filesharing.entity.CollaborativeDocument;
import lombok.Data;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.util.ArrayList;
import java.util.List;

/**
 * 协作文档创建DTO
 */
@Data
public class DocumentCreateDTO {
    
    @NotBlank(message = "文档名称不能为空")
    @Size(max = 200, message = "文档名称长度不能超过200个字符")
    private String documentName;
    
    @NotNull(message = "文档类型不能为空")
    private CollaborativeDocument.DocumentType documentType;
    
    @Size(max = 2000, message = "文档内容长度不能超过2000个字符")
    private String content;
    
    private Long projectId; // 关联的项目ID
    
    @Size(max = 10, message = "标签数量不能超过10个")
    private List<String> tags = new ArrayList<>();
    
    // 默认构造函数
    public DocumentCreateDTO() {
        this.tags = new ArrayList<>();
    }
}