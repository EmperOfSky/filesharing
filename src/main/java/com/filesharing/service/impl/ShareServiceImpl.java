package com.filesharing.service.impl;

import com.filesharing.dto.ShareCreateRequest;
import com.filesharing.dto.ShareResponse;
import com.filesharing.entity.FileEntity;
import com.filesharing.entity.Folder;
import com.filesharing.entity.ShareClickLog;
import com.filesharing.entity.ShareRecord;
import com.filesharing.entity.User;
import com.filesharing.exception.BusinessException;
import com.filesharing.repository.FileRepository;
import com.filesharing.repository.FolderRepository;
import com.filesharing.repository.ShareClickLogRepository;
import com.filesharing.repository.ShareRepository;
import com.filesharing.service.ShareService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 分享服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class ShareServiceImpl implements ShareService {

    private static final String KEY_CHARS = "23456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz";
    private static final int KEY_LENGTH = 8;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    
    private final ShareRepository shareRepository;
    private final ShareClickLogRepository shareClickLogRepository;
    private final FileRepository fileRepository;
    private final FolderRepository folderRepository;
    private final PasswordEncoder passwordEncoder;
    
    @Override
    public ShareResponse createShare(ShareCreateRequest request, User sharer) {
        if (sharer == null) {
            throw new BusinessException("用户未登录");
        }

        if (request.getExpireTime() != null && request.getExpireTime().isBefore(LocalDateTime.now())) {
            throw new BusinessException("过期时间不能早于当前时间");
        }

        int maxAccessCount = request.getMaxAccessCount() == null ? 0 : request.getMaxAccessCount();
        if (maxAccessCount < 0) {
            throw new BusinessException("最大访问次数不能小于0");
        }

        ShareRecord share = new ShareRecord();
        share.setShareKey(generateUniqueShareKey());
        share.setTitle(request.getTitle());
        share.setDescription(request.getDescription());
        share.setSharer(sharer);
        share.setMaxAccessCount(maxAccessCount);
        share.setCurrentAccessCount(0);
        share.setAllowDownload(request.getAllowDownload() != null ? request.getAllowDownload() : true);
        share.setStatus(ShareRecord.ShareStatus.ACTIVE);
        share.setCreatedAt(LocalDateTime.now());
        share.setUpdatedAt(LocalDateTime.now());
        
        // 设置过期时间
        if (request.getExpireTime() != null) {
            share.setExpireTime(request.getExpireTime());
        }
        
        // 设置访问密码
        if (request.getPassword() != null && !request.getPassword().isBlank()) {
            share.setPassword(passwordEncoder.encode(request.getPassword()));
        }
        
        // 根据分享类型设置关联对象
        String normalizedShareType = normalizeShareType(request.getShareType());
        if ("FILE".equals(normalizedShareType) && request.getContentId() != null) {
            FileEntity file = fileRepository.findById(request.getContentId())
                    .orElseThrow(() -> new BusinessException("文件不存在"));

            if (file.getUploader() == null || !file.getUploader().getId().equals(sharer.getId())) {
                throw new BusinessException("无权分享该文件");
            }

            share.setFile(file);
            share.setShareType(ShareRecord.ShareType.FILE);
        } else if ("FOLDER".equals(normalizedShareType) && request.getContentId() != null) {
            Folder folder = folderRepository.findById(request.getContentId())
                    .orElseThrow(() -> new BusinessException("文件夹不存在"));

            if (folder.getOwner() == null || !folder.getOwner().getId().equals(sharer.getId())) {
                throw new BusinessException("无权分享该文件夹");
            }

            share.setFolder(folder);
            share.setShareType(ShareRecord.ShareType.FOLDER);
        } else {
            throw new BusinessException("无效的分享类型或缺少必要参数");
        }
        
        ShareRecord savedShare = shareRepository.save(share);
        log.info("创建分享：分享 ID={}, 分享者={}", savedShare.getId(), sharer.getUsername());
        
        return convertToShareResponse(savedShare);
    }
    
    @Override
    @Transactional(readOnly = true)
    public ShareResponse getShareByShareKey(String shareKey) {
        return getPublicShareInfo(shareKey);
    }

    @Override
    @Transactional(readOnly = true)
    public ShareResponse getPublicShareInfo(String shareKey) {
        ShareRecord share = findShareByKey(shareKey);
        validateShareState(share, false);
        return convertToShareResponse(share);
    }

    @Override
    public ShareResponse getPublicShareInfoWithTracking(String shareKey,
                                                        String ipAddress,
                                                        String address,
                                                        String userAgent,
                                                        String referer) {
        ShareRecord share = findShareByKey(shareKey);
        validateShareState(share, false);

        ShareRecord saved = trackShareClick(share, ipAddress, address, userAgent, referer);
        return convertToShareResponse(saved);
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<ShareResponse> getUserShares(User user, int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.max(size, 1);

        return shareRepository
                .findBySharer(user, PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.DESC, "createdAt")))
                .getContent()
                .stream()
                .map(this::convertToShareResponse)
                .toList();
    }
    
    @Override
    public ShareResponse accessShare(String shareKey, String password, String ipAddress) {
        ShareRecord share = findShareByKey(shareKey);
        validateShareState(share, true);

        if (!validateShareAccess(share, password)) {
            throw new BusinessException("分享密码错误");
        }

        int current = share.getCurrentAccessCount() == null ? 0 : share.getCurrentAccessCount();
        share.setCurrentAccessCount(current + 1);
        share.setUpdatedAt(LocalDateTime.now());
        ShareRecord saved = shareRepository.save(share);

        log.info("访问分享：shareKey={}, IP={}", shareKey, ipAddress);
        return convertToShareResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public FileEntity resolveShareFileForDownload(String shareKey, String password) {
        ShareRecord share = findShareByKey(shareKey);
        validateShareState(share, false);

        if (!Boolean.TRUE.equals(share.getAllowDownload())) {
            throw new BusinessException("该分享不允许下载");
        }

        if (password != null && !password.isBlank() && !validateShareAccess(share, password)) {
            throw new BusinessException("分享密码错误");
        }

        if (share.getShareType() != ShareRecord.ShareType.FILE || share.getFile() == null) {
            throw new BusinessException("当前分享不支持文件下载");
        }

        return share.getFile();
    }
    
    @Override
    public void deleteShare(Long shareId, User currentUser) {
        ShareRecord share = getShareById(shareId);
        ensureShareOwner(share, currentUser);
        shareRepository.delete(share);
        log.info("删除分享：分享 ID={}, 用户={}", shareId, currentUser.getUsername());
    }
    
    @Override
    public void disableShare(Long shareId, User currentUser) {
        ShareRecord share = getShareById(shareId);
        ensureShareOwner(share, currentUser);
        share.setStatus(ShareRecord.ShareStatus.DISABLED);
        share.setUpdatedAt(LocalDateTime.now());
        shareRepository.save(share);
        log.info("禁用分享：分享 ID={}, 用户={}", shareId, currentUser.getUsername());
    }
    
    @Override
    public void enableShare(Long shareId, User currentUser) {
        ShareRecord share = getShareById(shareId);
        ensureShareOwner(share, currentUser);
        if (share.getExpireTime() != null && share.getExpireTime().isBefore(LocalDateTime.now())) {
            throw new BusinessException("分享已过期，无法启用");
        }
        share.setStatus(ShareRecord.ShareStatus.ACTIVE);
        share.setUpdatedAt(LocalDateTime.now());
        shareRepository.save(share);
        log.info("启用分享：分享 ID={}, 用户={}", shareId, currentUser.getUsername());
    }
    
    @Override
    public ShareResponse updateShare(Long shareId, ShareCreateRequest request, User currentUser) {
        ShareRecord share = getShareById(shareId);
        ensureShareOwner(share, currentUser);

        if (request.getTitle() != null) {
            share.setTitle(request.getTitle());
        }
        if (request.getDescription() != null) {
            share.setDescription(request.getDescription());
        }
        if (request.getExpireTime() != null) {
            if (request.getExpireTime().isBefore(LocalDateTime.now())) {
                throw new BusinessException("过期时间不能早于当前时间");
            }
            share.setExpireTime(request.getExpireTime());
        }
        if (request.getMaxAccessCount() != null) {
            if (request.getMaxAccessCount() < 0) {
                throw new BusinessException("最大访问次数不能小于0");
            }
            share.setMaxAccessCount(request.getMaxAccessCount());
        }
        if (request.getAllowDownload() != null) {
            share.setAllowDownload(request.getAllowDownload());
        }
        if (request.getPassword() != null) {
            if (request.getPassword().isBlank()) {
                share.setPassword(null);
            } else {
                share.setPassword(passwordEncoder.encode(request.getPassword()));
            }
        }

        share.setUpdatedAt(LocalDateTime.now());
        ShareRecord saved = shareRepository.save(share);
        log.info("更新分享：分享 ID={}, 用户={}", shareId, currentUser.getUsername());
        return convertToShareResponse(saved);
    }
    
    @Override
    public ShareStatistics getShareStatistics(User user) {
        Long totalShares = shareRepository.countBySharer(user);
        Long activeShares = shareRepository.findBySharerAndStatus(user, ShareRecord.ShareStatus.ACTIVE,
                        PageRequest.of(0, 1))
                .getTotalElements();

        long expiredShares = totalShares - activeShares;
        long totalAccessCount = shareRepository
            .findBySharer(user, PageRequest.of(0, 5000))
                .getContent()
                .stream()
                .mapToLong(s -> s.getCurrentAccessCount() == null ? 0 : s.getCurrentAccessCount())
                .sum();

        return new ShareStatistics(totalShares, activeShares, expiredShares, totalAccessCount);
    }

        @Override
        @Transactional(readOnly = true)
        public Map<String, Object> getShareMonitoringDetails(Long shareId, User currentUser, int limit) {
        ShareRecord share = getShareById(shareId);
        ensureShareOwner(share, currentUser);

        int safeLimit = Math.max(1, Math.min(limit, 100));
        List<ShareClickLog> logs = shareClickLogRepository.findByShare_IdOrderByAccessedAtDesc(shareId);

        long pv = logs.size();
        long uv = logs.stream()
            .map(ShareClickLog::getVisitorFingerprint)
            .filter(Objects::nonNull)
            .filter(value -> !value.isBlank())
            .distinct()
            .count();
        long uip = logs.stream()
            .map(ShareClickLog::getVisitorIp)
            .filter(Objects::nonNull)
            .filter(value -> !value.isBlank())
            .distinct()
            .count();

        List<Map<String, Object>> recentVisits = logs.stream()
            .limit(safeLimit)
            .map(this::buildRecentVisitItem)
            .toList();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("shareId", share.getId());
        result.put("shareKey", share.getShareKey());
        result.put("title", share.getTitle());
        result.put("status", share.getStatus() == null ? null : share.getStatus().name());
        result.put("pv", pv);
        result.put("uv", uv);
        result.put("uip", uip);
        result.put("accessCount", share.getCurrentAccessCount() == null ? 0 : share.getCurrentAccessCount());
        result.put("lastAccessAt", share.getLastAccessAt() == null ? null : share.getLastAccessAt().toString());
        result.put("lastVisitorIp", share.getLastVisitorIp());
        result.put("lastVisitorAddress", share.getLastVisitorAddress());
        result.put("dailyTrend", buildDailyTrend(logs, 7));
        result.put("topIps", buildTopDimension(logs, ShareClickLog::getVisitorIp, 5));
        result.put("topAddresses", buildTopDimension(logs, ShareClickLog::getVisitorAddress, 5));
        result.put("browserStats", buildTopDimension(logs, item -> parseBrowser(item.getUserAgent()), 6));
        result.put("osStats", buildTopDimension(logs, item -> parseOs(item.getUserAgent()), 6));
        result.put("deviceStats", buildTopDimension(logs, item -> parseDevice(item.getUserAgent()), 4));
        result.put("refererStats", buildTopDimension(logs, ShareClickLog::getReferer, 6));
        result.put("recentVisits", recentVisits);

        return result;
        }
    
    @Override
    public void cleanupExpiredShares() {
        List<ShareRecord> expiredShares = shareRepository.findExpiredShares(LocalDateTime.now());
        for (ShareRecord share : expiredShares) {
            share.setStatus(ShareRecord.ShareStatus.EXPIRED);
            share.setUpdatedAt(LocalDateTime.now());
        }
        if (!expiredShares.isEmpty()) {
            shareRepository.saveAll(expiredShares);
        }
        log.info("清理过期分享");
    }
    
    @Override
    public ShareRecord getShareEntityById(Long shareId) {
        return getShareById(shareId);
    }
    
    @Override
    public boolean validateShareAccess(ShareRecord share, String password) {
        String storedPassword = share.getPassword();
        if (storedPassword == null || storedPassword.isBlank()) {
            return true;
        }
        if (password == null || password.isBlank()) {
            return false;
        }

        if (isBcryptHash(storedPassword)) {
            return passwordEncoder.matches(password, storedPassword);
        }
        return storedPassword.equals(password);
    }
    
    // ==================== 私有方法 ====================
    
    private ShareRecord getShareById(Long shareId) {
        return shareRepository.findById(shareId)
                .orElseThrow(() -> new BusinessException("分享记录不存在"));
    }

    private ShareRecord findShareByKey(String shareKey) {
        return shareRepository.findByShareKey(shareKey)
                .orElseThrow(() -> new BusinessException("分享不存在或已失效"));
    }

    private void validateShareState(ShareRecord share, boolean mutable) {
        if (share.getStatus() == ShareRecord.ShareStatus.DISABLED) {
            throw new BusinessException("分享已禁用");
        }

        if (share.getExpireTime() != null && share.getExpireTime().isBefore(LocalDateTime.now())) {
            if (mutable) {
                share.setStatus(ShareRecord.ShareStatus.EXPIRED);
                share.setUpdatedAt(LocalDateTime.now());
                shareRepository.save(share);
            }
            throw new BusinessException("分享已过期");
        }

        int maxAccessCount = share.getMaxAccessCount() == null ? 0 : share.getMaxAccessCount();
        int currentAccessCount = share.getCurrentAccessCount() == null ? 0 : share.getCurrentAccessCount();
        if (maxAccessCount > 0 && currentAccessCount >= maxAccessCount) {
            if (mutable) {
                share.setStatus(ShareRecord.ShareStatus.DISABLED);
                share.setUpdatedAt(LocalDateTime.now());
                shareRepository.save(share);
            }
            throw new BusinessException("分享访问次数已达上限");
        }
    }

    private void ensureShareOwner(ShareRecord share, User currentUser) {
        if (share.getSharer() == null || !share.getSharer().getId().equals(currentUser.getId())) {
            throw new BusinessException("无权操作该分享");
        }
    }

    private String normalizeShareType(String shareType) {
        if (shareType == null || shareType.isBlank()) {
            throw new BusinessException("分享类型不能为空");
        }
        return shareType.trim().toUpperCase(Locale.ROOT);
    }

    private String generateUniqueShareKey() {
        for (int i = 0; i < 10; i++) {
            String key = randomShareKey();
            if (shareRepository.findByShareKey(key).isEmpty()) {
                return key;
            }
        }
        throw new BusinessException("生成分享短链失败，请稍后重试");
    }

    private String randomShareKey() {
        StringBuilder sb = new StringBuilder(KEY_LENGTH);
        for (int i = 0; i < KEY_LENGTH; i++) {
            int idx = SECURE_RANDOM.nextInt(KEY_CHARS.length());
            sb.append(KEY_CHARS.charAt(idx));
        }
        return sb.toString();
    }

    private boolean isBcryptHash(String raw) {
        return raw.startsWith("$2a$") || raw.startsWith("$2b$") || raw.startsWith("$2y$");
    }

    private ShareRecord trackShareClick(ShareRecord share,
                                        String ipAddress,
                                        String address,
                                        String userAgent,
                                        String referer) {
        String normalizedIp = normalizeText(ipAddress, 64, "unknown");
        String normalizedAddress = normalizeText(address, 255, "unknown");
        String normalizedAgent = normalizeText(userAgent, 500, "unknown");
        String normalizedReferer = normalizeText(referer, 255, "direct");

        ShareClickLog clickLog = new ShareClickLog();
        clickLog.setShare(share);
        clickLog.setShareKey(share.getShareKey());
        clickLog.setVisitorIp(normalizedIp);
        clickLog.setVisitorAddress(normalizedAddress);
        clickLog.setUserAgent(normalizedAgent);
        clickLog.setReferer(normalizedReferer);
        clickLog.setVisitorFingerprint(buildVisitorFingerprint(normalizedIp, normalizedAgent));
        shareClickLogRepository.save(clickLog);

        long pvCount = shareClickLogRepository.countByShare_Id(share.getId());
        long uvCount = shareClickLogRepository.countDistinctVisitorsByShareId(share.getId());

        share.setPvCount(safeInt(pvCount));
        share.setUvCount(safeInt(uvCount));
        share.setLastVisitorIp(normalizedIp);
        share.setLastVisitorAddress(normalizedAddress);
        share.setLastAccessAt(LocalDateTime.now());
        share.setUpdatedAt(LocalDateTime.now());
        return shareRepository.save(share);
    }

    private String normalizeText(String raw, int maxLen, String defaultValue) {
        if (raw == null || raw.isBlank()) {
            return defaultValue;
        }
        String trimmed = raw.trim();
        if (trimmed.length() <= maxLen) {
            return trimmed;
        }
        return trimmed.substring(0, maxLen);
    }

    private String buildVisitorFingerprint(String ipAddress, String userAgent) {
        String source = ipAddress + "|" + userAgent;
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(source.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(hashed.length * 2);
            for (byte b : hashed) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException ex) {
            log.warn("构建访客指纹失败，退化为原始标识");
            String fallback = source.replace('|', '_');
            return normalizeText(fallback, 64, "unknown");
        }
    }

    private int safeInt(long value) {
        if (value > Integer.MAX_VALUE) {
            return Integer.MAX_VALUE;
        }
        if (value < 0) {
            return 0;
        }
        return (int) value;
    }

    private Map<String, Object> buildRecentVisitItem(ShareClickLog clickLog) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("ip", clickLog.getVisitorIp());
        item.put("address", clickLog.getVisitorAddress());
        item.put("referer", clickLog.getReferer());
        item.put("browser", parseBrowser(clickLog.getUserAgent()));
        item.put("os", parseOs(clickLog.getUserAgent()));
        item.put("device", parseDevice(clickLog.getUserAgent()));
        item.put("userAgent", clickLog.getUserAgent());
        item.put("accessedAt", clickLog.getAccessedAt() == null ? null : clickLog.getAccessedAt().toString());
        return item;
    }

    private List<Map<String, Object>> buildDailyTrend(List<ShareClickLog> logs, int days) {
        LocalDate today = LocalDate.now();
        Map<LocalDate, Long> grouped = logs.stream()
                .filter(log -> log.getAccessedAt() != null)
                .collect(Collectors.groupingBy(log -> log.getAccessedAt().toLocalDate(), Collectors.counting()));

        List<Map<String, Object>> trend = new ArrayList<>();
        for (int i = days - 1; i >= 0; i--) {
            LocalDate date = today.minusDays(i);
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("date", date.toString());
            item.put("count", grouped.getOrDefault(date, 0L));
            trend.add(item);
        }
        return trend;
    }

    private List<Map<String, Object>> buildTopDimension(List<ShareClickLog> logs,
                                                         Function<ShareClickLog, String> extractor,
                                                         int topN) {
        return logs.stream()
                .map(extractor)
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .collect(Collectors.groupingBy(value -> value, Collectors.counting()))
                .entrySet()
                .stream()
                .sorted((a, b) -> Long.compare(b.getValue(), a.getValue()))
                .limit(topN)
                .map(entry -> {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("name", entry.getKey());
                    item.put("count", entry.getValue());
                    return item;
                })
                .toList();
    }

    private String parseBrowser(String userAgent) {
        String ua = userAgent == null ? "" : userAgent.toLowerCase(Locale.ROOT);
        if (ua.contains("micromessenger")) {
            return "WeChat";
        }
        if (ua.contains("edg/")) {
            return "Edge";
        }
        if (ua.contains("chrome/")) {
            return "Chrome";
        }
        if (ua.contains("firefox/")) {
            return "Firefox";
        }
        if (ua.contains("safari/") && !ua.contains("chrome/")) {
            return "Safari";
        }
        if (ua.contains("opera") || ua.contains("opr/")) {
            return "Opera";
        }
        return "Other";
    }

    private String parseOs(String userAgent) {
        String ua = userAgent == null ? "" : userAgent.toLowerCase(Locale.ROOT);
        if (ua.contains("windows")) {
            return "Windows";
        }
        if (ua.contains("android")) {
            return "Android";
        }
        if (ua.contains("iphone") || ua.contains("ipad") || ua.contains("ios")) {
            return "iOS";
        }
        if (ua.contains("mac os") || ua.contains("macintosh")) {
            return "macOS";
        }
        if (ua.contains("linux")) {
            return "Linux";
        }
        return "Other";
    }

    private String parseDevice(String userAgent) {
        String ua = userAgent == null ? "" : userAgent.toLowerCase(Locale.ROOT);
        if (ua.contains("ipad") || ua.contains("tablet")) {
            return "Tablet";
        }
        if (ua.contains("mobile") || ua.contains("android") || ua.contains("iphone")) {
            return "Mobile";
        }
        if (ua.isBlank() || "unknown".equals(ua)) {
            return "Unknown";
        }
        return "Desktop";
    }
    
    private ShareResponse convertToShareResponse(ShareRecord share) {
        ShareResponse response = new ShareResponse();
        response.setId(share.getId());
        response.setShareKey(share.getShareKey());
        response.setTitle(share.getTitle());
        response.setDescription(share.getDescription());
        response.setShareType(share.getShareType().name());
        response.setSharerName(share.getSharer().getUsername());
        response.setSharerId(share.getSharer().getId());
        
        if (share.getExpireTime() != null) {
            response.setExpireTime(share.getExpireTime().toString());
        }
        
        response.setMaxAccessCount(share.getMaxAccessCount());
        response.setCurrentAccessCount(share.getCurrentAccessCount());
        response.setPvCount(share.getPvCount());
        response.setUvCount(share.getUvCount());
        response.setLastVisitorIp(share.getLastVisitorIp());
        response.setLastVisitorAddress(share.getLastVisitorAddress());
        if (share.getLastAccessAt() != null) {
            response.setLastAccessAt(share.getLastAccessAt().toString());
        }
        response.setStatus(share.getStatus().name());
        response.setAllowDownload(share.getAllowDownload());
        response.setRequiresPassword(share.getPassword() != null && !share.getPassword().isBlank());
        response.setAccessUrl("/api/shares/public/" + share.getShareKey());
        response.setShortLink("/s/" + share.getShareKey());
        if (share.getShareType() == ShareRecord.ShareType.FILE) {
            response.setDownloadUrl("/api/shares/public/" + share.getShareKey() + "/download");
        }
        response.setSharedContent(buildSharedContent(share));
        response.setCreatedAt(share.getCreatedAt().toString());
        response.setUpdatedAt(share.getUpdatedAt().toString());
        
        return response;
    }

    private Object buildSharedContent(ShareRecord share) {
        Map<String, Object> content = new HashMap<>();
        if (share.getShareType() == ShareRecord.ShareType.FILE && share.getFile() != null) {
            content.put("id", share.getFile().getId());
            content.put("name", share.getFile().getOriginalName());
            content.put("size", share.getFile().getFileSize());
            content.put("contentType", share.getFile().getContentType());
            return content;
        }

        if (share.getShareType() == ShareRecord.ShareType.FOLDER && share.getFolder() != null) {
            content.put("id", share.getFolder().getId());
            content.put("name", share.getFolder().getName());
            content.put("path", share.getFolder().getFolderPath());
            return content;
        }

        content.put("id", null);
        content.put("name", "unknown");
        content.put("type", share.getShareType() == null ? "UNKNOWN" : share.getShareType().name());
        return content;
    }
}
