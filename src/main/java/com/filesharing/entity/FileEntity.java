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
     * 预览次数
     */
    @Column(name = "preview_count")
    private Integer previewCount = 0;
    
    /**
     * 分享次数
     */
    @Column(name = "share_count")
    private Integer shareCount = 0;
    
    /**
     * 最后下载时间
     */
    @Column(name = "last_download_at")
    private LocalDateTime lastDownloadAt;
    
    /**
     * 最后预览时间
     */
    @Column(name = "last_preview_at")
    private LocalDateTime lastPreviewAt;
    
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
    
    // 手动添加getter/setter方法
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getOriginalName() { return originalName; }
    public void setOriginalName(String originalName) { this.originalName = originalName; }
    
    public String getStorageName() { return storageName; }
    public void setStorageName(String storageName) { this.storageName = storageName; }
    
    public String getFilePath() { return filePath; }
    public void setFilePath(String filePath) { this.filePath = filePath; }
    
    public Long getFileSize() { return fileSize; }
    public void setFileSize(Long fileSize) { this.fileSize = fileSize; }
    
    public String getContentType() { return contentType; }
    public void setContentType(String contentType) { this.contentType = contentType; }
    
    public String getExtension() { return extension; }
    public void setExtension(String extension) { this.extension = extension; }
    
    public String getMd5Hash() { return md5Hash; }
    public void setMd5Hash(String md5Hash) { this.md5Hash = md5Hash; }
    
    public FileStatus getStatus() { return status; }
    public void setStatus(FileStatus status) { this.status = status; }
    
    public Boolean getIsPublic() { return isPublic; }
    public void setIsPublic(Boolean isPublic) { this.isPublic = isPublic; }
    
    public Integer getDownloadCount() { return downloadCount; }
    public void setDownloadCount(Integer downloadCount) { this.downloadCount = downloadCount; }
    
    public Integer getPreviewCount() { return previewCount; }
    public void setPreviewCount(Integer previewCount) { this.previewCount = previewCount; }
    
    public Integer getShareCount() { return shareCount; }
    public void setShareCount(Integer shareCount) { this.shareCount = shareCount; }
    
    public LocalDateTime getLastDownloadAt() { return lastDownloadAt; }
    public void setLastDownloadAt(LocalDateTime lastDownloadAt) { this.lastDownloadAt = lastDownloadAt; }
    
    public LocalDateTime getLastPreviewAt() { return lastPreviewAt; }
    public void setLastPreviewAt(LocalDateTime lastPreviewAt) { this.lastPreviewAt = lastPreviewAt; }
    
    public User getUploader() { return uploader; }
    public void setUploader(User uploader) { this.uploader = uploader; }
    
    public Folder getFolder() { return folder; }
    public void setFolder(Folder folder) { this.folder = folder; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    
    public LocalDateTime getDeletedAt() { return deletedAt; }
    public void setDeletedAt(LocalDateTime deletedAt) { this.deletedAt = deletedAt; }
    
    public List<FileTag> getFileTags() { return fileTags; }
    public void setFileTags(List<FileTag> fileTags) { this.fileTags = fileTags; }
}