package com.filesharing.service;

import com.filesharing.entity.CloudStorageConfig;
import com.filesharing.dto.response.StorageHealthReport;
import com.filesharing.dto.response.PerformanceMetrics;

import java.util.List;
import java.util.Map;

/**
 * 云存储监控服务接口
 */
public interface CloudStorageMonitoringService {
    
    /**
     * 监控所有云存储配置的健康状况
     */
    List<StorageHealthReport> monitorAllStorageHealth();
    
    /**
     * 监控特定云存储配置的健康状况
     */
    StorageHealthReport monitorStorageHealth(Long configId);
    
    /**
     * 收集性能指标
     */
    PerformanceMetrics collectPerformanceMetrics(Long configId);
    
    /**
     * 获取历史性能数据
     */
    Map<String, Object> getHistoricalPerformanceData(Long configId, int days);
    
    /**
     * 发送健康检查告警
     */
    void sendHealthAlert(StorageHealthReport healthReport);
    
    /**
     * 自动故障转移
     */
    CloudStorageConfig autoFailover(CloudStorageConfig failedConfig);
    
    /**
     * 清理监控数据
     */
    void cleanupMonitoringData(int daysToKeep);
}