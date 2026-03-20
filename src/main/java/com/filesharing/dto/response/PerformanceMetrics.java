package com.filesharing.dto.response;

import java.time.LocalDateTime;

/**
 * 性能指标DTO
 */
public class PerformanceMetrics {
    private Long configId;
    private LocalDateTime timestamp;
    private Long connectionLatency; // 连接延迟(ms)
    private Double uploadSpeed;     // 上传速度(MB/s)
    private Double downloadSpeed;   // 下载速度(MB/s)
    private Double availability;    // 可用性百分比
    private String error;
    
    // Constructors
    public PerformanceMetrics() {}
    
    public PerformanceMetrics(Long configId, LocalDateTime timestamp) {
        this.configId = configId;
        this.timestamp = timestamp;
    }
    
    // Getters and Setters
    public Long getConfigId() { return configId; }
    public void setConfigId(Long configId) { this.configId = configId; }
    
    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
    
    public Long getConnectionLatency() { return connectionLatency; }
    public void setConnectionLatency(Long connectionLatency) { this.connectionLatency = connectionLatency; }
    
    public Double getUploadSpeed() { return uploadSpeed; }
    public void setUploadSpeed(Double uploadSpeed) { this.uploadSpeed = uploadSpeed; }
    
    public Double getDownloadSpeed() { return downloadSpeed; }
    public void setDownloadSpeed(Double downloadSpeed) { this.downloadSpeed = downloadSpeed; }
    
    public Double getAvailability() { return availability; }
    public void setAvailability(Double availability) { this.availability = availability; }
    
    public String getError() { return error; }
    public void setError(String error) { this.error = error; }
    
    // 便利方法
    public boolean hasError() {
        return error != null && !error.isEmpty();
    }
    
    public boolean isGoodPerformance() {
        return connectionLatency != null && connectionLatency < 1000 && // 延迟小于1秒
               uploadSpeed != null && uploadSpeed > 5 && // 上传速度大于5MB/s
               downloadSpeed != null && downloadSpeed > 10; // 下载速度大于10MB/s
    }
}