package com.filesharing.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * 分片上传记录实体
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "chunk_upload_records")
public class ChunkUploadRecord {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /**
     * 上传ID
     */
    @Column(name = "upload_id", length = 50, unique = true, nullable = false)
    private String uploadId;
    
    /**
     * 原始文件名
     */
    @Column(name = "original_name", length = 255)
    private String originalName;
    
    /**
     * 文件总大小
     */
    @Column(name = "file_size")
    private Long fileSize;
    
    /**
     * 分片大小
     */
    @Column(name = "chunk_size")
    private Long chunkSize;
    
    /**
     * 总分片数
     */
    @Column(name = "total_chunks")
    private Integer totalChunks;
    
    /**
     * 已上传分片数
     */
    @Column(name = "uploaded_chunks")
    private Integer uploadedChunks = 0;
    
    /**
     * 上传状态：INITIALIZED, UPLOADING, COMPLETED, FAILED, CANCELLED
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20)
    private UploadStatus status = UploadStatus.INITIALIZED;
    
    /**
     * 上传者
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "uploader_id")
    private User uploader;
    
    /**
     * 目标文件夹
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "folder_id")
    private Folder folder;
    
    /**
     * MD5哈希值（用于秒传）
     */
    @Column(name = "md5_hash", length = 32)
    private String md5Hash;
    
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
     * 过期时间
     */
    @Column(name = "expire_time")
    private LocalDateTime expireTime;
    
    /**
     * 上传状态枚举
     */
    public enum UploadStatus {
        INITIALIZED,    // 已初始化
        UPLOADING,      // 上传中
        COMPLETED,      // 已完成
        FAILED,         // 失败
        CANCELLED       // 已取消
    }
}