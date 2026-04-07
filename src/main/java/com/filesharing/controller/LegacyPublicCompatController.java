package com.filesharing.controller;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.filesharing.config.FileCodeBoxProperties;
import com.filesharing.dto.filecodebox.PresignUploadConfirmRequest;
import com.filesharing.dto.filecodebox.PresignUploadInitRequest;
import com.filesharing.dto.filecodebox.ShareSelectRequest;
import com.filesharing.entity.PickupCodeRecord;
import com.filesharing.entity.User;
import com.filesharing.exception.BusinessException;
import com.filesharing.service.FileCodeBoxService;
import com.filesharing.service.UserService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * old_python_example 公共接口兼容层（/api/share|chunk|presign）。
 */
@Slf4j
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Validated
public class LegacyPublicCompatController {

    private final FileCodeBoxService fileCodeBoxService;
    private final FileCodeBoxProperties fileCodeBoxProperties;
    private final UserService userService;

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("status", "ok");
        detail.put("service", "file-sharing-system");
        return legacyOk(detail);
    }

    @PostMapping({"/share/text", "/share/text/"})
    public ResponseEntity<Map<String, Object>> shareText(@RequestParam("text") String text,
                                                         @RequestParam(value = "expire_value", defaultValue = "1") Integer expireValue,
                                                         @RequestParam(value = "expire_style", defaultValue = "day") String expireStyle,
                                                         HttpServletRequest request) {
        Long uploaderId = resolveUploaderId(request);
        Map<String, Object> detail = fileCodeBoxService.shareText(
                text,
                expireValue,
                expireStyle,
                resolveClientIp(request),
                uploaderId
        );
            return legacyOk(detail);
    }

    @PostMapping({"/share/file", "/share/file/"})
            public ResponseEntity<Map<String, Object>> shareFile(@RequestParam("file") MultipartFile file,
                                     @RequestParam(value = "expire_value", defaultValue = "1") Integer expireValue,
                                     @RequestParam(value = "expire_style", defaultValue = "day") String expireStyle,
                                     HttpServletRequest request) throws Exception {
        Long uploaderId = resolveUploaderId(request);
        Map<String, Object> detail = fileCodeBoxService.shareFile(
                file,
                expireValue,
                expireStyle,
                resolveClientIp(request),
                uploaderId
        );
            return legacyOk(detail);
    }

    @GetMapping({"/share/select", "/share/select/"})
    public ResponseEntity<?> getByCode(@RequestParam("code") String code, HttpServletRequest request) {
        PickupCodeRecord record = fileCodeBoxService.directFetchByCode(code, resolveClientIp(request));
        return buildContentResponse(record);
    }

    @PostMapping({"/share/select", "/share/select/"})
    public ResponseEntity<Map<String, Object>> selectByCode(@Valid @RequestBody ShareSelectRequest body,
                                                             HttpServletRequest request) {
        Map<String, Object> detail = fileCodeBoxService.selectByCode(body.getCode(), resolveClientIp(request));
        return legacyOk(adaptLegacySelectDetail(detail));
    }

    @GetMapping("/share/download")
    public ResponseEntity<?> downloadByKey(@RequestParam("key") String key,
                                           @RequestParam("code") String code,
                                           HttpServletRequest request) {
        PickupCodeRecord record = fileCodeBoxService.validateDownload(key, code, resolveClientIp(request));
        return buildContentResponse(record);
    }

    @PostMapping({"/chunk/upload/init", "/chunk/upload/init/"})
    public ResponseEntity<Map<String, Object>> initChunkUpload(@RequestBody LegacyChunkInitRequest body,
                                                                HttpServletRequest request) {
        Long uploaderId = resolveUploaderId(request);
        Map<String, Object> detail = fileCodeBoxService.initChunkUpload(
                body.getFileName(),
                body.getFileSize(),
            body.getFileHash(),
                body.getChunkSize(),
                body.getExpireStyle(),
                body.getExpireValue(),
                resolveClientIp(request),
                uploaderId
        );
        return legacyOk(detail);
    }

    @PostMapping({"/chunk/upload/chunk/{uploadId}/{chunkIndex}", "/chunk/upload/chunk/{uploadId}/{chunkIndex}/"})
    public ResponseEntity<Map<String, Object>> uploadChunk(@PathVariable String uploadId,
                                                            @PathVariable Integer chunkIndex,
                                                            @RequestParam("chunk") MultipartFile chunk) throws Exception {
        Map<String, Object> detail = fileCodeBoxService.uploadChunk(uploadId, chunkIndex, chunk);
        return legacyOk(detail);
    }

    @GetMapping({"/chunk/upload/status/{uploadId}", "/chunk/upload/status/{uploadId}/"})
    public ResponseEntity<Map<String, Object>> getChunkStatus(@PathVariable String uploadId) {
        Map<String, Object> detail = fileCodeBoxService.getChunkUploadStatus(uploadId);
        return legacyOk(detail);
    }

    @DeleteMapping({"/chunk/upload/{uploadId}", "/chunk/upload/{uploadId}/"})
    public ResponseEntity<Map<String, Object>> cancelChunkUpload(@PathVariable String uploadId) {
        fileCodeBoxService.cancelChunkUpload(uploadId);
        Map<String, Object> detail = new HashMap<>();
        detail.put("message", "上传已取消");
        return legacyOk(detail);
    }

    @PostMapping({"/chunk/upload/complete/{uploadId}", "/chunk/upload/complete/{uploadId}/"})
    public ResponseEntity<Map<String, Object>> completeChunkUpload(@PathVariable String uploadId,
                                                                    @RequestBody LegacyChunkCompleteRequest body,
                                                                    HttpServletRequest request) throws Exception {
        Map<String, Object> detail = fileCodeBoxService.completeChunkUpload(
                uploadId,
                body.getExpireValue(),
                body.getExpireStyle(),
                resolveClientIp(request)
        );
        return legacyOk(detail);
    }

    @PostMapping({"/presign/upload/init", "/presign/upload/init/"})
    public ResponseEntity<Map<String, Object>> initPresignUpload(@Valid @RequestBody PresignUploadInitRequest body,
                                                                  HttpServletRequest request) {
        Long uploaderId = resolveUploaderId(request);
        Map<String, Object> detail = fileCodeBoxService.initPresignUpload(
                body.getFileName(),
                body.getFileSize(),
                body.getExpireValue(),
                body.getExpireStyle(),
                resolveClientIp(request),
                uploaderId
        );
        return legacyOk(detail);
    }

    @PutMapping({"/presign/upload/proxy/{uploadId}", "/presign/upload/proxy/{uploadId}/"})
    public ResponseEntity<Map<String, Object>> uploadByProxy(@PathVariable String uploadId,
                                                              @RequestParam("file") MultipartFile file,
                                                              HttpServletRequest request) throws Exception {
        Long uploaderId = resolveUploaderId(request);
        if (uploaderId == null && !fileCodeBoxProperties.isOpenUpload()) {
            throw new BusinessException("AUTH_REQUIRED", "本站未开启游客上传，如需上传请先登录后台");
        }

        Map<String, Object> detail = fileCodeBoxService.uploadPresignProxy(uploadId, file, resolveClientIp(request));
        return legacyOk(detail);
    }

    @PostMapping({"/presign/upload/confirm/{uploadId}", "/presign/upload/confirm/{uploadId}/"})
    public ResponseEntity<Map<String, Object>> confirmPresignUpload(@PathVariable String uploadId,
                                                                     @RequestBody(required = false) PresignUploadConfirmRequest body,
                                                                     HttpServletRequest request) {
        Long uploaderId = resolveUploaderId(request);
        if (uploaderId == null && !fileCodeBoxProperties.isOpenUpload()) {
            throw new BusinessException("AUTH_REQUIRED", "本站未开启游客上传，如需上传请先登录后台");
        }

        Integer expireValue = body == null ? null : body.getExpireValue();
        String expireStyle = body == null ? null : body.getExpireStyle();

        Map<String, Object> detail = fileCodeBoxService.confirmPresignUpload(uploadId, expireValue, expireStyle);
        return legacyOk(detail);
    }

    @GetMapping({"/presign/upload/status/{uploadId}", "/presign/upload/status/{uploadId}/"})
    public ResponseEntity<Map<String, Object>> getPresignStatus(@PathVariable String uploadId) {
        Map<String, Object> detail = fileCodeBoxService.getPresignStatus(uploadId);
        return legacyOk(detail);
    }

    @DeleteMapping({"/presign/upload/{uploadId}", "/presign/upload/{uploadId}/"})
    public ResponseEntity<Map<String, Object>> cancelPresignSession(@PathVariable String uploadId) {
        fileCodeBoxService.cancelPresignUpload(uploadId);
        Map<String, Object> detail = new HashMap<>();
        detail.put("message", "上传会话已取消");
        return legacyOk(detail);
    }

    private ResponseEntity<?> buildContentResponse(PickupCodeRecord record) {
        if (record.getShareType() == PickupCodeRecord.ShareType.TEXT) {
            return ResponseEntity.ok(legacyBody(200, "ok", record.getTextContent()));
        }

        if (record.getStorageMode() == PickupCodeRecord.StorageMode.CLOUD_DIRECT) {
            String signedUrl = fileCodeBoxService.buildCloudDownloadUrl(record);
            if (signedUrl == null || signedUrl.isBlank()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(legacyBody(404, "文件不存在或链接已失效", null));
            }
            return ResponseEntity.status(HttpStatus.FOUND)
                    .header(HttpHeaders.LOCATION, signedUrl)
                    .build();
        }

        Resource resource = fileCodeBoxService.loadLocalResource(record);
        if (resource == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(legacyBody(404, "文件已过期删除或不存在", null));
        }

        String contentType = record.getContentType();
        if (contentType == null || contentType.isBlank()) {
            contentType = MediaType.APPLICATION_OCTET_STREAM_VALUE;
        }

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + record.getDisplayName() + "\"")
                .body(resource);
    }

    private ResponseEntity<Map<String, Object>> legacyOk(Object detail) {
        return ResponseEntity.ok(legacyBody(200, "ok", detail));
    }

    private Map<String, Object> adaptLegacySelectDetail(Map<String, Object> detail) {
        if (detail == null) {
            return null;
        }
        Object text = detail.get("text");
        if (text instanceof String) {
            String value = (String) text;
            if (value.startsWith("/api/public/share/download")) {
                detail.put("text", value.replaceFirst("^/api/public/share/download", "/api/share/download"));
            }
        }
        return detail;
    }

    private Map<String, Object> legacyBody(int code, String message, Object detail) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("code", code);
        body.put("message", message);
        body.put("detail", detail);
        return body;
    }

    private Long resolveUploaderId(HttpServletRequest request) {
        if (!fileCodeBoxProperties.isOpenUpload()) {
            User user = userService.getCurrentUser(request);
            if (user.getRole() != User.UserRole.ADMIN) {
                throw new BusinessException("AUTH_REQUIRED", "本站未开启游客上传，如需上传请先登录后台");
            }
            return user.getId();
        }

        try {
            String auth = request.getHeader("Authorization");
            if (auth != null && auth.startsWith("Bearer ")) {
                User user = userService.getCurrentUser(request);
                return user.getId();
            }
        } catch (Exception ignored) {
            // 游客上传模式下忽略无效 token。
        }
        return null;
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

    @Data
    private static class LegacyChunkInitRequest {
        @JsonProperty("file_name")
        private String fileName;

        @JsonProperty("chunk_size")
        private Integer chunkSize = 5 * 1024 * 1024;

        @JsonProperty("file_size")
        private Long fileSize;

        @JsonProperty("file_hash")
        private String fileHash;

        @JsonProperty("expire_value")
        private Integer expireValue = 1;

        @JsonProperty("expire_style")
        private String expireStyle = "day";
    }

    @Data
    private static class LegacyChunkCompleteRequest {
        @JsonProperty("expire_value")
        private Integer expireValue = 1;

        @JsonProperty("expire_style")
        private String expireStyle = "day";
    }
}
