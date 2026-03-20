package com.filesharing.audit;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 安全审计服务 - 记录和分析系统安全事件
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SecurityAuditService {
    
    private final ObjectMapper objectMapper;
    
    // 存储审计日志
    private final List<AuditLog> auditLogs = Collections.synchronizedList(new ArrayList<>());
    
    // 存储安全事件统计
    private final Map<String, EventCounter> eventCounters = new ConcurrentHashMap<>();
    
    // 审计配置
    private static final int MAX_LOG_ENTRIES = 10000;
    private static final int ALERT_THRESHOLD = 10; // 异常事件阈值
    
    /**
     * 记录安全审计日志
     */
    public void logSecurityEvent(String eventType, String userId, String ipAddress, 
                               String resource, Map<String, Object> details) {
        try {
            AuditLog logEntry = new AuditLog();
            logEntry.setId(UUID.randomUUID().toString());
            logEntry.setEventType(eventType);
            logEntry.setUserId(userId);
            logEntry.setIpAddress(ipAddress);
            logEntry.setResource(resource);
            logEntry.setDetails(details);
            logEntry.setTimestamp(LocalDateTime.now());
            
            // 添加到审计日志
            auditLogs.add(logEntry);
            
            // 维护日志大小
            if (auditLogs.size() > MAX_LOG_ENTRIES) {
                auditLogs.remove(0);
            }
            
            // 更新事件计数器
            EventCounter counter = eventCounters.computeIfAbsent(eventType, 
                k -> new EventCounter(k));
            counter.increment();
            
            // 检查是否需要告警
            if (counter.getCount() % ALERT_THRESHOLD == 0) {
                generateSecurityAlert(eventType, counter.getCount());
            }
            
            // 记录到系统日志
            log.info("安全审计 - 类型: {}, 用户: {}, IP: {}, 资源: {}", 
                eventType, userId, ipAddress, resource);
                
        } catch (Exception e) {
            log.error("记录安全审计日志失败", e);
        }
    }
    
    /**
     * 获取指定时间范围内的审计日志
     */
    public List<AuditLog> getAuditLogs(LocalDateTime startTime, LocalDateTime endTime, 
                                     String eventType, String userId) {
        return auditLogs.stream()
            .filter(log -> log.getTimestamp().isAfter(startTime) && log.getTimestamp().isBefore(endTime))
            .filter(log -> eventType == null || eventType.equals(log.getEventType()))
            .filter(log -> userId == null || userId.equals(log.getUserId()))
            .sorted(Comparator.comparing(AuditLog::getTimestamp).reversed())
            .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
    }
    
    /**
     * 获取安全事件统计
     */
    public Map<String, Object> getSecurityStatistics(int hours) {
        Map<String, Object> statistics = new HashMap<>();
        
        try {
            LocalDateTime since = LocalDateTime.now().minusHours(hours);
            
            // 按事件类型统计
            Map<String, Long> eventTypeStats = auditLogs.stream()
                .filter(log -> log.getTimestamp().isAfter(since))
                .collect(java.util.stream.Collectors.groupingBy(
                    AuditLog::getEventType, 
                    java.util.stream.Collectors.counting()));
            statistics.put("eventTypeStats", eventTypeStats);
            
            // 按用户统计
            Map<String, Long> userStats = auditLogs.stream()
                .filter(log -> log.getTimestamp().isAfter(since))
                .filter(log -> log.getUserId() != null)
                .collect(java.util.stream.Collectors.groupingBy(
                    AuditLog::getUserId, 
                    java.util.stream.Collectors.counting()));
            statistics.put("userActivityStats", userStats);
            
            // 按IP统计
            Map<String, Long> ipStats = auditLogs.stream()
                .filter(log -> log.getTimestamp().isAfter(since))
                .filter(log -> log.getIpAddress() != null)
                .collect(java.util.stream.Collectors.groupingBy(
                    AuditLog::getIpAddress, 
                    java.util.stream.Collectors.counting()));
            statistics.put("ipActivityStats", ipStats);
            
            // 异常事件统计
            long suspiciousEvents = auditLogs.stream()
                .filter(log -> log.getTimestamp().isAfter(since))
                .filter(log -> isSuspiciousEvent(log.getEventType()))
                .count();
            statistics.put("suspiciousEvents", suspiciousEvents);
            
            statistics.put("totalEvents", auditLogs.size());
            statistics.put("timeRangeHours", hours);
            
        } catch (Exception e) {
            log.error("获取安全统计失败", e);
        }
        
        return statistics;
    }
    
    /**
     * 生成安全报告
     */
    public SecurityReport generateSecurityReport(int days) {
        SecurityReport report = new SecurityReport();
        report.setGeneratedAt(LocalDateTime.now());
        report.setPeriodDays(days);
        
        try {
            LocalDateTime since = LocalDateTime.now().minusDays(days);
            
            // 基本统计
            report.setTotalEvents(auditLogs.stream()
                .filter(log -> log.getTimestamp().isAfter(since))
                .count());
            
            report.setUniqueUsers(auditLogs.stream()
                .filter(log -> log.getTimestamp().isAfter(since))
                .filter(log -> log.getUserId() != null)
                .map(AuditLog::getUserId)
                .distinct()
                .count());
            
            report.setUniqueIps(auditLogs.stream()
                .filter(log -> log.getTimestamp().isAfter(since))
                .filter(log -> log.getIpAddress() != null)
                .map(AuditLog::getIpAddress)
                .distinct()
                .count());
            
            // 高风险事件
            List<AuditLog> highRiskEvents = auditLogs.stream()
                .filter(log -> log.getTimestamp().isAfter(since))
                .filter(log -> isHighRiskEvent(log.getEventType()))
                .sorted(Comparator.comparing(AuditLog::getTimestamp).reversed())
                .limit(50)
                .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
            report.setHighRiskEvents(highRiskEvents);
            
            // 异常模式检测
            List<String> detectedPatterns = detectAnomalousPatterns(since);
            report.setAnomalousPatterns(detectedPatterns);
            
            // 建议措施
            List<String> recommendations = generateRecommendations(detectedPatterns);
            report.setRecommendations(recommendations);
            
        } catch (Exception e) {
            log.error("生成安全报告失败", e);
            report.setError("生成报告时发生错误: " + e.getMessage());
        }
        
        return report;
    }
    
    /**
     * 导出审计日志
     */
    public String exportAuditLogs(LocalDateTime startTime, LocalDateTime endTime, String format) {
        try {
            List<AuditLog> logsToExport = getAuditLogs(startTime, endTime, null, null);
            
            switch (format.toLowerCase()) {
                case "json":
                    return objectMapper.writerWithDefaultPrettyPrinter()
                        .writeValueAsString(logsToExport);
                case "csv":
                    return generateCsvExport(logsToExport);
                default:
                    throw new IllegalArgumentException("不支持的导出格式: " + format);
            }
            
        } catch (Exception e) {
            log.error("导出审计日志失败", e);
            throw new RuntimeException("导出失败: " + e.getMessage());
        }
    }
    
    /**
     * 清理过期的审计日志
     */
    public void cleanupOldLogs(int daysToKeep) {
        try {
            LocalDateTime cutoffTime = LocalDateTime.now().minusDays(daysToKeep);
            int removedCount = auditLogs.size();
            
            auditLogs.removeIf(log -> log.getTimestamp().isBefore(cutoffTime));
            
            removedCount -= auditLogs.size();
            log.info("清理过期审计日志: 移除了 {} 条记录", removedCount);
            
        } catch (Exception e) {
            log.error("清理审计日志失败", e);
        }
    }
    
    // ==================== 私有辅助方法 ====================
    
    private boolean isSuspiciousEvent(String eventType) {
        Set<String> suspiciousTypes = Set.of(
            "FAILED_LOGIN", "UNAUTHORIZED_ACCESS", "BRUTE_FORCE_ATTEMPT",
            "PRIVILEGE_ESCALATION", "DATA_EXFILTRATION", "MALICIOUS_ACTIVITY"
        );
        return suspiciousTypes.contains(eventType);
    }
    
    private boolean isHighRiskEvent(String eventType) {
        Set<String> highRiskTypes = Set.of(
            "UNAUTHORIZED_ACCESS", "PRIVILEGE_ESCALATION", "DATA_EXFILTRATION",
            "SYSTEM_COMPROMISE", "ADMIN_PRIVILEGE_ABUSE"
        );
        return highRiskTypes.contains(eventType);
    }
    
    private void generateSecurityAlert(String eventType, long count) {
        String alertMessage = String.format(
            "安全告警: 事件类型 '%s' 在短时间内发生 %d 次", 
            eventType, count);
        log.warn(alertMessage);
        
        // 实际应用中应该发送邮件、短信或其他通知
        // notificationService.sendSecurityAlert(alertMessage);
    }
    
    private List<String> detectAnomalousPatterns(LocalDateTime since) {
        List<String> patterns = new ArrayList<>();
        
        try {
            // 检测高频失败登录
            Map<String, Long> failedLoginByIp = auditLogs.stream()
                .filter(log -> log.getTimestamp().isAfter(since))
                .filter(log -> "FAILED_LOGIN".equals(log.getEventType()))
                .filter(log -> log.getIpAddress() != null)
                .collect(java.util.stream.Collectors.groupingBy(
                    AuditLog::getIpAddress, 
                    java.util.stream.Collectors.counting()));
            
            failedLoginByIp.entrySet().stream()
                .filter(entry -> entry.getValue() > 20)
                .forEach(entry -> patterns.add(
                    String.format("IP %s 高频失败登录: %d 次", 
                        entry.getKey(), entry.getValue())));
            
            // 检测异常时间活动
            long nightActivity = auditLogs.stream()
                .filter(log -> log.getTimestamp().isAfter(since))
                .filter(log -> {
                    int hour = log.getTimestamp().getHour();
                    return hour >= 22 || hour <= 6;
                })
                .count();
                
            if (nightActivity > 100) {
                patterns.add(String.format("夜间异常活动: %d 次操作", nightActivity));
            }
            
            // 检测权限提升尝试
            long privilegeAttempts = auditLogs.stream()
                .filter(log -> log.getTimestamp().isAfter(since))
                .filter(log -> "PRIVILEGE_ESCALATION".equals(log.getEventType()))
                .count();
                
            if (privilegeAttempts > 0) {
                patterns.add(String.format("检测到 %d 次权限提升尝试", privilegeAttempts));
            }
            
        } catch (Exception e) {
            log.error("检测异常模式失败", e);
        }
        
        return patterns;
    }
    
    private List<String> generateRecommendations(List<String> patterns) {
        List<String> recommendations = new ArrayList<>();
        
        for (String pattern : patterns) {
            if (pattern.contains("高频失败登录")) {
                recommendations.add("建议对该IP地址进行临时封禁");
                recommendations.add("考虑实施更强的身份验证机制");
            } else if (pattern.contains("夜间异常活动")) {
                recommendations.add("审查夜间活动策略");
                recommendations.add("考虑实施时间访问控制");
            } else if (pattern.contains("权限提升尝试")) {
                recommendations.add("立即审查相关账户权限");
                recommendations.add("加强权限审计和监控");
            }
        }
        
        if (recommendations.isEmpty()) {
            recommendations.add("当前未检测到明显安全威胁");
            recommendations.add("继续保持现有安全措施");
        }
        
        return recommendations;
    }
    
    private String generateCsvExport(List<AuditLog> logs) {
        StringBuilder csv = new StringBuilder();
        
        // CSV头部
        csv.append("ID,时间戳,事件类型,用户ID,IP地址,资源,详情\n");
        
        // CSV数据行
        for (AuditLog log : logs) {
            csv.append(String.format("%s,%s,%s,%s,%s,%s,\"%s\"\n",
                log.getId(),
                log.getTimestamp().toString(),
                log.getEventType(),
                log.getUserId() != null ? log.getUserId() : "",
                log.getIpAddress() != null ? log.getIpAddress() : "",
                log.getResource() != null ? log.getResource() : "",
                log.getDetails() != null ? log.getDetails().toString() : ""));
        }
        
        return csv.toString();
    }
    
    // ==================== 内部类 ====================
    
    @Data
    public static class AuditLog {
        private String id;
        private String eventType;
        private String userId;
        private String ipAddress;
        private String resource;
        private Map<String, Object> details;
        private LocalDateTime timestamp;
    }
    
    @Data
    public static class EventCounter {
        private String eventType;
        private long count;
        private LocalDateTime lastOccurrence;
        
        public EventCounter(String eventType) {
            this.eventType = eventType;
            this.count = 0;
        }
        
        public void increment() {
            this.count++;
            this.lastOccurrence = LocalDateTime.now();
        }
    }
    
    @Data
    public static class SecurityReport {
        private LocalDateTime generatedAt;
        private int periodDays;
        private long totalEvents;
        private long uniqueUsers;
        private long uniqueIps;
        private List<AuditLog> highRiskEvents;
        private List<String> anomalousPatterns;
        private List<String> recommendations;
        private String error;
    }
}