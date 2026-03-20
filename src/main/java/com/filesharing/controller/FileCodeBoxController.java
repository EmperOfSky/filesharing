package com.filesharing.controller;

import com.filesharing.config.FileCodeBoxProperties;
import com.filesharing.dto.ApiResponse;
import com.filesharing.dto.filecodebox.PresignUploadConfirmRequest;
import com.filesharing.dto.filecodebox.PresignUploadInitRequest;
import com.filesharing.dto.filecodebox.ShareSelectRequest;
import com.filesharing.entity.PickupCodeRecord;
import com.filesharing.entity.User;
import com.filesharing.exception.BusinessException;
import com.filesharing.service.FileCodeBoxService;
import com.filesharing.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.InvalidMediaTypeException;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * 快传中心公共接口控制器。
 */
@Slf4j
@RestController
@RequestMapping("/api/public")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Validated
public class FileCodeBoxController {

    private final FileCodeBoxService fileCodeBoxService;
    private final FileCodeBoxProperties fileCodeBoxProperties;
    private final UserService userService;

    @PostMapping("/share/text")
    public ResponseEntity<ApiResponse<Map<String, Object>>> shareText(@RequestParam("text") String text,
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
        return ResponseEntity.ok(ApiResponse.success(detail));
    }

    @PostMapping("/share/file")
    public ResponseEntity<ApiResponse<Map<String, Object>>> shareFile(@RequestParam("file") MultipartFile file,
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
        return ResponseEntity.ok(ApiResponse.success(detail));
    }

    @GetMapping("/share/select")
    public ResponseEntity<?> getByCode(@RequestParam("code") String code, HttpServletRequest request) {
        PickupCodeRecord record = fileCodeBoxService.directFetchByCode(code, resolveClientIp(request));
        return buildContentResponse(record);
    }

    @PostMapping("/share/select")
    public ResponseEntity<ApiResponse<Map<String, Object>>> selectByCode(@Valid @RequestBody ShareSelectRequest body,
                                                                          HttpServletRequest request) {
        Map<String, Object> detail = fileCodeBoxService.selectByCode(body.getCode(), resolveClientIp(request));
        return ResponseEntity.ok(ApiResponse.success(detail));
    }

    @GetMapping("/share/download")
    public ResponseEntity<?> downloadByKey(@RequestParam("key") String key,
                                           @RequestParam("code") String code,
                                           HttpServletRequest request) {
        PickupCodeRecord record = fileCodeBoxService.validateDownload(key, code, resolveClientIp(request));
        return buildContentResponse(record);
    }

    @PostMapping("/presign/upload/init")
    public ResponseEntity<ApiResponse<Map<String, Object>>> initPresignUpload(@Valid @RequestBody PresignUploadInitRequest body,
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
        return ResponseEntity.ok(ApiResponse.success(detail));
    }

    @PutMapping("/presign/upload/proxy/{uploadId}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> uploadByProxy(@PathVariable String uploadId,
                                                                           @RequestParam("file") MultipartFile file,
                                                                           HttpServletRequest request) throws Exception {
        Long uploaderId = resolveUploaderId(request);
        ensureUploadAllowed(uploaderId);

        Map<String, Object> detail = fileCodeBoxService.uploadPresignProxy(uploadId, file, resolveClientIp(request));
        return ResponseEntity.ok(ApiResponse.success(detail));
    }

    @PostMapping("/presign/upload/confirm/{uploadId}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> confirmPresignUpload(@PathVariable String uploadId,
                                                                                  @RequestBody(required = false) PresignUploadConfirmRequest body,
                                                                                  HttpServletRequest request) {
        Long uploaderId = resolveUploaderId(request);
        ensureUploadAllowed(uploaderId);

        Integer expireValue = body == null ? null : body.getExpireValue();
        String expireStyle = body == null ? null : body.getExpireStyle();

        Map<String, Object> detail = fileCodeBoxService.confirmPresignUpload(uploadId, expireValue, expireStyle);
        return ResponseEntity.ok(ApiResponse.success(detail));
    }

    @GetMapping("/presign/upload/status/{uploadId}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getPresignStatus(@PathVariable String uploadId) {
        Map<String, Object> detail = fileCodeBoxService.getPresignStatus(uploadId);
        return ResponseEntity.ok(ApiResponse.success(detail));
    }

    private ResponseEntity<?> buildContentResponse(PickupCodeRecord record) {
        if (record.getShareType() == PickupCodeRecord.ShareType.TEXT) {
            Map<String, Object> detail = new HashMap<>();
            detail.put("code", record.getCode());
            detail.put("name", record.getDisplayName());
            detail.put("size", record.getSizeBytes());
            detail.put("text", record.getTextContent());
            return ResponseEntity.ok(ApiResponse.success(detail));
        }

        if (record.getStorageMode() == PickupCodeRecord.StorageMode.CLOUD_DIRECT) {
            String signedUrl = fileCodeBoxService.buildCloudDownloadUrl(record);
            if (signedUrl == null || signedUrl.isBlank()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.error("FILE_NOT_FOUND", "文件不存在或链接已失效"));
            }
            return ResponseEntity.status(HttpStatus.FOUND)
                    .header(HttpHeaders.LOCATION, signedUrl)
                    .build();
        }

        Resource resource = fileCodeBoxService.loadLocalResource(record);
        if (resource == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("FILE_NOT_FOUND", "文件已过期删除或不存在"));
        }

        MediaType mediaType = resolveMediaType(record.getContentType());
        String fileName = resolveDownloadFileName(record);
        String contentDisposition = ContentDisposition.attachment()
                .filename(fileName, StandardCharsets.UTF_8)
                .build()
                .toString();

        return ResponseEntity.ok()
                .contentType(mediaType)
                .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition)
                .body(resource);
    }

    private void ensureUploadAllowed(Long uploaderId) {
        if (uploaderId == null && !fileCodeBoxProperties.isOpenUpload()) {
            throw new BusinessException("AUTH_REQUIRED", "未开启游客上传，请先登录管理员账号");
        }
    }

    private MediaType resolveMediaType(String contentType) {
        if (contentType == null || contentType.isBlank()) {
            return MediaType.APPLICATION_OCTET_STREAM;
        }
        try {
            return MediaType.parseMediaType(contentType);
        } catch (InvalidMediaTypeException ex) {
            log.warn("FileCodeBox记录存在非法contentType: {}, fallback为application/octet-stream", contentType);
            return MediaType.APPLICATION_OCTET_STREAM;
        }
    }

    private String resolveDownloadFileName(PickupCodeRecord record) {
        String displayName = record.getDisplayName();
        if (displayName == null || displayName.isBlank()) {
            return "download.bin";
        }
        return displayName.replace("\r", "_").replace("\n", "_");
    }

    private Long resolveUploaderId(HttpServletRequest request) {
        if (!fileCodeBoxProperties.isOpenUpload()) {
            User user = userService.getCurrentUser(request);
            if (user.getRole() != User.UserRole.ADMIN) {
                throw new BusinessException("AUTH_REQUIRED", "未开启游客上传，请先登录管理员账号");
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
            // 游客上传模式下，忽略无效 token。
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
}
