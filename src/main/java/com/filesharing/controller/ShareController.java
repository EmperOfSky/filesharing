package com.filesharing.controller;

import com.filesharing.dto.ApiResponse;
import com.filesharing.dto.ShareCreateRequest;
import com.filesharing.dto.ShareResponse;
import com.filesharing.entity.FileEntity;
import com.filesharing.entity.User;
import com.filesharing.service.GeoIpService;
import com.filesharing.service.ShareService;
import com.filesharing.service.UserService;
import com.filesharing.util.FileStorageUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 短链分享控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/shares")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class ShareController {

    private static final long ACCESS_TOKEN_TTL_SECONDS = 600;

    private final ShareService shareService;
    private final UserService userService;
    private final GeoIpService geoIpService;
    private final FileStorageUtil fileStorageUtil;

    private final Map<String, AccessGrant> accessTokenStore = new ConcurrentHashMap<>();

    @PostMapping
    public ResponseEntity<ApiResponse<ShareResponse>> createShare(
            @Valid @RequestBody ShareCreateRequest request,
            HttpServletRequest httpRequest) {
        User currentUser = userService.getCurrentUser(httpRequest);
        ShareResponse response = shareService.createShare(request, currentUser);
        response.setShortLink(toAbsoluteLink(httpRequest, response.getShortLink()));
        return ResponseEntity.ok(ApiResponse.success("分享创建成功", response));
    }

    @GetMapping("/mine")
    public ResponseEntity<ApiResponse<List<ShareResponse>>> getMyShares(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            HttpServletRequest httpRequest) {
        User currentUser = userService.getCurrentUser(httpRequest);
        List<ShareResponse> shares = shareService.getUserShares(currentUser, page, size);
        shares.forEach(share -> share.setShortLink(toAbsoluteLink(httpRequest, share.getShortLink())));
        return ResponseEntity.ok(ApiResponse.success(shares));
    }

    @GetMapping("/public/{shareKey}")
    public ResponseEntity<ApiResponse<ShareResponse>> getPublicShareInfo(
            @PathVariable String shareKey,
            HttpServletRequest request) {
        String ipAddress = resolveClientIp(request);
        String visitorAddress = resolveVisitorAddress(request, ipAddress);
        String userAgent = request.getHeader("User-Agent");

        ShareResponse response = shareService.getPublicShareInfoWithTracking(
            shareKey,
            ipAddress,
            visitorAddress,
            userAgent,
            request.getHeader("Referer")
        );
        response.setShortLink(toAbsoluteLink(request, response.getShortLink()));
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/{shareId}/monitoring")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getShareMonitoring(
            @PathVariable Long shareId,
            @RequestParam(defaultValue = "20") int limit,
            HttpServletRequest request) {
        User currentUser = userService.getCurrentUser(request);
        Map<String, Object> details = shareService.getShareMonitoringDetails(shareId, currentUser, limit);
        return ResponseEntity.ok(ApiResponse.success("获取分享监控详情成功", details));
    }

    @PostMapping("/public/{shareKey}/access")
    public ResponseEntity<ApiResponse<Map<String, Object>>> accessPublicShare(
            @PathVariable String shareKey,
            @RequestBody(required = false) Map<String, String> body,
            HttpServletRequest request) {
        cleanupExpiredTokens();

        String password = body == null ? null : body.get("password");
        ShareResponse share = shareService.accessShare(shareKey, password, resolveClientIp(request));
        share.setShortLink(toAbsoluteLink(request, share.getShortLink()));

        String accessToken = UUID.randomUUID().toString().replace("-", "");
        LocalDateTime expiresAt = LocalDateTime.now().plusSeconds(ACCESS_TOKEN_TTL_SECONDS);
        accessTokenStore.put(accessToken, new AccessGrant(shareKey, expiresAt));

        Map<String, Object> result = new HashMap<>();
        result.put("share", share);
        result.put("accessToken", accessToken);
        result.put("expiresInSeconds", ACCESS_TOKEN_TTL_SECONDS);
        return ResponseEntity.ok(ApiResponse.success("访问授权成功", result));
    }

    @PostMapping("/public/{shareKey}/download")
    public ResponseEntity<Resource> downloadSharedFile(
            @PathVariable String shareKey,
            @RequestBody(required = false) Map<String, String> body) {
        cleanupExpiredTokens();

        String accessToken = body == null ? null : body.get("accessToken");
        if (accessToken == null || accessToken.isBlank()) {
            return ResponseEntity.badRequest().build();
        }

        AccessGrant grant = accessTokenStore.get(accessToken);
        if (grant == null || grant.expiresAt.isBefore(LocalDateTime.now()) || !shareKey.equals(grant.shareKey)) {
            return ResponseEntity.status(401).build();
        }

        // 一次性下载令牌，下载完成后立即失效。
        accessTokenStore.remove(accessToken);

        FileEntity fileEntity = shareService.resolveShareFileForDownload(shareKey, null);
        Resource resource = resolveSharedFileResource(fileEntity);
        if (resource == null) {
            return ResponseEntity.notFound().build();
        }

        String contentType = fileEntity.getContentType();
        if (contentType == null || contentType.isBlank()) {
            contentType = MediaType.APPLICATION_OCTET_STREAM_VALUE;
        }

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + fileEntity.getOriginalName() + "\"")
                .body(resource);
    }

    @DeleteMapping("/{shareId}")
    public ResponseEntity<ApiResponse<String>> deleteShare(
            @PathVariable Long shareId,
            HttpServletRequest request) {
        User currentUser = userService.getCurrentUser(request);
        shareService.deleteShare(shareId, currentUser);
        return ResponseEntity.ok(ApiResponse.success("分享删除成功"));
    }

    @PutMapping("/{shareId}/disable")
    public ResponseEntity<ApiResponse<String>> disableShare(
            @PathVariable Long shareId,
            HttpServletRequest request) {
        User currentUser = userService.getCurrentUser(request);
        shareService.disableShare(shareId, currentUser);
        return ResponseEntity.ok(ApiResponse.success("分享已禁用"));
    }

    @PutMapping("/{shareId}/enable")
    public ResponseEntity<ApiResponse<String>> enableShare(
            @PathVariable Long shareId,
            HttpServletRequest request) {
        User currentUser = userService.getCurrentUser(request);
        shareService.enableShare(shareId, currentUser);
        return ResponseEntity.ok(ApiResponse.success("分享已启用"));
    }

    private Resource resolveSharedFileResource(FileEntity fileEntity) {
        return fileStorageUtil.loadAsResource(fileEntity.getStorageName(), fileEntity.getFilePath());
    }

    private String toAbsoluteLink(HttpServletRequest request, String shortLink) {
        if (shortLink == null || shortLink.isBlank()) {
            return shortLink;
        }
        if (shortLink.startsWith("http://") || shortLink.startsWith("https://")) {
            return shortLink;
        }

        String origin = request.getHeader("Origin");
        if (origin != null && !origin.isBlank()) {
            return joinBaseAndPath(origin, shortLink);
        }

        String referer = request.getHeader("Referer");
        if (referer != null && !referer.isBlank()) {
            try {
                URI uri = new URI(referer);
                StringBuilder refererBase = new StringBuilder();
                refererBase.append(uri.getScheme()).append("://").append(uri.getHost());
                if (uri.getPort() != -1 && uri.getPort() != 80 && uri.getPort() != 443) {
                    refererBase.append(":").append(uri.getPort());
                }
                return joinBaseAndPath(refererBase.toString(), shortLink);
            } catch (URISyntaxException ignored) {
                // ignore invalid referer and fallback to request host
            }
        }

        StringBuilder sb = new StringBuilder();
        sb.append(request.getScheme()).append("://").append(request.getServerName());
        if (request.getServerPort() != 80 && request.getServerPort() != 443) {
            sb.append(":").append(request.getServerPort());
        }
        return joinBaseAndPath(sb.toString(), shortLink);
    }

    private String joinBaseAndPath(String base, String path) {
        if (base.endsWith("/") && path.startsWith("/")) {
            return base.substring(0, base.length() - 1) + path;
        }
        if (!base.endsWith("/") && !path.startsWith("/")) {
            return base + "/" + path;
        }
        return base + path;
    }

    private void cleanupExpiredTokens() {
        LocalDateTime now = LocalDateTime.now();
        accessTokenStore.entrySet().removeIf(entry -> entry.getValue().expiresAt.isBefore(now));
    }

    private String resolveClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }

        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) {
            return realIp.trim();
        }

        return request.getRemoteAddr();
    }

    private String resolveVisitorAddress(HttpServletRequest request, String ipAddress) {
        String headerAddress = joinGeoHeaders(
                request.getHeader("X-Geo-City"),
                request.getHeader("X-Geo-Region"),
                request.getHeader("X-Geo-Country")
        );
        if (!headerAddress.isBlank()) {
            return headerAddress;
        }

        String cfCountry = request.getHeader("CF-IPCountry");
        if (cfCountry != null && !cfCountry.isBlank()) {
            return cfCountry.trim();
        }

        if (ipAddress == null || ipAddress.isBlank()) {
            return "unknown";
        }

        return geoIpService.resolveLocation(ipAddress);
    }

    private String joinGeoHeaders(String city, String region, String country) {
        StringBuilder sb = new StringBuilder();
        appendAddressPart(sb, country);
        appendAddressPart(sb, region);
        appendAddressPart(sb, city);
        return sb.toString();
    }

    private void appendAddressPart(StringBuilder sb, String value) {
        if (value == null || value.isBlank()) {
            return;
        }
        if (sb.length() > 0) {
            sb.append('/');
        }
        sb.append(value.trim());
    }

    private static class AccessGrant {
        private final String shareKey;
        private final LocalDateTime expiresAt;

        private AccessGrant(String shareKey, LocalDateTime expiresAt) {
            this.shareKey = shareKey;
            this.expiresAt = expiresAt;
        }
    }
}
