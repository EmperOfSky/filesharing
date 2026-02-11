 package com.filesharing.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.Set;

/**
 * 标签实体
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "tags")
public class Tag {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /**
     * 标签名称
     */
    @Column(name = "tag_name", length = 50, nullable = false)
    private String tagName;
    
    /**
     * 标签颜色（十六进制颜色代码）
     */
    @Column(name = "color", length = 7)
    private String color = "#007bff";
    
    /**
     * 标签描述
     */
    @Column(name = "description", length = 200)
    private String description;
    
    /**
     * 是否为系统标签
     */
    @Column(name = "is_system_tag")
    private Boolean isSystemTag = false;
    
    /**
     * 创建用户
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;
    
    /**
     * 使用次数
     */
    @Column(name = "usage_count")
    private Integer usageCount = 0;
    
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
     * 关联的文件标签关系
     */
    @OneToMany(mappedBy = "tag", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Set<FileTag> fileTags;
    
    /**
     * 关联的文件夹标签关系
     */
    @OneToMany(mappedBy = "tag", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Set<FolderTag> folderTags;
}