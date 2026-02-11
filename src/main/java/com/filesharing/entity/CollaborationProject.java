package com.filesharing.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.ArrayList;

/**
 * 协作项目实体
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "collaboration_projects")
public class CollaborationProject {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /**
     * 项目名称
     */
    @Column(name = "project_name", length = 100, nullable = false)
    private String projectName;
    
    /**
     * 项目描述
     */
    @Column(name = "description", length = 500)
    private String description;
    
    /**
     * 项目所有者
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;
    
    /**
     * 项目状态：ACTIVE, ARCHIVED, CLOSED
     */
    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private ProjectStatus status = ProjectStatus.ACTIVE;
    
    /**
     * 是否公开项目
     */
    @Column(name = "is_public")
    private Boolean isPublic = false;
    
    /**
     * 项目封面图片URL
     */
    @Column(name = "cover_image", length = 500)
    private String coverImage;
    
    /**
     * 项目标签
     */
    @Column(name = "tags", length = 200)
    private String tags;
    
    /**
     * 成员数量
     */
    @Column(name = "member_count")
    private Integer memberCount = 1;
    
    /**
     * 文件数量
     */
    @Column(name = "file_count")
    private Integer fileCount = 0;
    
    /**
     * 最后活动时间
     */
    @Column(name = "last_activity")
    private LocalDateTime lastActivity;
    
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
     * 项目成员
     */
    @OneToMany(mappedBy = "project", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<ProjectMember> members = new ArrayList<>();
    
    /**
     * 项目状态枚举
     */
    public enum ProjectStatus {
        ACTIVE,     // 活跃
        ARCHIVED,   // 已归档
        CLOSED      // 已关闭
    }
}