package com.filesharing.repository;

import com.filesharing.entity.CloudStorageConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 云存储配置Repository接口
 */
@Repository
public interface CloudStorageConfigRepository extends JpaRepository<CloudStorageConfig, Long> {
    
    /**
     * 根据配置名称查找配置
     */
    Optional<CloudStorageConfig> findByConfigName(String configName);
    
    /**
     * 查找启用的配置
     */
    @Query("SELECT csc FROM CloudStorageConfig csc WHERE csc.isEnabled = true")
    List<CloudStorageConfig> findEnabledConfigs();
    
    /**
     * 查找启用的配置（方法命名方式）
     */
    List<CloudStorageConfig> findByIsEnabledTrue();
    
    /**
     * 查找第一个启用的默认配置
     */
    Optional<CloudStorageConfig> findFirstByIsDefaultTrueAndIsEnabledTrue();
    
    /**
     * 查找默认存储配置
     */
    @Query("SELECT csc FROM CloudStorageConfig csc WHERE csc.isDefault = true AND csc.isEnabled = true")
    Optional<CloudStorageConfig> findDefaultConfig();
    
    /**
     * 根据提供商类型查找配置
     */
    @Query("SELECT csc FROM CloudStorageConfig csc WHERE csc.providerType = :providerType")
    List<CloudStorageConfig> findByProviderType(@Param("providerType") CloudStorageConfig.ProviderType providerType);
    
    /**
     * 查找启用且有存储空间的配置
     */
    @Query("SELECT csc FROM CloudStorageConfig csc WHERE csc.isEnabled = true " +
           "AND (csc.storageLimit IS NULL OR csc.usedStorage < csc.storageLimit)")
    List<CloudStorageConfig> findAvailableConfigs();
    
    /**
     * 检查配置名称是否存在
     */
    boolean existsByConfigName(String configName);
    
    /**
     * 统计各类云服务商的配置数量
     */
    @Query("SELECT csc.providerType, COUNT(csc) FROM CloudStorageConfig csc GROUP BY csc.providerType")
    List<Object[]> countConfigsByProvider();
    
    /**
     * 获取存储使用统计
     */
    @Query("SELECT csc.providerType, " +
           "SUM(csc.usedStorage) as totalUsed, " +
           "SUM(csc.storageLimit) as totalLimit, " +
           "COUNT(csc) as configCount " +
           "FROM CloudStorageConfig csc WHERE csc.isEnabled = true GROUP BY csc.providerType")
    List<Object[]> getStorageUsageStats();
    
    /**
     * 查找连接测试失败的配置
     */
    @Query("SELECT csc FROM CloudStorageConfig csc WHERE csc.connectionStatus = 'FAILED'")
    List<CloudStorageConfig> findFailedConnectionConfigs();
    
    /**
     * 重置所有配置的默认状态
     */
    @Query("UPDATE CloudStorageConfig csc SET csc.isDefault = false")
    void resetDefaultConfig();
}