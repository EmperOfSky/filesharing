package com.filesharing.service.impl;

import com.filesharing.entity.CloudStorageConfig;
import com.filesharing.exception.BusinessException;
import com.filesharing.repository.CloudStorageConfigRepository;
import com.filesharing.service.CloudStorageMonitoringService;
import com.filesharing.service.CloudStorageService;
import com.filesharing.dto.response.StorageHealthReport;
import com.filesharing.dto.response.PerformanceMetrics;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 云存储监控服务实现类
 * @author Admin
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CloudStorageMonitoringServiceImpl implements CloudStorageMonitoringService {
    
    private final CloudStorageConfigRepository cloudStorageConfigRepository;
    private final CloudStorageService cloudStorageService;
    
    // 存储性能指标的缓存
    private final Map<Long, List<PerformanceMetrics>> performanceCache = new ConcurrentHashMap<>();
    private static final int CACHE_SIZE = 100; // 每个配置最多缓存100个指标点
    
    @Override
    public List<StorageHealthReport> monitorAllStorageHealth() {
        List<CloudStorageConfig> configs = cloudStorageConfigRepository.findByIsEnabledTrue();
        List<StorageHealthReport> healthReports = new ArrayList<>();
        
        for (CloudStorageConfig config : configs) {
            try {
                StorageHealthReport report = monitorStorageHealth(config.getId());
                healthReports.add(report);
            } catch (Exception e) {
                log.error("监控云存储配置健康状况失败: 配置ID={}", config.getId(), e);
                StorageHealthReport errorReport = createErrorHealthReport(config, e);
                healthReports.add(errorReport);
            }
        }
        
        return healthReports;
    }
    
    @Override
    public StorageHealthReport monitorStorageHealth(Long configId) {
        CloudStorageConfig config = cloudStorageConfigRepository.findById(configId)
            .orElseThrow(() -> new BusinessException("云存储配置不存在"));
        
        StorageHealthReport report = new StorageHealthReport();
        report.setConfigId(configId);
        report.setConfigName(config.getConfigName());
        report.setProviderType(config.getProviderType().name());
        report.setRegion(config.getRegion());
        report.setCheckTime(LocalDateTime.now());
        
        try {
            // 测试连接
            long startTime = System.currentTimeMillis();
            boolean isConnected = testConnectionWithTimeout(config, 5000); // 5秒超时
            long responseTime = System.currentTimeMillis() - startTime;
            
            report.setConnectionStatus(isConnected ? "HEALTHY" : "UNHEALTHY");
            report.setResponseTime(responseTime);
            report.setLastCheckSuccess(isConnected);
            
            if (isConnected) {
                // 获取存储使用情况
                try {
                    CloudStorageService.StorageUsage usage = cloudStorageService.getStorageUsage(configId);
                    report.setTotalSpace(usage.getTotalLimit());
                    report.setUsedSpace(usage.getUsedStorage());
                    report.setAvailableSpace(usage.getAvailableStorage());
                    report.setUsagePercentage(usage.getUsagePercentage());
                } catch (Exception e) {
                    log.warn("获取存储使用情况失败: 配置ID={}", configId, e);
                    report.setErrorMessage("获取存储使用情况失败: " + e.getMessage());
                }
                
                // 评估整体健康状况
                report.setOverallHealth(calculateOverallHealth(report));
            } else {
                report.setOverallHealth("CRITICAL");
                report.setErrorMessage("连接测试失败");
            }
            
        } catch (Exception e) {
            log.error("健康检查失败: 配置ID={}", configId, e);
            report.setConnectionStatus("ERROR");
            report.setOverallHealth("CRITICAL");
            report.setErrorMessage(e.getMessage());
            report.setLastCheckSuccess(false);
        }
        
        // 更新配置的连接状态
        updateConfigConnectionStatus(config, report.getConnectionStatus());
        
        log.debug("健康检查完成: 配置ID={}, 状态={}, 响应时间={}ms", 
            configId, report.getOverallHealth(), report.getResponseTime());
        
        return report;
    }
    
    @Override
    public PerformanceMetrics collectPerformanceMetrics(Long configId) {
        CloudStorageConfig config = cloudStorageConfigRepository.findById(configId)
            .orElseThrow(() -> new BusinessException("云存储配置不存在"));
        
        PerformanceMetrics metrics = new PerformanceMetrics();
        metrics.setConfigId(configId);
        metrics.setTimestamp(LocalDateTime.now());
        
        try {
            // 收集各种性能指标
            metrics.setConnectionLatency(measureConnectionLatency(config));
            metrics.setUploadSpeed(measureUploadSpeed(config));
            metrics.setDownloadSpeed(measureDownloadSpeed(config));
            metrics.setAvailability(calculateAvailability(configId));
            
            // 存储到缓存
            cachePerformanceMetrics(configId, metrics);
            
            log.debug("性能指标收集完成: 配置ID={}, 延迟={}ms", configId, metrics.getConnectionLatency());
            
        } catch (Exception e) {
            log.error("性能指标收集失败: 配置ID={}", configId, e);
            metrics.setError("指标收集失败: " + e.getMessage());
        }
        
        return metrics;
    }
    
    @Override
    public Map<String, Object> getHistoricalPerformanceData(Long configId, int days) {
        Map<String, Object> result = new HashMap<>();
        
        List<PerformanceMetrics> cachedMetrics = performanceCache.getOrDefault(configId, new ArrayList<>());
        LocalDateTime cutoffTime = LocalDateTime.now().minusDays(days);
        
        List<PerformanceMetrics> filteredMetrics = cachedMetrics.stream()
            .filter(metric -> metric.getTimestamp().isAfter(cutoffTime))
            .sorted(Comparator.comparing(PerformanceMetrics::getTimestamp))
            .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
        
        result.put("metrics", filteredMetrics);
        result.put("count", filteredMetrics.size());
        result.put("period", days + "天");
        
        if (!filteredMetrics.isEmpty()) {
            // 计算统计数据
            result.put("avgLatency", filteredMetrics.stream()
                .mapToLong(PerformanceMetrics::getConnectionLatency)
                .average()
                .orElse(0.0));
            
            result.put("avgAvailability", filteredMetrics.stream()
                .mapToDouble(PerformanceMetrics::getAvailability)
                .average()
                .orElse(0.0));
        }
        
        return result;
    }
    
    @Override
    public void sendHealthAlert(StorageHealthReport healthReport) {
        if ("CRITICAL".equals(healthReport.getOverallHealth()) || 
            "WARNING".equals(healthReport.getOverallHealth())) {
            
            String alertMessage = String.format(
                "云存储健康告警 - 配置: %s (%s), 状态: %s, 响应时间: %dms", 
                healthReport.getConfigName(), 
                healthReport.getProviderType(),
                healthReport.getOverallHealth(),
                healthReport.getResponseTime()
            );
            
            log.warn(alertMessage);
            
            // 实际应用中这里应该发送邮件、短信或其他通知
            // notificationService.sendAlert(alertMessage);
        }
    }
    
    @Override
    public CloudStorageConfig autoFailover(CloudStorageConfig failedConfig) {
        try {
            List<CloudStorageConfig> enabledConfigs = cloudStorageConfigRepository.findByIsEnabledTrue();
            
            // 寻找同区域的备用配置
            Optional<CloudStorageConfig> backupConfig = enabledConfigs.stream()
                .filter(config -> !config.getId().equals(failedConfig.getId()) &&
                               config.getRegion().equals(failedConfig.getRegion()) &&
                               config.getProviderType().equals(failedConfig.getProviderType()) &&
                               config.getIsDefault() != null && config.getIsDefault())
                .findFirst();
            
            if (backupConfig.isPresent()) {
                log.info("自动故障转移成功: 从配置 {} 切换到配置 {}", 
                    failedConfig.getId(), backupConfig.get().getId());
                return backupConfig.get();
            }
            
            // 如果没有同区域的，默认切换到第一个启用的配置
            Optional<CloudStorageConfig> fallbackConfig = enabledConfigs.stream()
                .filter(config -> !config.getId().equals(failedConfig.getId()))
                .findFirst();
                
            if (fallbackConfig.isPresent()) {
                log.info("故障转移至备选配置: 从配置 {} 切换到配置 {}", 
                    failedConfig.getId(), fallbackConfig.get().getId());
                return fallbackConfig.get();
            }
            
            log.error("无可用的故障转移配置: 原配置ID={}", failedConfig.getId());
            throw new BusinessException("无可用的故障转移配置");
            
        } catch (Exception e) {
            log.error("自动故障转移失败: 原配置ID={}", failedConfig.getId(), e);
            throw new BusinessException("故障转移失败: " + e.getMessage());
        }
    }
    
    @Override
    public void cleanupMonitoringData(int daysToKeep) {
        LocalDateTime cutoffTime = LocalDateTime.now().minusDays(daysToKeep);
        
        int cleanedCount = 0;
        for (List<PerformanceMetrics> metricsList : performanceCache.values()) {
            metricsList.removeIf(metrics -> metrics.getTimestamp().isBefore(cutoffTime));
            cleanedCount++;
        }
        
        log.info("清理监控数据完成: 保留{}天数据, 清理了{}个配置的旧数据", daysToKeep, cleanedCount);
    }
    
    // ==================== 定时任务 ====================
    
    /**
     * 定时健康检查（每5分钟执行一次）
     */
    @Scheduled(fixedRate = 300000) // 5分钟
    public void scheduledHealthCheck() {
        try {
            List<StorageHealthReport> reports = monitorAllStorageHealth();
            
            // 发送告警
            reports.stream()
                .filter(report -> !"HEALTHY".equals(report.getOverallHealth()))
                .forEach(this::sendHealthAlert);
                
            log.info("定时健康检查完成: 检查了{}个配置", reports.size());
            
        } catch (Exception e) {
            log.error("定时健康检查失败", e);
        }
    }
    
    /**
     * 定时性能指标收集（每小时执行一次）
     */
    @Scheduled(fixedRate = 3600000) // 1小时
    public void scheduledPerformanceCollection() {
        try {
            List<CloudStorageConfig> configs = cloudStorageConfigRepository.findByIsEnabledTrue();
            
            for (CloudStorageConfig config : configs) {
                try {
                    collectPerformanceMetrics(config.getId());
                } catch (Exception e) {
                    log.warn("收集性能指标失败: 配置ID={}", config.getId(), e);
                }
            }
            
            log.info("定时性能指标收集完成: 收集了{}个配置的数据", configs.size());
            
        } catch (Exception e) {
            log.error("定时性能指标收集失败", e);
        }
    }
    
    // ==================== 私有辅助方法 ====================
    
    private boolean testConnectionWithTimeout(CloudStorageConfig config, int timeoutMs) {
        try {
            // 实际应用中应该使用Future和超时控制
            return cloudStorageService.testConnection(config.getId()).getSuccess();
        } catch (Exception e) {
            log.debug("连接测试超时或失败: 配置ID={}, 超时={}ms", config.getId(), timeoutMs);
            return false;
        }
    }
    
    private long measureConnectionLatency(CloudStorageConfig config) {
        try {
            long startTime = System.currentTimeMillis();
            cloudStorageService.testConnection(config.getId());
            return System.currentTimeMillis() - startTime;
        } catch (Exception e) {
            return -1L; // 表示测量失败
        }
    }
    
    private double measureUploadSpeed(CloudStorageConfig config) {
        // 简化实现：返回模拟的上传速度
        return 10.0 + new Random().nextDouble() * 90.0; // 10-100 MB/s
    }
    
    private double measureDownloadSpeed(CloudStorageConfig config) {
        // 简化实现：返回模拟的下载速度
        return 15.0 + new Random().nextDouble() * 85.0; // 15-100 MB/s
    }
    
    private double calculateAvailability(Long configId) {
        // 简化实现：基于最近的健康检查结果计算可用性
        List<PerformanceMetrics> metrics = performanceCache.getOrDefault(configId, new ArrayList<>());
        if (metrics.isEmpty()) {
            return 100.0; // 默认100%可用性
        }
        
        long successfulChecks = metrics.stream()
            .filter(m -> m.getConnectionLatency() > 0)
            .count();
            
        return (double) successfulChecks / metrics.size() * 100;
    }
    
    private String calculateOverallHealth(StorageHealthReport report) {
        if (!report.isLastCheckSuccess()) {
            return "CRITICAL";
        }
        
        if (report.getResponseTime() > 3000) { // 超过3秒
            return "WARNING";
        }
        
        if (report.getUsagePercentage() != null && report.getUsagePercentage() > 90) {
            return "WARNING";
        }
        
        return "HEALTHY";
    }
    
    private void updateConfigConnectionStatus(CloudStorageConfig config, String status) {
        try {
            config.setConnectionStatus(status);
            config.setUpdatedAt(LocalDateTime.now());
            cloudStorageConfigRepository.save(config);
        } catch (Exception e) {
            log.warn("更新配置连接状态失败: 配置ID={}", config.getId(), e);
        }
    }
    
    private void cachePerformanceMetrics(Long configId, PerformanceMetrics metrics) {
        List<PerformanceMetrics> metricsList = performanceCache.computeIfAbsent(
            configId, k -> new ArrayList<>());
        
        metricsList.add(metrics);
        
        // 保持缓存大小限制
        if (metricsList.size() > CACHE_SIZE) {
            metricsList.remove(0);
        }
    }
    
    private StorageHealthReport createErrorHealthReport(CloudStorageConfig config, Exception e) {
        StorageHealthReport report = new StorageHealthReport();
        report.setConfigId(config.getId());
        report.setConfigName(config.getConfigName());
        report.setProviderType(config.getProviderType().name());
        report.setRegion(config.getRegion());
        report.setCheckTime(LocalDateTime.now());
        report.setConnectionStatus("ERROR");
        report.setOverallHealth("CRITICAL");
        report.setLastCheckSuccess(false);
        report.setErrorMessage("监控失败: " + e.getMessage());
        return report;
    }
}