package com.filesharing.dto;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 评论响应DTO
 */
@Data
public class CommentResponseDTO {
    
    private Long id;
    private String content;
    private UserInfoDTO author;
    private Long parentCommentId;
    private DocumentInfoDTO document;
    private Long replyCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    /**
     * 用户信息内部类
     */
    @Data
    public static class UserInfoDTO {
        private Long id;
        private String username;
        private String avatar;
    }
    
    /**
     * 文档信息内部类
     */
    @Data
    public static class DocumentInfoDTO {
        private Long id;
        private String documentName;
        private String documentType;
    }
}