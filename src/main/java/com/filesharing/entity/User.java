package com.filesharing.entity;

import javax.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * 用户实体类
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "users")
public class User {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /**
     * 用户名
     */
    @Column(nullable = false, unique = true, length = 50)
    private String username;
    
    /**
     * 邮箱
     */
    @Column(nullable = false, unique = true, length = 100)
    private String email;
    
    /**
     * 密码（加密存储）
     */
    @Column(nullable = false)
    private String password;
    
    /**
     * 昵称
     */
    @Column(length = 50)
    private String nickname;
    
    /**
     * 头像URL
     */
    @Column(length = 255)
    private String avatar;
    
    /**
     * 存储空间配额（字节）
     */
    @Column(name = "storage_quota")
    private Long storageQuota = 1073741824L; // 默认1GB
    
    /**
     * 已使用存储空间（字节）
     */
    @Column(name = "used_storage")
    private Long usedStorage = 0L;
    
    /**
     * 账户状态：ACTIVE, DISABLED, SUSPENDED
     */
    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private UserStatus status = UserStatus.ACTIVE;
    
    /**
     * 用户角色：USER, ADMIN
     */
    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private UserRole role = UserRole.USER;
    
    /**
     * 最后登录时间
     */
    @Column(name = "last_login_time")
    private LocalDateTime lastLoginTime;
    
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
     * 用户状态枚举
     */
    public enum UserStatus {
        ACTIVE,     // 激活
        DISABLED,   // 禁用
        SUSPENDED   // 暂停
    }
    
    /**
     * 用户角色枚举
     */
    public enum UserRole {
        USER,       // 普通用户
        ADMIN       // 管理员
    }
}