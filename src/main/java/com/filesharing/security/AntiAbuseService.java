package com.filesharing.security;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 防刷服务 - 防止恶意请求和系统滥用
 */
@Slf4j
@Service
public class AntiAbuseService {
    
    // 存储IP访问频率记录
    private final Map<String, AccessFrequency> ipAccessRecords = new ConcurrentHashMap<>();
    
    // 存储用户行为模式
    private final Map<Long, UserBehaviorPattern> userBehaviorPatterns = new ConcurrentHashMap<>();
    
    // 存储API端点访问统计
    private final Map<String, EndpointStats> endpointStats = new ConcurrentHashMap<>();
    
    // 黑名单IP集合
    private final Set<String> blacklistedIps = ConcurrentHashMap.newKeySet();
    
    // 配置参数
    private static final int DEFAULT_RATE_LIMIT = 100; // 每分钟请求数
    private static final int STRICT_RATE_LIMIT = 20;   // 严格限制
    private static final int BLACKLIST_DURATION_MINUTES = 60; // 黑名单持续时间
    
    /**
     * 检查请求频率限制
     */
    public RateLimitResult checkRateLimit(String ipAddress, String endpoint, String userId) {
        RateLimitResult result = new RateLimitResult();
        
        try {
            // 检查IP是否在黑名单中
            if (blacklistedIps.contains(ipAddress)) {
                result.setAllowed(false);
                result.setReason("IP地址在黑名单中");
                return result;
            }
            
            // 获取或创建访问记录
            String key = ipAddress + ":" + endpoint;
            AccessFrequency frequency = ipAccessRecords.computeIfAbsent(key, 
                k -> new AccessFrequency(ipAddress, endpoint));
            
            // 检查频率限制
            int rateLimit = getRateLimitForEndpoint(endpoint);
            if (frequency.isExceedingLimit(rateLimit)) {
                result.setAllowed(false);
                result.setReason("请求频率超过限制");
                
                // 自动加入黑名单
                if (frequency.getViolationCount() > 3) {
                    addToBlacklist(ipAddress, "频繁违反频率限制");
                }
                
                log.warn("频率限制触发: IP={}, 端点={}, 限制={}", ipAddress, endpoint, rateLimit);
                return result;
            }
            
            // 记录访问
            frequency.recordAccess();
            
            result.setAllowed(true);
            result.setRemainingRequests(rateLimit - frequency.getRequestCountInWindow());
            result.setResetTime(frequency.getWindowEnd().toString());
            
        } catch (Exception e) {
            log.error("频率限制检查失败: IP={}, 端点={}", ipAddress, endpoint, e);
            result.setAllowed(false);
            result.setReason("系统错误");
        }
        
        return result;
    }
    
    /**
     * 分析用户行为模式
     */
    public BehaviorAnalysisResult analyzeUserBehavior(Long userId, String action, 
                                                    String resource, String ipAddress) {
        BehaviorAnalysisResult result = new BehaviorAnalysisResult();
        result.setUserId(userId);
        
        try {
            UserBehaviorPattern pattern = userBehaviorPatterns.computeIfAbsent(userId,
                id -> new UserBehaviorPattern(id));
            
            // 记录行为
            UserAction userAction = new UserAction(action, resource, ipAddress, LocalDateTime.now());
            pattern.addAction(userAction);
            
            // 分析异常行为
            if (pattern.hasAnomalousBehavior()) {
                result.setSuspicious(true);
                result.setRiskLevel(pattern.calculateRiskLevel());
                result.setAnomalies(pattern.getDetectedAnomalies());
                
                log.warn("检测到用户异常行为: 用户ID={}, 风险等级={}", userId, result.getRiskLevel());
            } else {
                result.setSuspicious(false);
                result.setRiskLevel("LOW");
            }
            
        } catch (Exception e) {
            log.error("用户行为分析失败: 用户ID={}", userId, e);
            result.setSuspicious(false);
            result.setRiskLevel("UNKNOWN");
        }
        
        return result;
    }
    
    /**
     * 更新API端点统计
     */
    public void updateEndpointStats(String endpoint, int responseTime, boolean success) {
        try {
            EndpointStats stats = endpointStats.computeIfAbsent(endpoint,
                ep -> new EndpointStats(ep));
            
            stats.updateStats(responseTime, success);
            
        } catch (Exception e) {
            log.error("更新端点统计失败: 端点={}", endpoint, e);
        }
    }
    
    /**
     * 获取端点健康状态
     */
    public EndpointHealth getEndpointHealth(String endpoint) {
        EndpointStats stats = endpointStats.get(endpoint);
        if (stats == null) {
            return new EndpointHealth(endpoint, "UNKNOWN", 0, 0, 0);
        }
        
        return new EndpointHealth(
            endpoint,
            stats.calculateHealthStatus(),
            stats.getAverageResponseTime(),
            stats.getErrorRate(),
            stats.getRequestCount().get()
        );
    }
    
    /**
     * 获取系统整体健康状态
     */
    public SystemHealth getSystemHealth() {
        SystemHealth health = new SystemHealth();
        
        try {
            // 计算总体指标
            long totalRequests = endpointStats.values().stream()
                .mapToLong(stats -> stats.getRequestCount().get())
                .sum();
            
            double avgResponseTime = endpointStats.values().stream()
                .mapToLong(stats -> stats.getTotalResponseTime().get())
                .sum() / (double) Math.max(totalRequests, 1);
            
            double errorRate = endpointStats.values().stream()
                .mapToLong(stats -> stats.getErrorCount().get())
                .sum() / (double) Math.max(totalRequests, 1);
            
            health.setTotalRequests(totalRequests);
            health.setAverageResponseTime(avgResponseTime);
            health.setErrorRate(errorRate);
            health.setBlacklistedIps(blacklistedIps.size());
            health.setActiveRateLimitedIps(ipAccessRecords.size());
            
            // 确定整体健康状态
            if (errorRate > 0.05 || avgResponseTime > 5000) {
                health.setStatus("DEGRADED");
            } else if (errorRate > 0.01 || avgResponseTime > 2000) {
                health.setStatus("WARNING");
            } else {
                health.setStatus("HEALTHY");
            }
            
        } catch (Exception e) {
            log.error("获取系统健康状态失败", e);
            health.setStatus("ERROR");
        }
        
        return health;
    }
    
    /**
     * 清理过期数据
     */
    public void cleanupOldData(int minutesToKeep) {
        try {
            LocalDateTime cutoffTime = LocalDateTime.now().minusMinutes(minutesToKeep);
            
            // 清理IP访问记录
            int ipRecordsRemoved = ipAccessRecords.size();
            ipAccessRecords.entrySet().removeIf(entry -> 
                entry.getValue().getLastAccess().isBefore(cutoffTime));
            ipRecordsRemoved -= ipAccessRecords.size();
            
            // 清理用户行为记录
            userBehaviorPatterns.values().forEach(pattern -> 
                pattern.cleanupOldActions(cutoffTime));
            
            // 清理过期黑名单
            // 注意：黑名单清理应该基于添加时间，这里简化处理
            
            log.info("清理过期数据: 移除IP记录{}条", ipRecordsRemoved);
            
        } catch (Exception e) {
            log.error("清理过期数据失败", e);
        }
    }
    
    /**
     * 手动添加IP到黑名单
     */
    public void manuallyBlacklistIp(String ipAddress, String reason, String adminUser) {
        try {
            blacklistedIps.add(ipAddress);
            log.warn("管理员手动将IP加入黑名单: IP={}, 原因={}, 操作者={}", 
                ipAddress, reason, adminUser);
                
            // 记录到安全日志
            // securityAuditService.logSecurityEvent("MANUAL_BLACKLIST", adminUser, ipAddress, 
            //     "Manual blacklist addition", Map.of("reason", reason));
            
        } catch (Exception e) {
            log.error("手动添加IP到黑名单失败: {}", ipAddress, e);
        }
    }
    
    /**
     * 获取防刷统计信息
     */
    public AntiAbuseStatistics getStatistics() {
        AntiAbuseStatistics stats = new AntiAbuseStatistics();
        
        try {
            stats.setTrackedIps(ipAccessRecords.size());
            stats.setBlacklistedIps(blacklistedIps.size());
            stats.setMonitoredEndpoints(endpointStats.size());
            
            long totalViolations = ipAccessRecords.values().stream()
                .mapToLong(AccessFrequency::getViolationCount)
                .sum();
            stats.setTotalViolations(totalViolations);
            
            double avgViolationsPerIp = ipAccessRecords.isEmpty() ? 0 :
                (double) totalViolations / ipAccessRecords.size();
            stats.setAvgViolationsPerIp(avgViolationsPerIp);
            
        } catch (Exception e) {
            log.error("获取防刷统计信息失败", e);
        }
        
        return stats;
    }
    
    // ==================== 私有辅助方法 ====================
    
    private int getRateLimitForEndpoint(String endpoint) {
        // 根据端点类型返回不同的限制
        if (endpoint.contains("/admin/") || endpoint.contains("/api/system/")) {
            return STRICT_RATE_LIMIT;
        } else if (endpoint.contains("/upload") || endpoint.contains("/download")) {
            return DEFAULT_RATE_LIMIT / 2; // 文件操作半速限制
        } else {
            return DEFAULT_RATE_LIMIT;
        }
    }
    
    private void addToBlacklist(String ipAddress, String reason) {
        blacklistedIps.add(ipAddress);
        log.warn("自动将IP加入黑名单: IP={}, 原因={}", ipAddress, reason);
    }
    
    // ==================== 内部类 ====================
    
    @Data
    public static class RateLimitResult {
        private boolean allowed;
        private String reason;
        private int remainingRequests;
        private String resetTime;
    }
    
    @Data
    public static class BehaviorAnalysisResult {
        private Long userId;
        private boolean suspicious;
        private String riskLevel;
        private java.util.List<String> anomalies;
    }
    
    @Data
    public static class EndpointHealth {
        private String endpoint;
        private String status;
        private double averageResponseTime;
        private double errorRate;
        private long requestCount;
        
        public EndpointHealth(String endpoint, String status, double avgTime, double errorRate, long count) {
            this.endpoint = endpoint;
            this.status = status;
            this.averageResponseTime = avgTime;
            this.errorRate = errorRate;
            this.requestCount = count;
        }
    }
    
    @Data
    public static class SystemHealth {
        private String status;
        private long totalRequests;
        private double averageResponseTime;
        private double errorRate;
        private int blacklistedIps;
        private int activeRateLimitedIps;
    }
    
    @Data
    public static class AntiAbuseStatistics {
        private int trackedIps;
        private int blacklistedIps;
        private int monitoredEndpoints;
        private long totalViolations;
        private double avgViolationsPerIp;
    }
    
    @Data
    public static class AccessFrequency {
        private String ipAddress;
        private String endpoint;
        private java.util.Queue<LocalDateTime> accessTimes;
        private LocalDateTime windowStart;
        private LocalDateTime windowEnd;
        private AtomicLong violationCount;
        private LocalDateTime lastAccess;
        
        public AccessFrequency(String ipAddress, String endpoint) {
            this.ipAddress = ipAddress;
            this.endpoint = endpoint;
            this.accessTimes = new java.util.LinkedList<>();
            this.windowStart = LocalDateTime.now();
            this.windowEnd = windowStart.plusMinutes(1);
            this.violationCount = new AtomicLong(0);
        }
        
        public void recordAccess() {
            LocalDateTime now = LocalDateTime.now();
            this.lastAccess = now;
            
            // 清理过期的访问记录
            accessTimes.removeIf(time -> time.isBefore(windowStart));
            
            // 添加新的访问记录
            accessTimes.offer(now);
            
            // 更新窗口
            if (now.isAfter(windowEnd)) {
                windowStart = now;
                windowEnd = now.plusMinutes(1);
                accessTimes.clear();
                accessTimes.offer(now);
            }
        }
        
        public boolean isExceedingLimit(int limit) {
            return getRequestCountInWindow() > limit;
        }
        
        public int getRequestCountInWindow() {
            return accessTimes.size();
        }
        
        public long getViolationCount() {
            return violationCount.get();
        }
    }
    
    @Data
    public static class UserBehaviorPattern {
        private Long userId;
        private java.util.List<UserAction> actions;
        private LocalDateTime createdAt;
        
        public UserBehaviorPattern(Long userId) {
            this.userId = userId;
            this.actions = java.util.Collections.synchronizedList(new java.util.ArrayList<>());
            this.createdAt = LocalDateTime.now();
        }
        
        public void addAction(UserAction action) {
            actions.add(action);
            // 保持最近100个动作
            if (actions.size() > 100) {
                actions.remove(0);
            }
        }
        
        public boolean hasAnomalousBehavior() {
            // 简化的异常行为检测
            if (actions.size() < 10) return false;
            
            // 检查高频操作
            Map<String, Long> actionCounts = actions.stream()
                .collect(java.util.stream.Collectors.groupingBy(
                    UserAction::getAction, 
                    java.util.stream.Collectors.counting()));
            
            return actionCounts.values().stream().anyMatch(count -> count > 20);
        }
        
        public String calculateRiskLevel() {
            if (actions.size() < 5) return "LOW";
            
            long suspiciousActions = actions.stream()
                .filter(action -> isSuspiciousAction(action.getAction()))
                .count();
                
            double suspiciousRatio = (double) suspiciousActions / actions.size();
            
            if (suspiciousRatio > 0.5) return "CRITICAL";
            if (suspiciousRatio > 0.2) return "HIGH";
            if (suspiciousRatio > 0.05) return "MEDIUM";
            return "LOW";
        }
        
        public java.util.List<String> getDetectedAnomalies() {
            java.util.List<String> anomalies = new java.util.ArrayList<>();
            
            if (hasAnomalousBehavior()) {
                anomalies.add("检测到高频重复操作");
            }
            
            long differentIps = actions.stream()
                .map(UserAction::getIpAddress)
                .distinct()
                .count();
                
            if (differentIps > 3) {
                anomalies.add("多个不同IP地址访问");
            }
            
            return anomalies;
        }
        
        public void cleanupOldActions(LocalDateTime cutoffTime) {
            actions.removeIf(action -> action.getTimestamp().isBefore(cutoffTime));
        }
        
        private boolean isSuspiciousAction(String action) {
            Set<String> suspiciousActions = Set.of(
                "DELETE", "MODIFY_PERMISSIONS", "ACCESS_SENSITIVE_DATA"
            );
            return suspiciousActions.contains(action);
        }
    }
    
    @Data
    public static class UserAction {
        private String action;
        private String resource;
        private String ipAddress;
        private LocalDateTime timestamp;
        
        public UserAction(String action, String resource, String ipAddress, LocalDateTime timestamp) {
            this.action = action;
            this.resource = resource;
            this.ipAddress = ipAddress;
            this.timestamp = timestamp;
        }
    }
    
    @Data
    public static class EndpointStats {
        private String endpoint;
        private AtomicLong requestCount;
        private AtomicLong errorCount;
        private AtomicLong totalResponseTime;
        private LocalDateTime lastAccess;
        
        public EndpointStats(String endpoint) {
            this.endpoint = endpoint;
            this.requestCount = new AtomicLong(0);
            this.errorCount = new AtomicLong(0);
            this.totalResponseTime = new AtomicLong(0);
            this.lastAccess = LocalDateTime.now();
        }
        
        public void updateStats(int responseTime, boolean success) {
            requestCount.incrementAndGet();
            totalResponseTime.addAndGet(responseTime);
            if (!success) {
                errorCount.incrementAndGet();
            }
            this.lastAccess = LocalDateTime.now();
        }
        
        public double getAverageResponseTime() {
            long count = requestCount.get();
            return count > 0 ? (double) totalResponseTime.get() / count : 0;
        }
        
        public double getErrorRate() {
            long count = requestCount.get();
            return count > 0 ? (double) errorCount.get() / count : 0;
        }
        
        public String calculateHealthStatus() {
            double errorRate = getErrorRate();
            double avgTime = getAverageResponseTime();
            
            if (errorRate > 0.05 || avgTime > 5000) return "UNHEALTHY";
            if (errorRate > 0.01 || avgTime > 2000) return "DEGRADED";
            return "HEALTHY";
        }
    }
}