package com.filesharing.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.List;
import java.util.ArrayList;

/**
 * 协作文档实体
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "collaborative_documents")
public class CollaborativeDocument {
    
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
     * 文档名称
     */
    @Column(name = "document_name", length = 200, nullable = false)
    private String documentName;
    
    /**
     * 文档内容
     */
    @Column(name = "content", columnDefinition = "TEXT")
    private String content;
    
    /**
     * 文档类型：TEXT, MARKDOWN, WIKI
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "document_type", length = 20)
    private DocumentType documentType = DocumentType.TEXT;
    
    /**
     * 创建者
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;
    
    /**
     * 最后编辑者
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "last_edited_by")
    private User lastEditedBy;
    
    /**
     * 最后编辑时间
     */
    @Column(name = "last_edited_at")
    private LocalDateTime lastEditedAt;
    
    /**
     * 版本号
     */
    @Column(name = "version")
    private Integer version = 1;
    
    /**
     * 是否锁定编辑
     */
    @Column(name = "is_locked")
    private Boolean isLocked = false;
    
    /**
     * 锁定用户
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "locked_by")
    private User lockedBy;
    
    /**
     * 锁定时间
     */
    @Column(name = "locked_at")
    private LocalDateTime lockedAt;
    
    /**
     * 查看次数
     */
    @Column(name = "view_count")
    private Integer viewCount = 0;
    
    /**
     * 评论数
     */
    @Column(name = "comment_count")
    private Integer commentCount = 0;
    
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
     * 正在编辑此文档的用户
     */
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "document_editors",
        joinColumns = @JoinColumn(name = "document_id"),
        inverseJoinColumns = @JoinColumn(name = "user_id")
    )
    private Set<User> editingUsers;
    
    /**
     * 文档状态：DRAFT, PUBLISHED, ARCHIVED
     */
    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private Status status = Status.DRAFT;
    
    /**
     * 文档标签
     */
    @ElementCollection
    @CollectionTable(name = "document_tags", joinColumns = @JoinColumn(name = "document_id"))
    @Column(name = "tag")
    private List<String> tags = new ArrayList<>();
    
    /**
     * 文档类型枚举
     */
    public enum DocumentType {
        TEXT,       // 纯文本
        MARKDOWN,   // Markdown格式
        WIKI        // Wiki格式
    }
    
    /**
     * 文档状态枚举
     */
    public enum Status {
        DRAFT,      // 草稿
        PUBLISHED,  // 已发布
        ARCHIVED    // 已归档
    }
}