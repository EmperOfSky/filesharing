package com.filesharing.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * 缩略图实体
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "thumbnails")
public class Thumbnail {
    
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
     * 缩略图类型：IMAGE, VIDEO_FRAME, DOCUMENT_PREVIEW
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "thumbnail_type", length = 30)
    private ThumbnailType thumbnailType;
    
    /**
     * 缩略图尺寸规格
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "size_spec", length = 20)
    private SizeSpec sizeSpec;
    
    /**
     * 缩略图文件路径
     */
    @Column(name = "thumbnail_path", length = 500)
    private String thumbnailPath;
    
    /**
     * 缩略图文件名
     */
    @Column(name = "thumbnail_name", length = 100)
    private String thumbnailName;
    
    /**
     * 缩略图大小（字节）
     */
    @Column(name = "file_size")
    private Long fileSize;
    
    /**
     * 图片宽度
     */
    private Integer width;
    
    /**
     * 图片高度
     */
    private Integer height;
    
    /**
     * MIME类型
     */
    @Column(name = "content_type", length = 100)
    private String contentType;
    
    /**
     * 生成状态：PENDING, PROCESSING, SUCCESS, FAILED
     */
    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private GenerationStatus status = GenerationStatus.PENDING;
    
    /**
     * 错误信息（如果生成失败）
     */
    @Column(name = "error_message", length = 1000)
    private String errorMessage;
    
    /**
     * 重试次数
     */
    @Column(name = "retry_count")
    private Integer retryCount = 0;
    
    /**
     * 最大重试次数
     */
    @Column(name = "max_retry_count")
    private Integer maxRetryCount = 3;
    
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
     * 最后生成时间
     */
    @Column(name = "generated_at")
    private LocalDateTime generatedAt;
    
    /**
     * 缩略图类型枚举
     */
    public enum ThumbnailType {
        IMAGE,              // 图片缩略图
        VIDEO_FRAME,        // 视频关键帧
        DOCUMENT_PREVIEW,   // 文档预览图
        PDF_PAGE            // PDF页面截图
    }
    
    /**
     * 尺寸规格枚举
     */
    public enum SizeSpec {
        SMALL("128x128"),    // 小尺寸
        MEDIUM("256x256"),   // 中等尺寸
        LARGE("512x512"),    // 大尺寸
        CUSTOM("自定义");     // 自定义尺寸
        
        private final String description;
        
        SizeSpec(String description) {
            this.description = description;
        }
        
        public String getDescription() {
            return description;
        }
    }
    
    /**
     * 生成状态枚举
     */
    public enum GenerationStatus {
        PENDING,        // 待处理
        PROCESSING,     // 处理中
        SUCCESS,        // 成功
        FAILED          // 失败
    }
}