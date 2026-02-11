package com.filesharing.repository;

import com.filesharing.entity.SystemConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 系统配置Repository接口
 */
@Repository
public interface SystemConfigRepository extends JpaRepository<SystemConfig, Long> {
    
    /**
     * 根据配置键查找配置
     */
    Optional<SystemConfig> findByConfigKey(String configKey);
    
    /**
     * 检查配置键是否存在
     */
    boolean existsByConfigKey(String configKey);
    
    /**
     * 根据配置类型查找配置
     */
    @Query("SELECT sc FROM SystemConfig sc WHERE sc.configType = :configType")
    Iterable<SystemConfig> findByConfigType(@Param("configType") SystemConfig.ConfigType configType);
    
    /**
     * 查找启用的配置
     */
    @Query("SELECT sc FROM SystemConfig sc WHERE sc.isEnabled = true")
    Iterable<SystemConfig> findEnabledConfigs();
    
    /**
     * 根据配置键和默认值获取配置值
     */
    @Query("SELECT COALESCE(sc.configValue, :defaultValue) FROM SystemConfig sc WHERE sc.configKey = :configKey AND sc.isEnabled = true")
    String getConfigValueOrDefault(@Param("configKey") String configKey, @Param("defaultValue") String defaultValue);
    
    /**
     * 获取布尔类型配置值
     */
    @Query("SELECT CASE WHEN sc.configValue = 'true' THEN true ELSE false END FROM SystemConfig sc WHERE sc.configKey = :configKey AND sc.isEnabled = true")
    boolean getBooleanConfigValue(@Param("configKey") String configKey);
    
    /**
     * 获取整数类型配置值
     */
    @Query("SELECT CAST(sc.configValue AS integer) FROM SystemConfig sc WHERE sc.configKey = :configKey AND sc.isEnabled = true AND sc.configType = 'INTEGER'")
    Integer getIntegerConfigValue(@Param("configKey") String configKey);
}