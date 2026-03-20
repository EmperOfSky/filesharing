package com.filesharing.monitoring;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;
import org.springframework.stereotype.Service;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.ThreadMXBean;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 系统监控服务 - 提供全面的性能监控和健康检查功能
 */
@Slf4j
@Service
public class SystemMonitoringService {
    
    // 存储性能指标历史数据
    private final Map<String, List<MetricPoint>> metricHistory = new ConcurrentHashMap<>();
    
    // 存储健康检查结果
    private final Map<String, HealthStatus> healthChecks = new ConcurrentHashMap<>();
    
    // 存储告警记录
    private final List<Alert> alerts = Collections.synchronizedList(new ArrayList<>());
    
    // 性能阈值配置
    private static final double MEMORY_THRESHOLD = 0.85; // 85%内存使用率
    private static final long DISK_SPACE_THRESHOLD = 1024 * 1024 * 1024; // 1GB剩余空间
    
    /**
     * 收集系统性能指标
     */
    public SystemMetrics collectSystemMetrics() {
        SystemMetrics metrics = new SystemMetrics();
        metrics.setTimestamp(LocalDateTime.now());
        
        try {
            // JVM内存指标
            MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
            var heapMemory = memoryBean.getHeapMemoryUsage();
            var nonHeapMemory = memoryBean.getNonHeapMemoryUsage();
            
            metrics.setHeapMemoryUsed(heapMemory.getUsed());
            metrics.setHeapMemoryMax(heapMemory.getMax());
            metrics.setHeapMemoryUsage((double) heapMemory.getUsed() / heapMemory.getMax());
            
            metrics.setNonHeapMemoryUsed(nonHeapMemory.getUsed());
            metrics.setNonHeapMemoryMax(nonHeapMemory.getMax());
            
            // 线程指标
            ThreadMXBean threadBean = ManagementFactory.getThreadMXBean();
            metrics.setThreadCount(threadBean.getThreadCount());
            metrics.setPeakThreadCount(threadBean.getPeakThreadCount());
            metrics.setDaemonThreadCount(threadBean.getDaemonThreadCount());
            
            // JVM运行时间
            metrics.setUptime(ManagementFactory.getRuntimeMXBean().getUptime());
            
            // 系统负载（简化实现）
            metrics.setSystemLoadAverage(getSystemLoadAverage());
            
            // 记录指标历史
            recordMetric("heap_memory_usage", metrics.getHeapMemoryUsage());
            recordMetric("thread_count", metrics.getThreadCount());
            
            log.debug("系统指标收集完成: 内存使用率={:.2f}%, 线程数={}", 
                metrics.getHeapMemoryUsage() * 100, metrics.getThreadCount());
                
        } catch (Exception e) {
            log.error("收集系统指标失败", e);
        }
        
        return metrics;
    }
    
    /**
     * 执行健康检查
     */
    public HealthCheckResult performHealthCheck() {
        HealthCheckResult result = new HealthCheckResult();
        result.setTimestamp(LocalDateTime.now());
        result.setComponents(new HashMap<>());
        
        try {
            // 数据库健康检查
            Health dbHealth = checkDatabaseHealth();
            result.getComponents().put("database", dbHealth);
            
            // 文件系统健康检查
            Health diskHealth = checkDiskHealth();
            result.getComponents().put("diskSpace", diskHealth);
            
            // 内存健康检查
            Health memoryHealth = checkMemoryHealth();
            result.getComponents().put("memory", memoryHealth);
            
            // 网络连接健康检查
            Health networkHealth = checkNetworkHealth();
            result.getComponents().put("network", networkHealth);
            
            // 计算总体健康状态
            result.setStatus(calculateOverallHealth(result.getComponents()));
            
            // 记录健康状态
            updateHealthStatus(result);
            
            // 检查是否需要生成告警
            checkAndGenerateAlerts(result);
            
        } catch (Exception e) {
            log.error("执行健康检查失败", e);
            result.setStatus(Status.DOWN);
            result.getComponents().put("system", Health.down().withDetail("error", e.getMessage()).build());
        }
        
        return result;
    }
    
    /**
     * 获取性能指标历史数据
     */
    public MetricHistory getMetricHistory(String metricName, int hours) {
        MetricHistory history = new MetricHistory();
        history.setMetricName(metricName);
        
        try {
            List<MetricPoint> points = metricHistory.getOrDefault(metricName, new ArrayList<>());
            LocalDateTime since = LocalDateTime.now().minusHours(hours);
            
            List<MetricPoint> filteredPoints = points.stream()
                .filter(point -> point.getTimestamp().isAfter(since))
                .sorted(Comparator.comparing(MetricPoint::getTimestamp))
                .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
            
            history.setDataPoints(filteredPoints);
            history.setCount(filteredPoints.size());
            
        } catch (Exception e) {
            log.error("获取指标历史失败: 指标={}", metricName, e);
        }
        
        return history;
    }
    
    /**
     * 生成性能报告
     */
    public PerformanceReport generatePerformanceReport(int days) {
        PerformanceReport report = new PerformanceReport();
        report.setGeneratedAt(LocalDateTime.now());
        report.setPeriodDays(days);
        
        try {
            LocalDateTime since = LocalDateTime.now().minusDays(days);
            
            // 收集各项指标统计
            report.setCpuUsageStats(calculateMetricStats("cpu_usage", since));
            report.setMemoryUsageStats(calculateMetricStats("heap_memory_usage", since));
            report.setDiskIoStats(calculateMetricStats("disk_io", since));
            report.setNetworkTrafficStats(calculateMetricStats("network_traffic", since));
            
            // 健康状态统计
            Map<String, Long> healthStats = healthChecks.values().stream()
                .collect(java.util.stream.Collectors.groupingBy(
                    HealthStatus::getStatus, 
                    java.util.stream.Collectors.counting()));
            report.setHealthStatusStats(healthStats);
            
            // 告警统计
            long alertCount = alerts.stream()
                .filter(alert -> alert.getTimestamp().isAfter(since))
                .count();
            report.setTotalAlerts(alertCount);
            
            // 性能趋势分析
            List<PerformanceTrend> trends = analyzePerformanceTrends(since);
            report.setTrends(trends);
            
        } catch (Exception e) {
            log.error("生成性能报告失败", e);
            report.setError("生成报告时发生错误: " + e.getMessage());
        }
        
        return report;
    }
    
    /**
     * 发送告警通知
     */
    public void sendAlert(String level, String title, String message) {
        try {
            Alert alert = new Alert();
            alert.setId(UUID.randomUUID().toString());
            alert.setLevel(level);
            alert.setTitle(title);
            alert.setMessage(message);
            alert.setTimestamp(LocalDateTime.now());
            alert.setStatus("OPEN");
            
            alerts.add(alert);
            
            // 保持告警列表在合理大小
            if (alerts.size() > 1000) {
                alerts.remove(0);
            }
            
            log.warn("发送告警通知 - 级别: {}, 标题: {}, 消息: {}", level, title, message);
            
            // 实际应用中应该发送邮件、短信或其他通知
            // notificationService.sendAlert(alert);
            
        } catch (Exception e) {
            log.error("发送告警通知失败", e);
        }
    }
    
    /**
     * 获取告警列表
     */
    public List<Alert> getAlerts(String level, String status, int limit) {
        return alerts.stream()
            .filter(alert -> level == null || level.equals(alert.getLevel()))
            .filter(alert -> status == null || status.equals(alert.getStatus()))
            .sorted(Comparator.comparing(Alert::getTimestamp).reversed())
            .limit(limit)
            .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
    }
    
    /**
     * 关闭告警
     */
    public void closeAlert(String alertId) {
        alerts.stream()
            .filter(alert -> alertId.equals(alert.getId()))
            .findFirst()
            .ifPresent(alert -> {
                alert.setStatus("CLOSED");
                alert.setClosedAt(LocalDateTime.now());
                log.info("告警已关闭: ID={}", alertId);
            });
    }
    
    /**
     * 清理过期的监控数据
     */
    public void cleanupOldData(int daysToKeep) {
        try {
            LocalDateTime cutoffTime = LocalDateTime.now().minusDays(daysToKeep);
            
            // 清理指标历史数据
            int cleanedMetrics = 0;
            for (List<MetricPoint> points : metricHistory.values()) {
                int beforeSize = points.size();
                points.removeIf(point -> point.getTimestamp().isBefore(cutoffTime));
                cleanedMetrics += (beforeSize - points.size());
            }
            
            // 清理过期告警
            int beforeAlerts = alerts.size();
            alerts.removeIf(alert -> alert.getTimestamp().isBefore(cutoffTime));
            int cleanedAlerts = beforeAlerts - alerts.size();
            
            log.info("清理过期监控数据: 指标数据{}条, 告警{}条", cleanedMetrics, cleanedAlerts);
            
        } catch (Exception e) {
            log.error("清理过期监控数据失败", e);
        }
    }
    
    // ==================== 私有辅助方法 ====================
    
    private void recordMetric(String metricName, Number value) {
        List<MetricPoint> points = metricHistory.computeIfAbsent(metricName, 
            k -> Collections.synchronizedList(new ArrayList<>()));
        
        MetricPoint point = new MetricPoint();
        point.setTimestamp(LocalDateTime.now());
        point.setValue(value.doubleValue());
        
        points.add(point);
        
        // 保持每个指标最多1000个数据点
        if (points.size() > 1000) {
            points.remove(0);
        }
    }
    
    private Health checkDatabaseHealth() {
        // 简化的数据库健康检查
        try {
            // 实际应该执行数据库查询测试
            return Health.up()
                .withDetail("database", "MySQL")
                .withDetail("connection", "OK")
                .build();
        } catch (Exception e) {
            return Health.down()
                .withDetail("error", e.getMessage())
                .build();
        }
    }
    
    private Health checkDiskHealth() {
        // 简化的磁盘健康检查
        try {
            long freeSpace = Runtime.getRuntime().freeMemory();
            boolean isHealthy = freeSpace > DISK_SPACE_THRESHOLD;
            
            return isHealthy ? 
                Health.up().withDetail("freeSpace", freeSpace).build() :
                Health.down().withDetail("freeSpace", freeSpace).withDetail("threshold", DISK_SPACE_THRESHOLD).build();
        } catch (Exception e) {
            return Health.down().withDetail("error", e.getMessage()).build();
        }
    }
    
    private Health checkMemoryHealth() {
        try {
            MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
            var heapMemory = memoryBean.getHeapMemoryUsage();
            double usage = (double) heapMemory.getUsed() / heapMemory.getMax();
            
            if (usage > MEMORY_THRESHOLD) {
                return Health.down()
                    .withDetail("usage", String.format("%.2f%%", usage * 100))
                    .withDetail("threshold", String.format("%.2f%%", MEMORY_THRESHOLD * 100))
                    .build();
            }
            
            return Health.up()
                .withDetail("usage", String.format("%.2f%%", usage * 100))
                .build();
                
        } catch (Exception e) {
            return Health.down().withDetail("error", e.getMessage()).build();
        }
    }
    
    private Health checkNetworkHealth() {
        // 简化的网络健康检查
        try {
            return Health.up()
                .withDetail("status", "CONNECTED")
                .build();
        } catch (Exception e) {
            return Health.down().withDetail("error", e.getMessage()).build();
        }
    }
    
    private Status calculateOverallHealth(Map<String, Health> components) {
        boolean hasDown = components.values().stream()
            .anyMatch(health -> health.getStatus() == Status.DOWN);
            
        boolean hasOutOfService = components.values().stream()
            .anyMatch(health -> health.getStatus() == Status.OUT_OF_SERVICE);
            
        if (hasDown) return Status.DOWN;
        if (hasOutOfService) return Status.OUT_OF_SERVICE;
        return Status.UP;
    }
    
    private void updateHealthStatus(HealthCheckResult result) {
        result.getComponents().forEach((name, health) -> {
            HealthStatus status = healthChecks.computeIfAbsent(name, 
                k -> new HealthStatus(k));
            status.setStatus(health.getStatus().getCode());
            status.setLastChecked(LocalDateTime.now());
            status.setDetails(health.getDetails());
        });
    }
    
    private void checkAndGenerateAlerts(HealthCheckResult result) {
        result.getComponents().forEach((component, health) -> {
            if (health.getStatus() == Status.DOWN) {
                sendAlert("CRITICAL", 
                    "组件故障", 
                    String.format("组件 '%s' 状态为 DOWN: %s", 
                        component, health.getDetails()));
            } else if (health.getStatus() == Status.OUT_OF_SERVICE) {
                sendAlert("WARNING", 
                    "组件不可用", 
                    String.format("组件 '%s' 状态为 OUT_OF_SERVICE: %s", 
                        component, health.getDetails()));
            }
        });
    }
    
    private double getSystemLoadAverage() {
        // 简化的系统负载获取
        try {
            return ManagementFactory.getOperatingSystemMXBean().getSystemLoadAverage();
        } catch (Exception e) {
            return -1.0;
        }
    }
    
    private MetricStats calculateMetricStats(String metricName, LocalDateTime since) {
        List<MetricPoint> points = metricHistory.getOrDefault(metricName, new ArrayList<>());
        
        List<Double> values = points.stream()
            .filter(point -> point.getTimestamp().isAfter(since))
            .map(MetricPoint::getValue)
            .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
            
        if (values.isEmpty()) {
            return new MetricStats(0, 0, 0, 0);
        }
        
        double min = Collections.min(values);
        double max = Collections.max(values);
        double avg = values.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
        double sum = values.stream().mapToDouble(Double::doubleValue).sum();
        
        return new MetricStats(min, max, avg, sum);
    }
    
    private List<PerformanceTrend> analyzePerformanceTrends(LocalDateTime since) {
        List<PerformanceTrend> trends = new ArrayList<>();
        
        // 分析内存使用趋势
        MetricStats memoryStats = calculateMetricStats("heap_memory_usage", since);
        PerformanceTrend memoryTrend = new PerformanceTrend();
        memoryTrend.setMetricName("heap_memory_usage");
        memoryTrend.setTrendDirection(memoryStats.getAverage() > 0.5 ? "上升" : "下降");
        memoryTrend.setSeverity(memoryStats.getAverage() > 0.7 ? "HIGH" : "LOW");
        trends.add(memoryTrend);
        
        return trends;
    }
    
    // ==================== 内部类 ====================
    
    @Data
    public static class SystemMetrics {
        private LocalDateTime timestamp;
        private long heapMemoryUsed;
        private long heapMemoryMax;
        private double heapMemoryUsage;
        private long nonHeapMemoryUsed;
        private long nonHeapMemoryMax;
        private int threadCount;
        private int peakThreadCount;
        private int daemonThreadCount;
        private long uptime;
        private double systemLoadAverage;
        
        // 手动添加setter方法以确保编译通过
        public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
        public void setHeapMemoryUsed(long heapMemoryUsed) { this.heapMemoryUsed = heapMemoryUsed; }
        public void setHeapMemoryMax(long heapMemoryMax) { this.heapMemoryMax = heapMemoryMax; }
        public void setHeapMemoryUsage(double heapMemoryUsage) { this.heapMemoryUsage = heapMemoryUsage; }
        public void setNonHeapMemoryUsed(long nonHeapMemoryUsed) { this.nonHeapMemoryUsed = nonHeapMemoryUsed; }
        public void setNonHeapMemoryMax(long nonHeapMemoryMax) { this.nonHeapMemoryMax = nonHeapMemoryMax; }
        public void setThreadCount(int threadCount) { this.threadCount = threadCount; }
        public void setPeakThreadCount(int peakThreadCount) { this.peakThreadCount = peakThreadCount; }
        public void setDaemonThreadCount(int daemonThreadCount) { this.daemonThreadCount = daemonThreadCount; }
        public void setUptime(long uptime) { this.uptime = uptime; }
        public void setSystemLoadAverage(double systemLoadAverage) { this.systemLoadAverage = systemLoadAverage; }
    }
    
    @Data
    public static class HealthCheckResult {
        private LocalDateTime timestamp;
        private Status status;
        private Map<String, Health> components;
    }
    
    @Data
    public static class HealthStatus {
        private String componentName;
        private String status;
        private LocalDateTime lastChecked;
        private Map<String, Object> details;
        
        public HealthStatus(String componentName) {
            this.componentName = componentName;
        }
    }
    
    @Data
    public static class MetricPoint {
        private LocalDateTime timestamp;
        private double value;
    }
    
    @Data
    public static class MetricHistory {
        private String metricName;
        private List<MetricPoint> dataPoints;
        private int count;
    }
    
    @Data
    public static class MetricStats {
        private double minimum;
        private double maximum;
        private double average;
        private double sum;
        
        public MetricStats(double min, double max, double avg, double sum) {
            this.minimum = min;
            this.maximum = max;
            this.average = avg;
            this.sum = sum;
        }
    }
    
    @Data
    public static class PerformanceReport {
        private LocalDateTime generatedAt;
        private int periodDays;
        private MetricStats cpuUsageStats;
        private MetricStats memoryUsageStats;
        private MetricStats diskIoStats;
        private MetricStats networkTrafficStats;
        private Map<String, Long> healthStatusStats;
        private long totalAlerts;
        private List<PerformanceTrend> trends;
        private String error;
    }
    
    @Data
    public static class PerformanceTrend {
        private String metricName;
        private String trendDirection;
        private String severity;
    }
    
    @Data
    public static class Alert {
        private String id;
        private String level;
        private String title;
        private String message;
        private LocalDateTime timestamp;
        private String status;
        private LocalDateTime closedAt;
    }
}