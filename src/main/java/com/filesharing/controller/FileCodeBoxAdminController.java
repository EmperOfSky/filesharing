package com.filesharing.controller;

import com.filesharing.audit.SecurityAuditService;
import com.filesharing.config.FileCodeBoxProperties;
import com.filesharing.dto.ApiResponse;
import com.filesharing.dto.filecodebox.FileCodeBoxConfigUpdateRequest;
import com.filesharing.dto.filecodebox.FileCodeBoxRecordStatusUpdateRequest;
import com.filesharing.entity.User;
import com.filesharing.exception.BusinessException;
import com.filesharing.service.FileCodeBoxService;
import com.filesharing.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.util.*;

/**
 * 快传中心后台管理接口。
 */
@RestController
@RequestMapping({"/api/admin/quick-transfer", "/api/admin/fcb"})
@RequiredArgsConstructor
@Validated
public class FileCodeBoxAdminController {

    private static final Set<String> ALLOWED_EXPIRE_STYLES =
            new LinkedHashSet<>(Arrays.asList("day", "hour", "minute", "forever", "count"));

    private final FileCodeBoxProperties fileCodeBoxProperties;
    private final UserService userService;
    private final FileCodeBoxService fileCodeBoxService;
    private final SecurityAuditService securityAuditService;

    @GetMapping("/config")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getConfig(HttpServletRequest request) {
        User admin = ensureAdmin(request);
        logAdminAudit("FCB_ADMIN_CONFIG_READ", admin, request, "/api/admin/quick-transfer/config", Collections.emptyMap());
        return ResponseEntity.ok(ApiResponse.success(buildConfig()));
    }

    @PutMapping("/config")
    public ResponseEntity<ApiResponse<Map<String, Object>>> updateConfig(@Valid @RequestBody FileCodeBoxConfigUpdateRequest body,
                                                                          HttpServletRequest request) {
        User admin = ensureAdmin(request);

        synchronized (fileCodeBoxProperties) {
            if (body.getOpenUpload() != null) {
                fileCodeBoxProperties.setOpenUpload(body.getOpenUpload());
            }
            if (body.getUploadSize() != null) {
                fileCodeBoxProperties.setUploadSize(body.getUploadSize());
            }
            if (body.getUploadCount() != null) {
                fileCodeBoxProperties.setUploadCount(body.getUploadCount());
            }
            if (body.getUploadMinute() != null) {
                fileCodeBoxProperties.setUploadMinute(body.getUploadMinute());
            }
            if (body.getErrorCount() != null) {
                fileCodeBoxProperties.setErrorCount(body.getErrorCount());
            }
            if (body.getErrorMinute() != null) {
                fileCodeBoxProperties.setErrorMinute(body.getErrorMinute());
            }
            if (body.getMaxSaveSeconds() != null) {
                fileCodeBoxProperties.setMaxSaveSeconds(body.getMaxSaveSeconds());
            }
            if (body.getPresignExpireSeconds() != null) {
                fileCodeBoxProperties.setPresignExpireSeconds(body.getPresignExpireSeconds());
            }
            if (body.getDownloadTokenTtlSeconds() != null) {
                fileCodeBoxProperties.setDownloadTokenTtlSeconds(body.getDownloadTokenTtlSeconds());
            }
            if (body.getExpireStyles() != null) {
                List<String> normalized = normalizeExpireStyles(body.getExpireStyles());
                if (normalized.isEmpty()) {
                    throw new BusinessException("INVALID_EXPIRE_STYLES", "expire_styles 不能为空");
                }
                fileCodeBoxProperties.setExpireStyles(normalized);
            }
        }

        Map<String, Object> details = new LinkedHashMap<>();
        details.put("open_upload", fileCodeBoxProperties.isOpenUpload());
        details.put("upload_size", fileCodeBoxProperties.getUploadSize());
        details.put("expire_styles", fileCodeBoxProperties.getExpireStyles());
        logAdminAudit("FCB_ADMIN_CONFIG_UPDATE", admin, request, "/api/admin/quick-transfer/config", details);

        return ResponseEntity.ok(ApiResponse.success("配置更新成功", buildConfig()));
    }

    @GetMapping("/records")
    public ResponseEntity<ApiResponse<Map<String, Object>>> listRecords(@RequestParam(value = "page", required = false) Integer page,
                                                                        @RequestParam(value = "size", required = false) Integer size,
                                                                        @RequestParam(value = "keyword", required = false) String keyword,
                                                                        @RequestParam(value = "status", required = false) String status,
                                                                        @RequestParam(value = "share_type", required = false) String shareType,
                                                                        HttpServletRequest request) {
        User admin = ensureAdmin(request);
        Map<String, Object> detail = fileCodeBoxService.listRecords(keyword, status, shareType, page, size);
        Map<String, Object> auditDetails = new LinkedHashMap<>();
        auditDetails.put("keyword", keyword);
        auditDetails.put("status", status);
        auditDetails.put("share_type", shareType);
        auditDetails.put("page", detail.get("number"));
        auditDetails.put("size", detail.get("size"));
        auditDetails.put("total", detail.get("totalElements"));
        logAdminAudit("FCB_ADMIN_RECORD_LIST", admin, request, "/api/admin/quick-transfer/records", auditDetails);
        return ResponseEntity.ok(ApiResponse.success(detail));
    }

    @GetMapping("/records/export")
    public ResponseEntity<byte[]> exportRecords(@RequestParam(value = "keyword", required = false) String keyword,
                                                @RequestParam(value = "status", required = false) String status,
                                                @RequestParam(value = "share_type", required = false) String shareType,
                                                HttpServletRequest request) {
        User admin = ensureAdmin(request);
        FileCodeBoxService.CsvExportResult exportResult = fileCodeBoxService.exportRecordsCsv(keyword, status, shareType);

        Map<String, Object> details = new LinkedHashMap<>();
        details.put("keyword", keyword);
        details.put("status", status);
        details.put("share_type", shareType);
        details.put("rows", exportResult.getRowCount());
        details.put("file_name", exportResult.getFileName());
        logAdminAudit("FCB_ADMIN_RECORD_EXPORT", admin, request, "/api/admin/quick-transfer/records/export", details);

        String contentDisposition = "attachment; filename=\"" + exportResult.getFileName() + "\"";
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("text/csv;charset=UTF-8"))
                .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition)
                .body(exportResult.getContent().getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }

    @PatchMapping("/records/{recordId}/status")
    public ResponseEntity<ApiResponse<Map<String, Object>>> updateRecordStatus(@PathVariable Long recordId,
                                                                                @Valid @RequestBody FileCodeBoxRecordStatusUpdateRequest body,
                                                                                HttpServletRequest request) {
                                            User admin = ensureAdmin(request);
        Map<String, Object> detail = fileCodeBoxService.updateRecordStatus(recordId, body.getStatus());
                                            Map<String, Object> auditDetails = new LinkedHashMap<>();
                                            auditDetails.put("record_id", recordId);
                                            auditDetails.put("target_status", body.getStatus());
                                            logAdminAudit("FCB_ADMIN_RECORD_STATUS_UPDATE", admin, request,
                                                "/api/admin/quick-transfer/records/" + recordId + "/status", auditDetails);
        return ResponseEntity.ok(ApiResponse.success("状态已更新", detail));
    }

    @DeleteMapping("/records/{recordId}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> deleteRecord(@PathVariable Long recordId,
                                                                         HttpServletRequest request) {
        User admin = ensureAdmin(request);
        fileCodeBoxService.deleteRecord(recordId);
        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("id", recordId);
        Map<String, Object> auditDetails = new LinkedHashMap<>();
        auditDetails.put("record_id", recordId);
        logAdminAudit("FCB_ADMIN_RECORD_DELETE", admin, request,
            "/api/admin/quick-transfer/records/" + recordId, auditDetails);
        return ResponseEntity.ok(ApiResponse.success("记录已删除", detail));
    }

    private User ensureAdmin(HttpServletRequest request) {
        User user;
        try {
            user = userService.getCurrentUser(request);
        } catch (RuntimeException ex) {
            logDeniedAudit(request, "AUTH_FAILED", ex.getMessage());
            throw ex;
        }

        if (user.getRole() != User.UserRole.ADMIN) {
            logDeniedAudit(request, "NOT_ADMIN", "仅管理员可操作该接口");
            throw new BusinessException("FORBIDDEN", "仅管理员可操作该接口");
        }
        return user;
    }

    private void logDeniedAudit(HttpServletRequest request, String reason, String message) {
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("method", request.getMethod());
        details.put("path", request.getRequestURI());
        details.put("reason", reason);
        details.put("message", message);
        securityAuditService.logSecurityEvent(
                "FCB_ADMIN_ACCESS_DENIED",
                null,
                resolveClientIp(request),
                request.getRequestURI(),
                details
        );
    }

    private void logAdminAudit(String eventType,
                               User admin,
                               HttpServletRequest request,
                               String resource,
                               Map<String, Object> details) {
        Map<String, Object> safeDetails = details == null ? new LinkedHashMap<>() : new LinkedHashMap<>(details);
        safeDetails.put("method", request.getMethod());
        securityAuditService.logSecurityEvent(
                eventType,
                admin == null ? null : String.valueOf(admin.getId()),
                resolveClientIp(request),
                resource,
                safeDetails
        );
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

    private List<String> normalizeExpireStyles(List<String> styles) {
        LinkedHashSet<String> result = new LinkedHashSet<>();
        for (String style : styles) {
            if (style == null) {
                continue;
            }
            String normalized = style.trim().toLowerCase(Locale.ROOT);
            if (normalized.isEmpty()) {
                continue;
            }
            if (!ALLOWED_EXPIRE_STYLES.contains(normalized)) {
                throw new BusinessException("INVALID_EXPIRE_STYLE", "不支持的过期策略: " + normalized);
            }
            result.add(normalized);
        }
        return new ArrayList<>(result);
    }

    private Map<String, Object> buildConfig() {
        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("open_upload", fileCodeBoxProperties.isOpenUpload());
        detail.put("upload_size", fileCodeBoxProperties.getUploadSize());
        detail.put("upload_size_mb", fileCodeBoxProperties.getUploadSize() / 1024.0 / 1024.0);
        detail.put("upload_count", fileCodeBoxProperties.getUploadCount());
        detail.put("upload_minute", fileCodeBoxProperties.getUploadMinute());
        detail.put("error_count", fileCodeBoxProperties.getErrorCount());
        detail.put("error_minute", fileCodeBoxProperties.getErrorMinute());
        detail.put("max_save_seconds", fileCodeBoxProperties.getMaxSaveSeconds());
        detail.put("expire_styles", fileCodeBoxProperties.getExpireStyles());
        detail.put("presign_expire_seconds", fileCodeBoxProperties.getPresignExpireSeconds());
        detail.put("download_token_ttl_seconds", fileCodeBoxProperties.getDownloadTokenTtlSeconds());
        return detail;
    }
}
