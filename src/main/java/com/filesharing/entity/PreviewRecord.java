package com.filesharing.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * 文件预览记录实体
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "preview_records")
public class PreviewRecord {
    
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
     * 预览用户
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;
    
    /**
     * 预览类型：IMAGE, TEXT, PDF, VIDEO, AUDIO, OFFICE
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "preview_type", length = 20)
    private PreviewType previewType;
    
    /**
     * 预览设备类型
     */
    @Column(name = "device_type", length = 50)
    private String deviceType;
    
    /**
     * 用户代理信息
     */
    @Column(name = "user_agent", length = 500)
    private String userAgent;
    
    /**
     * IP地址
     */
    @Column(name = "ip_address", length = 45)
    private String ipAddress;
    
    /**
     * 预览时长（秒）
     */
    @Column(name = "duration_seconds")
    private Integer durationSeconds;
    
    /**
     * 是否成功预览
     */
    @Column(name = "is_success")
    private Boolean isSuccess = true;
    
    /**
     * 错误信息（如果预览失败）
     */
    @Column(name = "error_message", length = 1000)
    private String errorMessage;
    
    /**
     * 预览时间
     */
    @CreationTimestamp
    @Column(name = "preview_time")
    private LocalDateTime previewTime;
    
    /**
     * 预览类型枚举
     */
    public enum PreviewType {
        IMAGE,      // 图片
        TEXT,       // 文本文件
        PDF,        // PDF文档
        VIDEO,      // 视频文件
        AUDIO,      // 音频文件
        OFFICE,     // Office文档
        ARCHIVE,    // 压缩文件
        OTHER       // 其他类型
    }
}