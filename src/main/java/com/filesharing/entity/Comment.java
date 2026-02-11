package com.filesharing.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * 评论实体
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "comments")
public class Comment {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /**
     * 关联的项目
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private CollaborationProject project;
    
    /**
     * 关联的文件（可选）
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "file_id")
    private FileEntity file;
    
    /**
     * 评论作者
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id", nullable = false)
    private User author;
    
    /**
     * 评论内容
     */
    @Column(name = "content", columnDefinition = "TEXT")
    private String content;
    
    /**
     * 父评论ID（用于回复）
     */
    @Column(name = "parent_id")
    private Long parentId;
    
    /**
     * 评论类型：FILE_COMMENT, PROJECT_COMMENT, DISCUSSION
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "comment_type", length = 30)
    private CommentType commentType;
    
    /**
     * 是否已解决
     */
    @Column(name = "is_resolved")
    private Boolean isResolved = false;
    
    /**
     * 解决时间
     */
    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;
    
    /**
     * 解决人
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resolved_by")
    private User resolvedBy;
    
    /**
     * 点赞数
     */
    @Column(name = "like_count")
    private Integer likeCount = 0;
    
    /**
     * 踩数
     */
    @Column(name = "dislike_count")
    private Integer dislikeCount = 0;
    
    /**
     * 创建时间
     */
    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    /**
     * 更新时间
     */
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    /**
     * 评论类型枚举
     */
    public enum CommentType {
        FILE_COMMENT,       // 文件评论
        PROJECT_COMMENT,    // 项目评论
        DISCUSSION,         // 讨论
        QUESTION,           // 问题
        SUGGESTION          // 建议
    }
}