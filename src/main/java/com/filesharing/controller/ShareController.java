package com.filesharing.controller;

import com.filesharing.dto.ApiResponse;
import com.filesharing.dto.ShareCreateRequest;
import com.filesharing.dto.ShareResponse;
import com.filesharing.entity.FileEntity;
import com.filesharing.entity.User;
import com.filesharing.service.ShareService;
import com.filesharing.service.UserService;
import com.filesharing.util.FileStorageUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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
        ShareResponse response = shareService.getPublicShareInfo(shareKey);
        response.setShortLink(toAbsoluteLink(request, response.getShortLink()));
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/public/{shareKey}/access")
    public ResponseEntity<ApiResponse<Map<String, Object>>> accessPublicShare(
            @PathVariable String shareKey,
            @RequestBody(required = false) Map<String, String> body,
            HttpServletRequest request) {
        cleanupExpiredTokens();

        String password = body == null ? null : body.get("password");
        ShareResponse share = shareService.accessShare(shareKey, password, request.getRemoteAddr());
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
        try {
            String storageName = fileEntity.getStorageName();
            Path uploadPath = Paths.get(fileStorageUtil.getUploadPath()).toAbsolutePath().normalize();
            Path filePath = uploadPath.resolve(storageName).normalize();

            if (!Files.exists(filePath)) {
                String fallbackPath = fileEntity.getFilePath();
                if (fallbackPath != null && !fallbackPath.isBlank()) {
                    String fileName = fallbackPath.substring(fallbackPath.lastIndexOf('/') + 1);
                    filePath = uploadPath.resolve(fileName).normalize();
                }
            }

            if (!Files.exists(filePath)) {
                return null;
            }

            return new UrlResource(filePath.toUri());
        } catch (MalformedURLException e) {
            log.error("解析分享文件失败: {}", e.getMessage(), e);
            return null;
        }
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

    private static class AccessGrant {
        private final String shareKey;
        private final LocalDateTime expiresAt;

        private AccessGrant(String shareKey, LocalDateTime expiresAt) {
            this.shareKey = shareKey;
            this.expiresAt = expiresAt;
        }
    }
}
