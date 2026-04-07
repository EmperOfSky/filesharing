package com.filesharing.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.filesharing.config.SecurityHardeningProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Comparator;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@RequiredArgsConstructor
public class IpRateLimitFilter extends OncePerRequestFilter {

    private static final long WINDOW_MILLIS = 1000L;

    private final SecurityHardeningProperties securityHardeningProperties;
    private final ObjectMapper objectMapper;
    private final AntPathMatcher antPathMatcher = new AntPathMatcher();

    private final Map<String, Deque<Long>> globalIpWindow = new ConcurrentHashMap<>();
    private final Map<String, Deque<Long>> endpointIpWindow = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String uri = request.getRequestURI();
        if (uri != null && (uri.startsWith("/swagger-ui") || uri.startsWith("/v3/api-docs") || uri.startsWith("/api/v3/api-docs"))) {
            filterChain.doFilter(request, response);
            return;
        }

        String clientIp = resolveClientIp(request);
        int globalLimit = Math.max(securityHardeningProperties.getRateLimit().getGlobalPerSecond(), 1);

        if (!allowRequest(globalIpWindow, clientIp, globalLimit)) {
            writeTooManyRequests(response, "GLOBAL_RATE_LIMIT_EXCEEDED", "请求过于频繁，请稍后重试");
            return;
        }

        EndpointLimit endpointLimit = resolveEndpointLimit(uri);
        if (endpointLimit != null && endpointLimit.limit > 0) {
            String endpointKey = clientIp + "::" + endpointLimit.pattern;
            if (!allowRequest(endpointIpWindow, endpointKey, endpointLimit.limit)) {
                writeTooManyRequests(response, "ENDPOINT_RATE_LIMIT_EXCEEDED", "该接口请求频率过高，请稍后重试");
                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    private EndpointLimit resolveEndpointLimit(String uri) {
        if (uri == null) {
            return null;
        }

        return securityHardeningProperties.getRateLimit().getEndpointLimits().entrySet().stream()
                .filter(entry -> antPathMatcher.match(entry.getKey(), uri))
                .max(Comparator.comparingInt(entry -> entry.getKey().length()))
                .map(entry -> new EndpointLimit(entry.getKey(), entry.getValue()))
                .orElse(null);
    }

    private static class EndpointLimit {
        private final String pattern;
        private final int limit;

        private EndpointLimit(String pattern, int limit) {
            this.pattern = pattern;
            this.limit = limit;
        }
    }

    private boolean allowRequest(Map<String, Deque<Long>> windowMap, String key, int limitPerSecond) {
        long now = System.currentTimeMillis();
        Deque<Long> deque = windowMap.computeIfAbsent(key, k -> new ArrayDeque<>());

        synchronized (deque) {
            while (!deque.isEmpty() && now - deque.peekFirst() >= WINDOW_MILLIS) {
                deque.pollFirst();
            }

            if (deque.size() >= limitPerSecond) {
                log.warn("触发限流: key={}, limit={}, ts={}", key, limitPerSecond, Instant.ofEpochMilli(now));
                return false;
            }

            deque.addLast(now);
            return true;
        }
    }

    private void writeTooManyRequests(HttpServletResponse response, String code, String message) throws IOException {
        response.setStatus(429);
        response.setCharacterEncoding("UTF-8");
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("success", false);
        payload.put("errorCode", code);
        payload.put("message", message);

        response.getWriter().write(objectMapper.writeValueAsString(payload));
    }

    private String resolveClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            return xForwardedFor.split(",")[0].trim();
        }

        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isBlank()) {
            return xRealIp.trim();
        }

        return request.getRemoteAddr();
    }
}
