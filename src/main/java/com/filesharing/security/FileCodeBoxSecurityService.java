package com.filesharing.security;

import com.filesharing.config.FileCodeBoxProperties;
import com.filesharing.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * FileCodeBox 兼容接口的 IP 频率限制与错误计数。
 */
@Service
@RequiredArgsConstructor
public class FileCodeBoxSecurityService {

    private final FileCodeBoxProperties properties;

    private final Map<String, CounterWindow> uploadCounters = new ConcurrentHashMap<>();
    private final Map<String, CounterWindow> errorCounters = new ConcurrentHashMap<>();

    public void ensureUploadAllowed(String ip) {
        String key = normalizeIp(ip);
        CounterWindow window = uploadCounters.computeIfAbsent(key, k -> new CounterWindow());
        int count = window.countWithin(properties.getUploadMinute());
        if (count >= properties.getUploadCount()) {
            throw new BusinessException("UPLOAD_RATE_LIMITED", "上传过于频繁，请稍后重试");
        }
    }

    public void markUpload(String ip) {
        String key = normalizeIp(ip);
        uploadCounters.computeIfAbsent(key, k -> new CounterWindow()).addNow();
    }

    public void ensureSelectAllowed(String ip) {
        String key = normalizeIp(ip);
        CounterWindow window = errorCounters.computeIfAbsent(key, k -> new CounterWindow());
        int count = window.countWithin(properties.getErrorMinute());
        if (count >= properties.getErrorCount()) {
            throw new BusinessException("ACCESS_RATE_LIMITED", "取件码尝试过于频繁，请稍后重试");
        }
    }

    public void markSelectError(String ip) {
        String key = normalizeIp(ip);
        errorCounters.computeIfAbsent(key, k -> new CounterWindow()).addNow();
    }

    public void clearSelectError(String ip) {
        String key = normalizeIp(ip);
        errorCounters.remove(key);
    }

    public void cleanup() {
        long now = Instant.now().toEpochMilli();
        pruneMap(uploadCounters, now, properties.getUploadMinute());
        pruneMap(errorCounters, now, properties.getErrorMinute());
    }

    private void pruneMap(Map<String, CounterWindow> map, long now, int keepMinutes) {
        long keepMillis = keepMinutes * 60_000L;
        map.entrySet().removeIf(entry -> entry.getValue().isStale(now, keepMillis));
    }

    private String normalizeIp(String ip) {
        if (ip == null || ip.isBlank()) {
            return "unknown";
        }
        return ip.trim();
    }

    private static class CounterWindow {
        private final ConcurrentLinkedDeque<Long> timestamps = new ConcurrentLinkedDeque<>();

        void addNow() {
            timestamps.addLast(Instant.now().toEpochMilli());
        }

        int countWithin(int minutes) {
            long now = Instant.now().toEpochMilli();
            long cutoff = now - minutes * 60_000L;
            while (true) {
                Long first = timestamps.peekFirst();
                if (first == null || first >= cutoff) {
                    break;
                }
                timestamps.pollFirst();
            }
            return timestamps.size();
        }

        boolean isStale(long now, long keepMillis) {
            Long last = timestamps.peekLast();
            return last == null || (now - last) > keepMillis;
        }
    }
}
