package com.filesharing.entity;

import javax.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 文件夹实体类
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "folders")
public class Folder {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /**
     * 文件夹名称
     */
    @Column(name = "folder_name", length = 255)
    private String name;

    /**
     * 兼容历史数据库中的 name 列
     */
    @Column(name = "name", length = 255)
    private String legacyName;
    
    /**
     * 文件夹描述
     */
    @Column(length = 500)
    private String description;
    
    /**
     * 父文件夹ID
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private Folder parent;
    
    /**
     * 所有子文件夹
     */
    @OneToMany(mappedBy = "parent", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Folder> children;
    
    /**
     * 文件夹中的文件
     */
    @OneToMany(mappedBy = "folder", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<FileEntity> files;
    
    /**
     * 所有者ID
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;
    
    /**
     * 是否公开
     */
    @Column(name = "is_public")
    private Boolean isPublic = false;
    
    /**
     * 文件夹路径（用于快速查询）
     */
    @Column(name = "folder_path", length = 1000)
    private String folderPath;
    
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

    @PostLoad
    @PrePersist
    @PreUpdate
    private void syncNameColumns() {
        if ((name == null || name.isBlank()) && legacyName != null && !legacyName.isBlank()) {
            name = legacyName;
        }
        if ((legacyName == null || legacyName.isBlank()) && name != null && !name.isBlank()) {
            legacyName = name;
        }
    }
}