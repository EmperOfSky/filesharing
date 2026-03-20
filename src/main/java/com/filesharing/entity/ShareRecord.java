package com.filesharing.entity;

import javax.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * 分享记录实体类
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "share_records")
public class ShareRecord {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /**
     * 分享链接标识符
     */
    @Column(name = "share_key", nullable = false, unique = true, length = 32)
    private String shareKey;
    
    /**
     * 分享标题
     */
    @Column(length = 200)
    private String title;
    
    /**
     * 分享描述
     */
    @Column(length = 500)
    private String description;
    
    /**
     * 分享类型：FILE, FOLDER
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "share_type", length = 20)
    private ShareType shareType;
    
    /**
     * 关联的文件ID
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "file_id")
    private FileEntity file;
    
    /**
     * 关联的文件夹ID
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "folder_id")
    private Folder folder;
    
    /**
     * 分享者ID
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sharer_id", nullable = false)
    private User sharer;
    
    /**
     * 访问密码（可选）
     */
    @Column(length = 100)
    private String password;
    
    /**
     * 过期时间
     */
    @Column(name = "expire_time")
    private LocalDateTime expireTime;
    
    /**
     * 最大访问次数（0表示无限制）
     */
    @Column(name = "max_access_count")
    private Integer maxAccessCount = 0;
    
    /**
     * 当前访问次数
     */
    @Column(name = "current_access_count")
    private Integer currentAccessCount = 0;

    /**
     * 短链接总点击量（PV）
     */
    @Column(name = "pv_count")
    private Integer pvCount = 0;

    /**
     * 短链接独立访客数（UV）
     */
    @Column(name = "uv_count")
    private Integer uvCount = 0;

    /**
     * 最近访客IP
     */
    @Column(name = "last_visitor_ip", length = 64)
    private String lastVisitorIp;

    /**
     * 最近访客地址
     */
    @Column(name = "last_visitor_address", length = 255)
    private String lastVisitorAddress;

    /**
     * 最近点击时间
     */
    @Column(name = "last_access_at")
    private LocalDateTime lastAccessAt;
    
    /**
     * 分享状态：ACTIVE, EXPIRED, DISABLED
     */
    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private ShareStatus status = ShareStatus.ACTIVE;
    
    /**
     * 是否允许下载
     */
    @Column(name = "allow_download")
    private Boolean allowDownload = true;
    
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
     * 分享类型枚举
     */
    public enum ShareType {
        FILE,       // 文件分享
        FOLDER      // 文件夹分享
    }
    
    /**
     * 分享状态枚举
     */
    public enum ShareStatus {
        ACTIVE,     // 激活
        EXPIRED,    // 已过期
        DISABLED    // 已禁用
    }
}