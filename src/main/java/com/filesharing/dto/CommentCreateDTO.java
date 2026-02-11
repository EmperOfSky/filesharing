package com.filesharing.dto;

import lombok.Data;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

/**
 * 评论创建DTO
 */
@Data
public class CommentCreateDTO {
    
    @NotBlank(message = "评论内容不能为空")
    @Size(max = 1000, message = "评论内容长度不能超过1000个字符")
    private String content;
    
    private Long parentCommentId; // 回复评论的ID
    
    private Long documentId; // 关联的文档ID（可选）
}