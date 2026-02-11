package com.filesharing.repository;

import com.filesharing.entity.CloudFileMapping;
import com.filesharing.entity.CloudStorageConfig;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 云存储文件映射Repository接口
 */
@Repository
public interface CloudFileMappingRepository extends JpaRepository<CloudFileMapping, Long> {
    
    /**
     * 根据本地文件ID查找映射
     */
    Optional<CloudFileMapping> findByLocalFileId(Long localFileId);
    
    /**
     * 根据云端Key查找映射
     */
    @Query("SELECT cfm FROM CloudFileMapping cfm WHERE cfm.cloudKey = :cloudKey " +
           "AND cfm.storageConfig = :storageConfig")
    Optional<CloudFileMapping> findByCloudKeyAndStorageConfig(@Param("cloudKey") String cloudKey,
                                                             @Param("storageConfig") CloudStorageConfig storageConfig);
    
    /**
     * 根据存储配置查找映射
     */
    Page<CloudFileMapping> findByStorageConfig(CloudStorageConfig storageConfig, Pageable pageable);
    
    /**
     * 查找未上传的文件映射
     */
    @Query("SELECT cfm FROM CloudFileMapping cfm WHERE cfm.isUploaded = false " +
           "AND cfm.uploadAttempts < cfm.maxRetryAttempts ORDER BY cfm.createdAt ASC")
    List<CloudFileMapping> findUnuploadedMappings();
    
    /**
     * 查找上传失败的映射
     */
    @Query("SELECT cfm FROM CloudFileMapping cfm WHERE cfm.isUploaded = false " +
           "AND cfm.uploadAttempts >= cfm.maxRetryAttempts")
    Page<CloudFileMapping> findFailedUploads(Pageable pageable);
    
    /**
     * 根据生命周期状态查找映射
     */
    @Query("SELECT cfm FROM CloudFileMapping cfm WHERE cfm.lifecycleStatus = :status")
    Page<CloudFileMapping> findByLifecycleStatus(@Param("status") CloudFileMapping.LifecycleStatus status, 
                                                Pageable pageable);
    
    /**
     * 统计存储配置的文件数量
     */
    @Query("SELECT COUNT(cfm) FROM CloudFileMapping cfm WHERE cfm.storageConfig = :storageConfig")
    Long countByStorageConfig(@Param("storageConfig") CloudStorageConfig storageConfig);
    
    /**
     * 统计各存储配置的文件数量
     */
    @Query("SELECT cfm.storageConfig.configName, COUNT(cfm) FROM CloudFileMapping cfm " +
           "GROUP BY cfm.storageConfig.configName")
    List<Object[]> countFilesByStorageConfig();
    
    /**
     * 获取文件上传统计
     */
    @Query("SELECT " +
           "COUNT(cfm) as totalFiles, " +
           "SUM(CASE WHEN cfm.isUploaded = true THEN 1 ELSE 0 END) as uploaded, " +
           "SUM(CASE WHEN cfm.isUploaded = false THEN 1 ELSE 0 END) as pending " +
           "FROM CloudFileMapping cfm")
    Object[] getUploadStatistics();
    
    /**
     * 查找需要迁移的文件（存储空间不足）
     */
    @Query("SELECT cfm FROM CloudFileMapping cfm WHERE cfm.storageConfig.usedStorage >= " +
           "cfm.storageConfig.storageLimit * 0.9")
    List<CloudFileMapping> findFilesForMigration();
    
    /**
     * 查找指定时间范围内上传的文件
     */
    @Query("SELECT cfm FROM CloudFileMapping cfm WHERE cfm.uploadedAt BETWEEN :startTime AND :endTime")
    Page<CloudFileMapping> findByUploadedAtBetween(@Param("startTime") LocalDateTime startTime,
                                                  @Param("endTime") LocalDateTime endTime,
                                                  Pageable pageable);
}