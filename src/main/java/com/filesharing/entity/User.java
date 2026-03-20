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
    
    // 手动添加getter/setter方法
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    
    public String getNickname() { return nickname; }
    public void setNickname(String nickname) { this.nickname = nickname; }
    
    public String getAvatar() { return avatar; }
    public void setAvatar(String avatar) { this.avatar = avatar; }
    
    public Long getStorageQuota() { return storageQuota; }
    public void setStorageQuota(Long storageQuota) { this.storageQuota = storageQuota; }
    
    public Long getUsedStorage() { return usedStorage; }
    public void setUsedStorage(Long usedStorage) { this.usedStorage = usedStorage; }
    
    public UserStatus getStatus() { return status; }
    public void setStatus(UserStatus status) { this.status = status; }
    
    public UserRole getRole() { return role; }
    public void setRole(UserRole role) { this.role = role; }
    
    public LocalDateTime getLastLoginTime() { return lastLoginTime; }
    public void setLastLoginTime(LocalDateTime lastLoginTime) { this.lastLoginTime = lastLoginTime; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}