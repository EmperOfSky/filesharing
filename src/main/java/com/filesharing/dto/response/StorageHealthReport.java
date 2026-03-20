package com.filesharing.dto.response;

import java.time.LocalDateTime;

/**
 * 存储健康报告DTO
 */
public class StorageHealthReport {
    private Long configId;
    private String configName;
    private String providerType;
    private String region;
    private String connectionStatus;
    private String overallHealth;
    private Long responseTime;
    private LocalDateTime checkTime;
    private Boolean lastCheckSuccess;
    private Long totalSpace;
    private Long usedSpace;
    private Long availableSpace;
    private Double usagePercentage;
    private String errorMessage;
    
    // Constructors
    public StorageHealthReport() {}
    
    public StorageHealthReport(Long configId, String configName, String providerType, String overallHealth) {
        this.configId = configId;
        this.configName = configName;
        this.providerType = providerType;
        this.overallHealth = overallHealth;
    }
    
    // Getters and Setters
    public Long getConfigId() { return configId; }
    public void setConfigId(Long configId) { this.configId = configId; }
    
    public String getConfigName() { return configName; }
    public void setConfigName(String configName) { this.configName = configName; }
    
    public String getProviderType() { return providerType; }
    public void setProviderType(String providerType) { this.providerType = providerType; }
    
    public String getRegion() { return region; }
    public void setRegion(String region) { this.region = region; }
    
    public String getConnectionStatus() { return connectionStatus; }
    public void setConnectionStatus(String connectionStatus) { this.connectionStatus = connectionStatus; }
    
    public String getOverallHealth() { return overallHealth; }
    public void setOverallHealth(String overallHealth) { this.overallHealth = overallHealth; }
    
    public Long getResponseTime() { return responseTime; }
    public void setResponseTime(Long responseTime) { this.responseTime = responseTime; }
    
    public LocalDateTime getCheckTime() { return checkTime; }
    public void setCheckTime(LocalDateTime checkTime) { this.checkTime = checkTime; }
    
    public Boolean isLastCheckSuccess() { return lastCheckSuccess; }
    public void setLastCheckSuccess(Boolean lastCheckSuccess) { this.lastCheckSuccess = lastCheckSuccess; }
    
    public Long getTotalSpace() { return totalSpace; }
    public void setTotalSpace(Long totalSpace) { this.totalSpace = totalSpace; }
    
    public Long getUsedSpace() { return usedSpace; }
    public void setUsedSpace(Long usedSpace) { this.usedSpace = usedSpace; }
    
    public Long getAvailableSpace() { return availableSpace; }
    public void setAvailableSpace(Long availableSpace) { this.availableSpace = availableSpace; }
    
    public Double getUsagePercentage() { return usagePercentage; }
    public void setUsagePercentage(Double usagePercentage) { this.usagePercentage = usagePercentage; }
    
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    
    // 便利方法
    public boolean isHealthy() {
        return "HEALTHY".equals(overallHealth);
    }
    
    public boolean isWarning() {
        return "WARNING".equals(overallHealth);
    }
    
    public boolean isCritical() {
        return "CRITICAL".equals(overallHealth);
    }
}