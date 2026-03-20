package com.filesharing.controller;

import com.filesharing.dto.ApiResponse;
import com.filesharing.monitoring.SystemMonitoringService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 系统监控控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/monitoring")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Tag(name = "系统监控", description = "系统性能监控和健康检查API")
public class MonitoringController {
    
    private final SystemMonitoringService monitoringService;
    
    /**
     * 获取系统性能指标
     */
    @GetMapping("/metrics")
    @Operation(summary = "获取系统指标", description = "获取当前系统的性能指标")
    public ResponseEntity<ApiResponse<SystemMonitoringService.SystemMetrics>> getSystemMetrics() {
        try {
            SystemMonitoringService.SystemMetrics metrics = monitoringService.collectSystemMetrics();
            return ResponseEntity.ok(ApiResponse.success("获取系统指标成功", metrics));
        } catch (Exception e) {
            log.error("获取系统指标失败", e);
            return ResponseEntity.badRequest().body(ApiResponse.error("获取指标失败: " + e.getMessage()));
        }
    }
    
    /**
     * 执行健康检查
     */
    @GetMapping("/health")
    @Operation(summary = "系统健康检查", description = "执行全面的系统健康检查")
    public ResponseEntity<ApiResponse<SystemMonitoringService.HealthCheckResult>> healthCheck() {
        try {
            SystemMonitoringService.HealthCheckResult result = monitoringService.performHealthCheck();
            return ResponseEntity.ok(ApiResponse.success("健康检查完成", result));
        } catch (Exception e) {
            log.error("健康检查失败", e);
            return ResponseEntity.badRequest().body(ApiResponse.error("健康检查失败: " + e.getMessage()));
        }
    }
    
    /**
     * 获取指标历史数据
     */
    @GetMapping("/metrics/history")
    @Operation(summary = "获取指标历史", description = "获取指定指标的历史数据")
    public ResponseEntity<ApiResponse<SystemMonitoringService.MetricHistory>> getMetricHistory(
            @Parameter(description = "指标名称") @RequestParam String metricName,
            @Parameter(description = "小时数") @RequestParam(defaultValue = "24") int hours) {
        try {
            SystemMonitoringService.MetricHistory history = monitoringService.getMetricHistory(metricName, hours);
            return ResponseEntity.ok(ApiResponse.success("获取历史数据成功", history));
        } catch (Exception e) {
            log.error("获取指标历史失败", e);
            return ResponseEntity.badRequest().body(ApiResponse.error("获取历史数据失败: " + e.getMessage()));
        }
    }
    
    /**
     * 生成性能报告
     */
    @GetMapping("/report")
    @Operation(summary = "生成性能报告", description = "生成指定时间范围内的性能分析报告")
    public ResponseEntity<ApiResponse<SystemMonitoringService.PerformanceReport>> generateReport(
            @Parameter(description = "天数") @RequestParam(defaultValue = "7") int days) {
        try {
            SystemMonitoringService.PerformanceReport report = monitoringService.generatePerformanceReport(days);
            return ResponseEntity.ok(ApiResponse.success("生成报告成功", report));
        } catch (Exception e) {
            log.error("生成性能报告失败", e);
            return ResponseEntity.badRequest().body(ApiResponse.error("生成报告失败: " + e.getMessage()));
        }
    }
    
    /**
     * 获取告警列表
     */
    @GetMapping("/alerts")
    @Operation(summary = "获取告警列表", description = "获取系统告警信息")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getAlerts(
            @Parameter(description = "告警级别") @RequestParam(required = false) String level,
            @Parameter(description = "告警状态") @RequestParam(required = false) String status,
            @Parameter(description = "数量限制") @RequestParam(defaultValue = "50") int limit) {
        try {
            List<SystemMonitoringService.Alert> alerts = monitoringService.getAlerts(level, status, limit);
            
            Map<String, Object> result = new HashMap<>();
            result.put("alerts", alerts);
            result.put("count", alerts.size());
            result.put("total", monitoringService.getAlerts(null, null, Integer.MAX_VALUE).size());
            
            return ResponseEntity.ok(ApiResponse.success("获取告警列表成功", result));
        } catch (Exception e) {
            log.error("获取告警列表失败", e);
            return ResponseEntity.badRequest().body(ApiResponse.error("获取告警列表失败: " + e.getMessage()));
        }
    }
    
    /**
     * 关闭告警
     */
    @PutMapping("/alerts/{alertId}/close")
    @Operation(summary = "关闭告警", description = "将指定告警标记为已关闭")
    public ResponseEntity<ApiResponse<Void>> closeAlert(
            @Parameter(description = "告警ID") @PathVariable String alertId) {
        try {
            monitoringService.closeAlert(alertId);
            return ResponseEntity.ok(ApiResponse.<Void>success(null));
        } catch (Exception e) {
            log.error("关闭告警失败: ID={}", alertId, e);
            return ResponseEntity.badRequest().body(ApiResponse.error("关闭告警失败: " + e.getMessage()));
        }
    }
    
    /**
     * 发送测试告警
     */
    @PostMapping("/alerts/test")
    @Operation(summary = "发送测试告警", description = "发送测试告警用于验证告警系统")
    public ResponseEntity<ApiResponse<Void>> sendTestAlert(
            @RequestBody Map<String, String> request) {
        try {
            String level = request.getOrDefault("level", "INFO");
            String title = request.getOrDefault("title", "测试告警");
            String message = request.getOrDefault("message", "这是一个测试告警消息");
            
            monitoringService.sendAlert(level, title, message);
            return ResponseEntity.ok(ApiResponse.<Void>success(null));
        } catch (Exception e) {
            log.error("发送测试告警失败", e);
            return ResponseEntity.badRequest().body(ApiResponse.error("发送测试告警失败: " + e.getMessage()));
        }
    }
    
    /**
     * 清理监控数据
     */
    @DeleteMapping("/cleanup")
    @Operation(summary = "清理监控数据", description = "清理过期的监控历史数据")
    public ResponseEntity<ApiResponse<Map<String, Object>>> cleanupData(
            @Parameter(description = "保留天数") @RequestParam(defaultValue = "30") int daysToKeep) {
        try {
            monitoringService.cleanupOldData(daysToKeep);
            
            Map<String, Object> result = new HashMap<>();
            result.put("message", "监控数据清理完成");
            result.put("daysKept", daysToKeep);
            result.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.ok(ApiResponse.success("清理完成", result));
        } catch (Exception e) {
            log.error("清理监控数据失败", e);
            return ResponseEntity.badRequest().body(ApiResponse.error("清理失败: " + e.getMessage()));
        }
    }
    
    /**
     * 获取监控统计信息
     */
    @GetMapping("/statistics")
    @Operation(summary = "获取监控统计", description = "获取系统监控的统计信息")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getMonitoringStatistics() {
        try {
            Map<String, Object> stats = new HashMap<>();
            
            // 这里可以添加各种监控统计信息
            stats.put("activeMetrics", "heap_memory_usage,thread_count,system_load");
            stats.put("monitoredComponents", "database,diskSpace,memory,network");
            stats.put("alertCount", monitoringService.getAlerts(null, "OPEN", 1000).size());
            stats.put("lastCheckTime", System.currentTimeMillis());
            
            return ResponseEntity.ok(ApiResponse.success("获取统计信息成功", stats));
        } catch (Exception e) {
            log.error("获取监控统计失败", e);
            return ResponseEntity.badRequest().body(ApiResponse.error("获取统计失败: " + e.getMessage()));
        }
    }
}