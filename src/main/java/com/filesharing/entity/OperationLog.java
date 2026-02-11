package com.filesharing.entity;

import javax.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * 操作日志实体类
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "operation_logs")
public class OperationLog {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /**
     * 操作类型：UPLOAD, DOWNLOAD, DELETE, SHARE, LOGIN, LOGOUT
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "operation_type", nullable = false, length = 30)
    private OperationType operationType;
    
    /**
     * 操作描述
     */
    @Column(length = 500)
    private String description;
    
    /**
     * 操作用户ID
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;
    
    /**
     * 相关文件ID
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "file_id")
    private FileEntity file;
    
    /**
     * 相关文件夹ID
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "folder_id")
    private Folder folder;
    
    /**
     * 相关分享记录ID
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "share_id")
    private ShareRecord shareRecord;
    
    /**
     * IP地址
     */
    @Column(name = "ip_address", length = 45)
    private String ipAddress;
    
    /**
     * 用户代理
     */
    @Column(name = "user_agent", length = 500)
    private String userAgent;
    
    /**
     * 操作结果：SUCCESS, FAILED
     */
    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private OperationResult result = OperationResult.SUCCESS;
    
    /**
     * 错误信息（操作失败时）
     */
    @Column(name = "error_message", length = 1000)
    private String errorMessage;
    
    /**
     * 操作时间
     */
    @CreationTimestamp
    @Column(name = "operation_time")
    private LocalDateTime operationTime;
    
    /**
     * 操作类型枚举
     */
    public enum OperationType {
        UPLOAD,         // 上传文件
        DOWNLOAD,       // 下载文件
        DELETE_FILE,    // 删除文件
        DELETE_FOLDER,  // 删除文件夹
        CREATE_FOLDER,  // 创建文件夹
        MOVE_FILE,      // 移动文件
        RENAME_FILE,    // 重命名文件
        SHARE_FILE,     // 分享文件
        ACCESS_SHARE,   // 访问分享
        LOGIN,          // 登录
        LOGOUT,         // 登出
        REGISTER        // 注册
    }
    
    /**
     * 操作结果枚举
     */
    public enum OperationResult {
        SUCCESS,        // 成功
        FAILED          // 失败
    }
}