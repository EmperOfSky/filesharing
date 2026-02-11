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
 * 文件实体类
 * @author Admin
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "files")
public class FileEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /**
     * 原始文件名
     */
    @Column(name = "original_name", nullable = false, length = 255)
    private String originalName;
    
    /**
     * 存储文件名（UUID生成）
     */
    @Column(name = "storage_name", nullable = false, unique = true, length = 36)
    private String storageName;
    
    /**
     * 文件路径
     */
    @Column(name = "file_path", nullable = false, length = 500)
    private String filePath;
    
    /**
     * 文件大小（字节）
     */
    @Column(name = "file_size")
    private Long fileSize;
    
    /**
     * 文件类型（MIME类型）
     */
    @Column(name = "content_type", length = 100)
    private String contentType;
    
    /**
     * 文件扩展名
     */
    @Column(length = 20)
    private String extension;
    
    /**
     * 文件MD5值（用于去重）
     */
    @Column(name = "md5_hash", length = 32)
    private String md5Hash;
    
    /**
     * 文件状态：UPLOADING, AVAILABLE, DELETED
     */
    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private FileStatus status = FileStatus.AVAILABLE;
    
    /**
     * 是否公开访问
     */
    @Column(name = "is_public")
    private Boolean isPublic = false;
    
    /**
     * 下载次数
     */
    @Column(name = "download_count")
    private Integer downloadCount = 0;
    
    /**
     * 上传者ID
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "uploader_id", nullable = false)
    private User uploader;
    
    /**
     * 所属文件夹ID
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "folder_id")
    private Folder folder;
    
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
     * 删除时间
     */
    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;
    
    /**
     * 文件标签关联
     */
    @OneToMany(mappedBy = "file", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<FileTag> fileTags;
    
    
    /**
     * 文件状态枚举
     */
    public enum FileStatus {
        UPLOADING,  // 上传中
        AVAILABLE,  // 可用
        DELETED     // 已删除
    }
}