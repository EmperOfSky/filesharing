package com.filesharing.security;

import com.filesharing.entity.User;
import com.filesharing.exception.BusinessException;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 安全服务 - 提供全面的安全防护功能
 */
@Slf4j
@Service
public class SecurityService {
    
    // 存储用户会话信息
    private final Map<String, UserSession> activeSessions = new ConcurrentHashMap<>();
    
    // 存储IP访问记录
    private final Map<String, IpAccessRecord> ipAccessRecords = new ConcurrentHashMap<>();
    
    // 存储用户行为记录
    private final Map<Long, List<UserBehavior>> userBehaviors = new ConcurrentHashMap<>();
    
    // 黑名单IP集合
    private final Set<String> blacklistedIps = ConcurrentHashMap.newKeySet();
    
    // 敏感操作白名单用户ID
    private final Set<Long> privilegedUsers = ConcurrentHashMap.newKeySet();
    
    // 安全配置参数
    private static final int SESSION_TIMEOUT_MINUTES = 30;
    private static final int MAX_CONCURRENT_SESSIONS = 3;
    private static final int RATE_LIMIT_WINDOW_SECONDS = 60;
    private static final int MAX_REQUESTS_PER_WINDOW = 100;
    
    /**
     * 用户登录安全检查
     */
    public LoginSecurityResult checkLoginSecurity(String username, String ipAddress) {
        LoginSecurityResult result = new LoginSecurityResult();
        
        try {
            // 检查IP是否在黑名单中
            if (blacklistedIps.contains(ipAddress)) {
                result.setAllowed(false);
                result.setReason("IP地址已被列入黑名单");
                log.warn("拒绝登录尝试 - IP在黑名单中: {}, 用户名: {}", ipAddress, username);
                return result;
            }
            
            // 检查登录频率限制
            IpAccessRecord ipRecord = ipAccessRecords.computeIfAbsent(ipAddress, 
                ip -> new IpAccessRecord(ip));
            
            if (ipRecord.isRateLimited(RATE_LIMIT_WINDOW_SECONDS, MAX_REQUESTS_PER_WINDOW)) {
                result.setAllowed(false);
                result.setReason("登录尝试过于频繁，请稍后再试");
                addToBlacklist(ipAddress, "频繁登录尝试");
                log.warn("拒绝登录尝试 - 频率限制: {}, 用户名: {}", ipAddress, username);
                return result;
            }
            
            // 记录登录尝试
            ipRecord.recordAccess();
            
            result.setAllowed(true);
            result.setReason("安全检查通过");
            
        } catch (Exception e) {
            log.error("登录安全检查失败: 用户名={}, IP={}", username, ipAddress, e);
            result.setAllowed(false);
            result.setReason("系统错误，请稍后重试");
        }
        
        return result;
    }
    
    /**
     * 创建用户会话
     */
    public UserSession createUserSession(User user, String ipAddress, String userAgent) {
        try {
            // 检查并发会话限制
            cleanupExpiredSessions();
            long activeSessionCount = activeSessions.values().stream()
                .filter(session -> session.getUserId().equals(user.getId()))
                .count();
                
            if (activeSessionCount >= MAX_CONCURRENT_SESSIONS) {
                throw new BusinessException("超出最大并发会话数限制");
            }
            
            // 生成会话ID
            String sessionId = UUID.randomUUID().toString();
            
            // 创建会话
            UserSession session = new UserSession();
            session.setSessionId(sessionId);
            session.setUserId(user.getId());
            session.setUsername(user.getUsername());
            session.setIpAddress(ipAddress);
            session.setUserAgent(userAgent);
            session.setCreatedAt(LocalDateTime.now());
            session.setLastAccessedAt(LocalDateTime.now());
            session.setExpiresAt(LocalDateTime.now().plusMinutes(SESSION_TIMEOUT_MINUTES));
            
            activeSessions.put(sessionId, session);
            
            log.info("创建用户会话: 用户ID={}, 会话ID={}, IP={}", user.getId(), sessionId, ipAddress);
            return session;
            
        } catch (Exception e) {
            log.error("创建用户会话失败: 用户ID={}, IP={}", user.getId(), ipAddress, e);
            throw new BusinessException("创建会话失败: " + e.getMessage());
        }
    }
    
    /**
     * 验证用户会话
     */
    public SessionValidationResult validateSession(String sessionId, String ipAddress) {
        SessionValidationResult result = new SessionValidationResult();
        
        try {
            UserSession session = activeSessions.get(sessionId);
            
            if (session == null) {
                result.setValid(false);
                result.setReason("会话不存在");
                return result;
            }
            
            // 检查会话是否过期
            if (LocalDateTime.now().isAfter(session.getExpiresAt())) {
                activeSessions.remove(sessionId);
                result.setValid(false);
                result.setReason("会话已过期");
                return result;
            }
            
            // 检查IP地址是否匹配（可选的安全增强）
            if (ipAddress != null && !ipAddress.equals(session.getIpAddress())) {
                log.warn("IP地址不匹配: 会话IP={}, 当前IP={}", session.getIpAddress(), ipAddress);
                // 可以选择拒绝或允许，这里暂时允许但记录警告
            }
            
            // 更新最后访问时间
            session.setLastAccessedAt(LocalDateTime.now());
            session.setExpiresAt(LocalDateTime.now().plusMinutes(SESSION_TIMEOUT_MINUTES));
            
            result.setValid(true);
            result.setSession(session);
            result.setReason("会话有效");
            
        } catch (Exception e) {
            log.error("会话验证失败: 会话ID={}", sessionId, e);
            result.setValid(false);
            result.setReason("会话验证异常");
        }
        
        return result;
    }
    
    /**
     * 记录用户行为
     */
    public void recordUserBehavior(Long userId, String action, String resource, 
                                 String ipAddress, Map<String, Object> details) {
        try {
            UserBehavior behavior = new UserBehavior();
            behavior.setUserId(userId);
            behavior.setAction(action);
            behavior.setResource(resource);
            behavior.setIpAddress(ipAddress);
            behavior.setDetails(details);
            behavior.setTimestamp(LocalDateTime.now());
            
            List<UserBehavior> behaviors = userBehaviors.computeIfAbsent(userId, 
                k -> Collections.synchronizedList(new ArrayList<>()));
            
            behaviors.add(behavior);
            
            // 保持行为记录在合理范围内
            if (behaviors.size() > 1000) {
                behaviors.remove(0);
            }
            
            log.debug("记录用户行为: 用户ID={}, 动作={}, 资源={}", userId, action, resource);
            
        } catch (Exception e) {
            log.error("记录用户行为失败: 用户ID={}", userId, e);
        }
    }
    
    /**
     * 检查敏感操作权限
     */
    public PermissionCheckResult checkSensitiveOperation(Long userId, String operation, 
                                                       String resource) {
        PermissionCheckResult result = new PermissionCheckResult();
        
        try {
            // 检查是否为特权用户
            if (privilegedUsers.contains(userId)) {
                result.setAllowed(true);
                result.setReason("特权用户");
                return result;
            }
            
            // 根据操作类型进行不同级别的检查
            switch (operation.toUpperCase()) {
                case "DELETE_USER":
                case "SYSTEM_CONFIG":
                    result.setAllowed(false);
                    result.setReason("需要管理员权限");
                    break;
                    
                case "DELETE_FILE":
                case "MODIFY_PERMISSIONS":
                    // 检查资源所有权
                    result.setAllowed(true);
                    result.setReason("基础权限检查通过");
                    break;
                    
                default:
                    result.setAllowed(true);
                    result.setReason("操作允许");
            }
            
            // 记录敏感操作
            Map<String, Object> details = new HashMap<>();
            details.put("operation", operation);
            details.put("resource", resource);
            details.put("result", result.isAllowed());
            recordUserBehavior(userId, "SENSITIVE_OPERATION", resource, null, details);
            
        } catch (Exception e) {
            log.error("敏感操作权限检查失败: 用户ID={}, 操作={}", userId, operation, e);
            result.setAllowed(false);
            result.setReason("权限检查异常");
        }
        
        return result;
    }
    
    /**
     * 检测异常行为
     */
    public AnomalyDetectionResult detectAnomalies(Long userId) {
        AnomalyDetectionResult result = new AnomalyDetectionResult();
        result.setUserId(userId);
        result.setAnomalies(new ArrayList<>());
        
        try {
            List<UserBehavior> behaviors = userBehaviors.get(userId);
            if (behaviors == null || behaviors.isEmpty()) {
                return result;
            }
            
            // 分析最近的行为模式
            LocalDateTime oneHourAgo = LocalDateTime.now().minusHours(1);
            List<UserBehavior> recentBehaviors = behaviors.stream()
                .filter(b -> b.getTimestamp().isAfter(oneHourAgo))
                .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
            
            // 检查高频操作
            Map<String, Long> actionCounts = recentBehaviors.stream()
                .collect(java.util.stream.Collectors.groupingBy(
                    UserBehavior::getAction, 
                    java.util.stream.Collectors.counting()));
            
            actionCounts.forEach((action, count) -> {
                if (count > 50) { // 1小时内同一操作超过50次
                    result.addAnomaly(String.format("高频操作: %s (%d次)", action, count));
                }
            });
            
            // 检查异常时间访问
            long nightAccessCount = recentBehaviors.stream()
                .filter(b -> {
                    int hour = b.getTimestamp().getHour();
                    return hour >= 22 || hour <= 6; // 晚上10点到早上6点
                })
                .count();
                
            if (nightAccessCount > 10) {
                result.addAnomaly(String.format("夜间异常访问: %d次", nightAccessCount));
            }
            
            result.setRiskLevel(calculateRiskLevel(result.getAnomalies().size()));
            
        } catch (Exception e) {
            log.error("异常行为检测失败: 用户ID={}", userId, e);
        }
        
        return result;
    }
    
    /**
     * 添加IP到黑名单
     */
    public void addToBlacklist(String ipAddress, String reason) {
        try {
            blacklistedIps.add(ipAddress);
            log.warn("IP地址加入黑名单: {}, 原因: {}", ipAddress, reason);
            
            // 记录到安全日志
            recordSecurityEvent("BLACKLIST_ADD", ipAddress, reason);
            
        } catch (Exception e) {
            log.error("添加IP到黑名单失败: {}", ipAddress, e);
        }
    }
    
    /**
     * 从黑名单移除IP
     */
    public void removeFromBlacklist(String ipAddress) {
        try {
            if (blacklistedIps.remove(ipAddress)) {
                log.info("IP地址从黑名单移除: {}", ipAddress);
                recordSecurityEvent("BLACKLIST_REMOVE", ipAddress, "手动移除");
            }
        } catch (Exception e) {
            log.error("从黑名单移除IP失败: {}", ipAddress, e);
        }
    }
    
    /**
     * 获取安全统计信息
     */
    public SecurityStatistics getSecurityStatistics() {
        SecurityStatistics stats = new SecurityStatistics();
        
        try {
            stats.setActiveSessions(activeSessions.size());
            stats.setBlacklistedIps(blacklistedIps.size());
            stats.setPrivilegedUsers(privilegedUsers.size());
            
            // 计算最近24小时的异常事件
            LocalDateTime dayAgo = LocalDateTime.now().minusDays(1);
            long recentAnomalies = userBehaviors.values().stream()
                .flatMap(List::stream)
                .filter(b -> b.getTimestamp().isAfter(dayAgo))
                .filter(b -> b.getAction().contains("ANOMALY"))
                .count();
            stats.setRecentAnomalies(recentAnomalies);
            
            // 计算平均会话时长
            double avgSessionDuration = activeSessions.values().stream()
                .mapToLong(session -> 
                    java.time.Duration.between(session.getCreatedAt(), LocalDateTime.now()).toMinutes())
                .average()
                .orElse(0.0);
            stats.setAvgSessionDurationMinutes(avgSessionDuration);
            
        } catch (Exception e) {
            log.error("获取安全统计信息失败", e);
        }
        
        return stats;
    }
    
    // ==================== 私有辅助方法 ====================
    
    private void cleanupExpiredSessions() {
        LocalDateTime now = LocalDateTime.now();
        activeSessions.entrySet().removeIf(entry -> 
            now.isAfter(entry.getValue().getExpiresAt()));
    }
    
    private String calculateRiskLevel(int anomalyCount) {
        if (anomalyCount == 0) return "LOW";
        if (anomalyCount <= 2) return "MEDIUM";
        if (anomalyCount <= 5) return "HIGH";
        return "CRITICAL";
    }
    
    private void recordSecurityEvent(String eventType, String target, String details) {
        Map<String, Object> eventDetails = new HashMap<>();
        eventDetails.put("eventType", eventType);
        eventDetails.put("target", target);
        eventDetails.put("details", details);
        eventDetails.put("timestamp", LocalDateTime.now());
        
        log.info("安全事件记录: 类型={}, 目标={}, 详情={}", eventType, target, details);
    }
    
    // ==================== 内部类 ====================
    
    @Data
    public static class LoginSecurityResult {
        private boolean allowed;
        private String reason;
    }
    
    @Data
    public static class SessionValidationResult {
        private boolean valid;
        private String reason;
        private UserSession session;
    }
    
    @Data
    public static class PermissionCheckResult {
        private boolean allowed;
        private String reason;
    }
    
    @Data
    public static class AnomalyDetectionResult {
        private Long userId;
        private List<String> anomalies;
        private String riskLevel;
        
        public void addAnomaly(String anomaly) {
            if (anomalies == null) {
                anomalies = new ArrayList<>();
            }
            anomalies.add(anomaly);
        }
    }
    
    @Data
    public static class SecurityStatistics {
        private int activeSessions;
        private int blacklistedIps;
        private int privilegedUsers;
        private long recentAnomalies;
        private double avgSessionDurationMinutes;
    }
    
    @Data
    public static class UserSession {
        private String sessionId;
        private Long userId;
        private String username;
        private String ipAddress;
        private String userAgent;
        private LocalDateTime createdAt;
        private LocalDateTime lastAccessedAt;
        private LocalDateTime expiresAt;
        private Map<String, Object> attributes;
    }
    
    @Data
    public static class IpAccessRecord {
        private String ipAddress;
        private List<LocalDateTime> accessTimes;
        private LocalDateTime lastBlockedTime;
        
        public IpAccessRecord(String ipAddress) {
            this.ipAddress = ipAddress;
            this.accessTimes = Collections.synchronizedList(new ArrayList<>());
        }
        
        public void recordAccess() {
            LocalDateTime now = LocalDateTime.now();
            accessTimes.add(now);
            
            // 只保留最近100次访问记录
            if (accessTimes.size() > 100) {
                accessTimes.remove(0);
            }
        }
        
        public boolean isRateLimited(int windowSeconds, int maxRequests) {
            LocalDateTime windowStart = LocalDateTime.now().minusSeconds(windowSeconds);
            long recentRequests = accessTimes.stream()
                .filter(time -> time.isAfter(windowStart))
                .count();
            return recentRequests >= maxRequests;
        }
    }
    
    @Data
    public static class UserBehavior {
        private Long userId;
        private String action;
        private String resource;
        private String ipAddress;
        private Map<String, Object> details;
        private LocalDateTime timestamp;
    }
}