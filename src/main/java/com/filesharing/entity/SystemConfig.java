package com.filesharing.entity;

import javax.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * 系统配置实体类
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "system_configs")
public class SystemConfig {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /**
     * 配置键
     */
    @Column(nullable = false, unique = true, length = 100)
    private String configKey;
    
    /**
     * 配置值
     */
    @Column(length = 1000)
    private String configValue;
    
    /**
     * 配置描述
     */
    @Column(length = 500)
    private String description;
    
    /**
     * 配置类型：STRING, INTEGER, BOOLEAN, JSON
     */
    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private ConfigType configType = ConfigType.STRING;
    
    /**
     * 是否启用
     */
    @Column(name = "is_enabled")
    private Boolean isEnabled = true;
    
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
     * 配置类型枚举
     */
    public enum ConfigType {
        STRING,     // 字符串
        INTEGER,    // 整数
        BOOLEAN,    // 布尔值
        JSON        // JSON格式
    }
}