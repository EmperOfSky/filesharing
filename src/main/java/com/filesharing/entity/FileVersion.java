package com.filesharing.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * 文件版本实体
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "file_versions")
public class FileVersion {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /**
     * 关联的文件
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "file_id", nullable = false)
    private FileEntity file;
    
    /**
     * 版本号
     */
    @Column(name = "version_number")
    private Integer versionNumber;
    
    /**
     * 版本描述/变更说明
     */
    @Column(name = "version_description", length = 500)
    private String versionDescription;
    
    /**
     * 存储文件名（UUID生成）
     */
    @Column(name = "storage_name", length = 36)
    private String storageName;
    
    /**
     * 文件路径
     */
    @Column(name = "file_path", length = 500)
    private String filePath;
    
    /**
     * 文件大小（字节）
     */
    @Column(name = "file_size")
    private Long fileSize;
    
    /**
     * 文件MD5值
     */
    @Column(name = "md5_hash", length = 32)
    private String md5Hash;
    
    /**
     * 文件类型（MIME类型）
     */
    @Column(name = "content_type", length = 100)
    private String contentType;
    
    /**
     * 修改用户
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "modified_by")
    private User modifiedBy;
    
    /**
     * 修改时间
     */
    @CreationTimestamp
    @Column(name = "modified_at")
    private LocalDateTime modifiedAt;
    
    /**
     * 是否为当前版本
     */
    @Column(name = "is_current")
    private Boolean isCurrent = false;
    
    /**
     * 是否已删除
     */
    @Column(name = "is_deleted")
    private Boolean isDeleted = false;
    
    /**
     * 删除时间
     */
    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;
    
    /**
     * 删除用户
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "deleted_by")
    private User deletedBy;
    
    /**
     * 版本标签（如：草稿、正式、归档等）
     */
    @Column(name = "version_tag", length = 50)
    private String versionTag;
    
    /**
     * 版本备注
     */
    @Column(name = "notes", length = 1000)
    private String notes;
}