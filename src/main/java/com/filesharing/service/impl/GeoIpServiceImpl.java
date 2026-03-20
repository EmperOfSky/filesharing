package com.filesharing.service.impl;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.filesharing.service.GeoIpService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 基于外部 GeoIP 服务的地址解析实现
 */
@Slf4j
@Service
public class GeoIpServiceImpl implements GeoIpService {

    private static final Duration CACHE_TTL = Duration.ofHours(24);
    private static final String UNKNOWN = "unknown";

    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;
    private final String serviceUrl;
    private final Map<String, CacheEntry> cache = new ConcurrentHashMap<>();

    public GeoIpServiceImpl(ObjectMapper objectMapper,
                            @Value("${geoip.service-url:https://ipwho.is/{ip}}") String serviceUrl,
                            @Value("${geoip.timeout-ms:3000}") long timeoutMs) {
        this.objectMapper = objectMapper;
        this.serviceUrl = serviceUrl;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(timeoutMs))
                .build();
    }

    @Override
    public String resolveLocation(String ipAddress) {
        String normalizedIp = normalizeIp(ipAddress);
        if (normalizedIp == null) {
            return UNKNOWN;
        }

        if (isPrivateOrLoopback(normalizedIp)) {
            return classifyPrivateIp(normalizedIp);
        }

        CacheEntry cached = cache.get(normalizedIp);
        if (cached != null && !cached.isExpired()) {
            return cached.location;
        }

        String resolved = queryExternalService(normalizedIp);
        cache.put(normalizedIp, new CacheEntry(resolved, Instant.now().plus(CACHE_TTL)));
        return resolved;
    }

    private String queryExternalService(String ipAddress) {
        try {
            String encodedIp = URLEncoder.encode(ipAddress, StandardCharsets.UTF_8);
            String url = serviceUrl.replace("{ip}", encodedIp);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(3))
                    .header("Accept", "application/json")
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                log.debug("GeoIP 请求失败，状态码: {}, ip: {}", response.statusCode(), ipAddress);
                return UNKNOWN;
            }

            GeoIpResponse geoIpResponse = objectMapper.readValue(response.body(), GeoIpResponse.class);
            if (!geoIpResponse.success) {
                String message = geoIpResponse.message == null ? UNKNOWN : geoIpResponse.message;
                log.debug("GeoIP 返回失败: {}, ip: {}", message, ipAddress);
                return UNKNOWN;
            }

            return joinParts(geoIpResponse.country, geoIpResponse.region, geoIpResponse.city);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.debug("GeoIP 查询被中断，ip={}: {}", ipAddress, e.getMessage());
            return UNKNOWN;
        } catch (IOException | IllegalArgumentException e) {
            log.debug("GeoIP 查询失败，ip={}: {}", ipAddress, e.getMessage());
            return UNKNOWN;
        } catch (Exception e) {
            log.debug("GeoIP 查询异常，ip={}: {}", ipAddress, e.getMessage());
            return UNKNOWN;
        }
    }

    private String joinParts(String country, String region, String city) {
        StringBuilder sb = new StringBuilder();
        appendPart(sb, country);
        appendPart(sb, region);
        appendPart(sb, city);
        return sb.length() == 0 ? UNKNOWN : sb.toString();
    }

    private void appendPart(StringBuilder sb, String value) {
        if (value == null || value.isBlank()) {
            return;
        }
        if (sb.length() > 0) {
            sb.append('/');
        }
        sb.append(value.trim());
    }

    private String normalizeIp(String ipAddress) {
        if (ipAddress == null || ipAddress.isBlank()) {
            return null;
        }
        return ipAddress.trim();
    }

    private boolean isPrivateOrLoopback(String ipAddress) {
        String lower = ipAddress.toLowerCase(Locale.ROOT);
        if ("127.0.0.1".equals(lower) || "::1".equals(lower) || "0:0:0:0:0:0:0:1".equals(lower)) {
            return true;
        }

        if (lower.startsWith("10.") || lower.startsWith("192.168.") || lower.startsWith("169.254.")) {
            return true;
        }

        if (lower.startsWith("172.")) {
            String[] parts = lower.split("\\.");
            if (parts.length >= 2) {
                try {
                    int secondOctet = Integer.parseInt(parts[1]);
                    return secondOctet >= 16 && secondOctet <= 31;
                } catch (NumberFormatException ignored) {
                    return false;
                }
            }
        }

        return lower.startsWith("fc") || lower.startsWith("fd") || lower.startsWith("fe80:");
    }

    private String classifyPrivateIp(String ipAddress) {
        if ("127.0.0.1".equals(ipAddress) || "::1".equals(ipAddress) || "0:0:0:0:0:0:0:1".equals(ipAddress)) {
            return "loopback";
        }
        return "private-network";
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class GeoIpResponse {
        @JsonProperty("success")
        private boolean success;

        @JsonProperty("country")
        private String country;

        @JsonProperty("region")
        private String region;

        @JsonProperty("city")
        private String city;

        @JsonProperty("message")
        private String message;
    }

    private static class CacheEntry {
        private final String location;
        private final Instant expiresAt;

        private CacheEntry(String location, Instant expiresAt) {
            this.location = location;
            this.expiresAt = expiresAt;
        }

        private boolean isExpired() {
            return Instant.now().isAfter(expiresAt);
        }
    }
}
