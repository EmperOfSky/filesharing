package com.filesharing.controller;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.filesharing.config.FileCodeBoxProperties;
import com.filesharing.dto.UserLoginRequest;
import com.filesharing.entity.PickupCodeRecord;
import com.filesharing.entity.User;
import com.filesharing.exception.BusinessException;
import com.filesharing.repository.PickupCodeRecordRepository;
import com.filesharing.service.FileCodeBoxService;
import com.filesharing.service.UserService;
import com.filesharing.util.FileStorageUtil;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * old_python_example 后台兼容层（/api/admin/*）。
 */
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Validated
public class LegacyAdminCompatController {

    private static final Set<String> ALLOWED_EXPIRE_STYLES =
            new LinkedHashSet<>(Arrays.asList("day", "hour", "minute", "forever", "count"));

    private final UserService userService;
    private final FileCodeBoxService fileCodeBoxService;
    private final PickupCodeRecordRepository pickupCodeRecordRepository;
    private final FileCodeBoxProperties fileCodeBoxProperties;
    private final FileStorageUtil fileStorageUtil;

    private final LocalDateTime appStartTime = LocalDateTime.now();

    @PostMapping({"/login", "/login/"})
    public ResponseEntity<Map<String, Object>> login(@RequestBody LegacyAdminLoginRequest body) {
        String password = body == null ? null : body.getPassword();
        if (!StringUtils.hasText(password)) {
            throw new BusinessException("AUTH_FAILED", "密码不能为空");
        }

        List<String> identifiers = new ArrayList<>();
        if (StringUtils.hasText(body.getIdentifier())) {
            identifiers.add(body.getIdentifier().trim());
        }
        identifiers.add("admin");
        identifiers.add("administrator");
        identifiers.add("root");

        String token = null;
        for (String identifier : identifiers.stream().distinct().collect(Collectors.toList())) {
            try {
                token = userService.login(new UserLoginRequest(identifier, password));
                break;
            } catch (RuntimeException ignored) {
                // 尝试下一个候选账号。
            }
        }

        if (!StringUtils.hasText(token)) {
            throw new BusinessException("AUTH_FAILED", "密码错误");
        }

        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("token", token);
        detail.put("token_type", "Bearer");
        return legacyOk(detail);
    }

    @GetMapping({"/dashboard", "/dashboard/"})
    public ResponseEntity<Map<String, Object>> dashboard(HttpServletRequest request) {
        ensureAdmin(request);

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        LocalDateTime yesterdayStart = todayStart.minusDays(1);
        LocalDateTime yesterdayEnd = todayStart.minusNanos(1);

        long totalFiles = pickupCodeRecordRepository.count();
        long storageUsed = Optional.ofNullable(pickupCodeRecordRepository.sumAllSizeBytes()).orElse(0L);
        long yesterdayCount = pickupCodeRecordRepository.countByCreatedAtBetween(yesterdayStart, yesterdayEnd);
        long yesterdaySize = Optional.ofNullable(pickupCodeRecordRepository.sumSizeByCreatedAtBetween(yesterdayStart, yesterdayEnd)).orElse(0L);
        long todayCount = pickupCodeRecordRepository.countByCreatedAtGreaterThanEqual(todayStart);
        long todaySize = Optional.ofNullable(pickupCodeRecordRepository.sumSizeByCreatedAtGreaterThanEqual(todayStart)).orElse(0L);

        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("totalFiles", totalFiles);
        detail.put("storageUsed", String.valueOf(storageUsed));
        detail.put("sysUptime", appStartTime.toString());
        detail.put("yesterdayCount", yesterdayCount);
        detail.put("yesterdaySize", String.valueOf(yesterdaySize));
        detail.put("todayCount", todayCount);
        detail.put("todaySize", String.valueOf(todaySize));
        detail.put("serverTime", now.toString());

        return legacyOk(detail);
    }

    @GetMapping({"/file/list", "/file/list/"})
    public ResponseEntity<Map<String, Object>> fileList(@RequestParam(value = "page", defaultValue = "1") Integer page,
                                                        @RequestParam(value = "size", defaultValue = "10") Integer size,
                                                        @RequestParam(value = "keyword", required = false) String keyword,
                                                        HttpServletRequest request) {
        ensureAdmin(request);

        int safePage = page == null || page < 1 ? 1 : page;
        int safeSize = size == null || size < 1 ? 10 : Math.min(size, 200);

        Map<String, Object> records = fileCodeBoxService.listRecords(keyword, null, null, safePage - 1, safeSize);

        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("page", safePage);
        detail.put("size", safeSize);
        detail.put("data", records.get("content"));
        detail.put("total", records.get("totalElements"));
        return legacyOk(detail);
    }

    @DeleteMapping({"/file/delete", "/file/delete/"})
    public ResponseEntity<Map<String, Object>> deleteFile(@RequestBody LegacyIdRequest body,
                                                           HttpServletRequest request) {
        ensureAdmin(request);
        if (body == null || body.getId() == null) {
            throw new BusinessException("INVALID_REQUEST", "id 不能为空");
        }
        fileCodeBoxService.deleteRecord(body.getId());
        return legacyOk(null);
    }

    @PatchMapping({"/file/update", "/file/update/"})
    public ResponseEntity<Map<String, Object>> updateFile(@RequestBody Map<String, Object> body,
                                                           HttpServletRequest request) {
        ensureAdmin(request);

        Long id = toLong(body.get("id"));
        if (id == null) {
            throw new BusinessException("INVALID_REQUEST", "id 不能为空");
        }

        PickupCodeRecord record = fileCodeBoxService.getRecordById(id);

        if (body.containsKey("code") && StringUtils.hasText(toText(body.get("code")))) {
            String newCode = toText(body.get("code")).trim();
            if (!newCode.equals(record.getCode()) && pickupCodeRecordRepository.existsByCode(newCode)) {
                throw new BusinessException("CODE_EXISTS", "code已存在");
            }
            record.setCode(newCode);
        }

        String currentName = Optional.ofNullable(record.getDisplayName()).orElse("");
        String[] nameParts = splitName(currentName);
        String prefix = nameParts[0];
        String suffix = nameParts[1];

        if (body.containsKey("prefix") && body.get("prefix") != null) {
            prefix = toText(body.get("prefix"));
        }
        if (body.containsKey("suffix") && body.get("suffix") != null) {
            suffix = toText(body.get("suffix"));
        }
        if (!prefix.isBlank() || !suffix.isBlank()) {
            record.setDisplayName(prefix + suffix);
        }

        if (body.containsKey("expired_count") && body.get("expired_count") != null) {
            Integer expiredCount = toInteger(body.get("expired_count"));
            record.setExpiredCount(expiredCount == null ? -1 : expiredCount);
        }

        if (body.containsKey("expired_at")) {
            LocalDateTime expiredAt = parseDateTime(body.get("expired_at"));
            record.setExpireAt(expiredAt);
        }

        pickupCodeRecordRepository.save(record);
        return legacyOk("更新成功");
    }

    @GetMapping({"/file/download", "/file/download/"})
    public ResponseEntity<?> downloadFile(@RequestParam("id") Long id,
                                          HttpServletRequest request) {
        ensureAdmin(request);

        PickupCodeRecord record = fileCodeBoxService.getRecordById(id);
        if (record.getShareType() == PickupCodeRecord.ShareType.TEXT) {
            return ResponseEntity.ok(legacyBody(200, "ok", record.getTextContent()));
        }

        if (record.getStorageMode() == PickupCodeRecord.StorageMode.CLOUD_DIRECT) {
            String signedUrl = fileCodeBoxService.buildCloudDownloadUrl(record);
            if (!StringUtils.hasText(signedUrl)) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(legacyBody(404, "文件不存在", null));
            }
            return ResponseEntity.status(HttpStatus.FOUND)
                    .header(HttpHeaders.LOCATION, signedUrl)
                    .build();
        }

        Resource resource = fileCodeBoxService.loadLocalResource(record);
        if (resource == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(legacyBody(404, "文件不存在", null));
        }

        String contentType = StringUtils.hasText(record.getContentType())
                ? record.getContentType()
                : MediaType.APPLICATION_OCTET_STREAM_VALUE;

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + record.getDisplayName() + "\"")
                .body(resource);
    }

    @GetMapping({"/config/get", "/config/get/"})
    public ResponseEntity<Map<String, Object>> getConfig(HttpServletRequest request) {
        ensureAdmin(request);
        return legacyOk(buildConfig());
    }

    @PatchMapping({"/config/update", "/config/update/"})
    public ResponseEntity<Map<String, Object>> updateConfig(@RequestBody Map<String, Object> body,
                                                             HttpServletRequest request) {
        ensureAdmin(request);

        synchronized (fileCodeBoxProperties) {
            applyBoolean(body, "open_upload", "openUpload", fileCodeBoxProperties::setOpenUpload);
            applyLong(body, "upload_size", "uploadSize", fileCodeBoxProperties::setUploadSize);
            applyInteger(body, "upload_count", "uploadCount", fileCodeBoxProperties::setUploadCount);
            applyInteger(body, "upload_minute", "uploadMinute", fileCodeBoxProperties::setUploadMinute);
            applyInteger(body, "error_count", "errorCount", fileCodeBoxProperties::setErrorCount);
            applyInteger(body, "error_minute", "errorMinute", fileCodeBoxProperties::setErrorMinute);
            applyInteger(body, "max_save_seconds", "maxSaveSeconds", fileCodeBoxProperties::setMaxSaveSeconds);
            applyInteger(body, "presign_expire_seconds", "presignExpireSeconds", fileCodeBoxProperties::setPresignExpireSeconds);
            applyInteger(body, "download_token_ttl_seconds", "downloadTokenTtlSeconds", fileCodeBoxProperties::setDownloadTokenTtlSeconds);

            List<String> styles = resolveExpireStyles(body);
            if (!styles.isEmpty()) {
                fileCodeBoxProperties.setExpireStyles(styles);
            }
        }

        return legacyOk(null);
    }

    @GetMapping({"/local/lists", "/local/lists/"})
    public ResponseEntity<Map<String, Object>> localLists(HttpServletRequest request) throws Exception {
        ensureAdmin(request);

        Path root = Paths.get(fileStorageUtil.getUploadPath()).toAbsolutePath().normalize();
        if (!Files.exists(root)) {
            return legacyOk(new ArrayList<>());
        }

        List<Map<String, Object>> files = new ArrayList<>();
        Files.walk(root)
                .filter(Files::isRegularFile)
                .forEach(path -> {
                    try {
                        Map<String, Object> item = new LinkedHashMap<>();
                        item.put("filename", root.relativize(path).toString().replace('\\', '/'));
                        item.put("size", Files.size(path));
                        item.put("modified_at", Files.getLastModifiedTime(path).toInstant().toString());
                        files.add(item);
                    } catch (Exception ignored) {
                        // 跳过异常文件，避免影响整个列表。
                    }
                });

        return legacyOk(files);
    }

    @DeleteMapping({"/local/delete", "/local/delete/"})
    public ResponseEntity<Map<String, Object>> deleteLocal(@RequestBody LegacyDeleteLocalRequest body,
                                                            HttpServletRequest request) throws Exception {
        ensureAdmin(request);

        if (body == null || !StringUtils.hasText(body.getFilename())) {
            throw new BusinessException("INVALID_REQUEST", "filename 不能为空");
        }

        Path root = Paths.get(fileStorageUtil.getUploadPath()).toAbsolutePath().normalize();
        Path target = root.resolve(body.getFilename()).normalize();
        if (!target.startsWith(root)) {
            throw new BusinessException("INVALID_PATH", "非法文件路径");
        }

        boolean deleted = Files.deleteIfExists(target);
        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("filename", body.getFilename());
        detail.put("deleted", deleted);
        return legacyOk(detail);
    }

    @PostMapping({"/local/share", "/local/share/"})
    public ResponseEntity<Map<String, Object>> shareLocal(@RequestBody LegacyShareLocalRequest body,
                                                           HttpServletRequest request) throws Exception {
        User admin = ensureAdmin(request);

        if (body == null || !StringUtils.hasText(body.getFilename())) {
            throw new BusinessException("INVALID_REQUEST", "filename 不能为空");
        }

        Path root = Paths.get(fileStorageUtil.getUploadPath()).toAbsolutePath().normalize();
        Path target = root.resolve(body.getFilename()).normalize();
        if (!target.startsWith(root)) {
            throw new BusinessException("INVALID_PATH", "非法文件路径");
        }
        if (!Files.exists(target) || !Files.isRegularFile(target)) {
            throw new BusinessException("NOT_FOUND", "文件不存在");
        }

        String relative = root.relativize(target).toString().replace('\\', '/');
        String fileName = target.getFileName().toString();
        long size = Files.size(target);
        String contentType = Optional.ofNullable(Files.probeContentType(target)).orElse(MediaType.APPLICATION_OCTET_STREAM_VALUE);

        Map<String, Object> detail = fileCodeBoxService.createLocalFileShareRecord(
                fileName,
                contentType,
                size,
                relative,
                body.getExpireValue(),
                body.getExpireStyle(),
                resolveClientIp(request),
                admin.getId()
        );

        return legacyOk(detail);
    }

    private User ensureAdmin(HttpServletRequest request) {
        User user = userService.getCurrentUser(request);
        if (user.getRole() != User.UserRole.ADMIN) {
            throw new BusinessException("FORBIDDEN", "仅管理员可操作该接口");
        }
        return user;
    }

    private Map<String, Object> buildConfig() {
        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("openUpload", fileCodeBoxProperties.isOpenUpload());
        detail.put("uploadSize", fileCodeBoxProperties.getUploadSize());
        detail.put("uploadCount", fileCodeBoxProperties.getUploadCount());
        detail.put("uploadMinute", fileCodeBoxProperties.getUploadMinute());
        detail.put("errorCount", fileCodeBoxProperties.getErrorCount());
        detail.put("errorMinute", fileCodeBoxProperties.getErrorMinute());
        detail.put("maxSaveSeconds", fileCodeBoxProperties.getMaxSaveSeconds());
        detail.put("expireStyle", fileCodeBoxProperties.getExpireStyles());
        detail.put("presignExpireSeconds", fileCodeBoxProperties.getPresignExpireSeconds());
        detail.put("downloadTokenTtlSeconds", fileCodeBoxProperties.getDownloadTokenTtlSeconds());
        detail.put("themesChoices", Collections.emptyList());
        return detail;
    }

    private List<String> resolveExpireStyles(Map<String, Object> body) {
        Object value = pick(body, "expire_styles", "expireStyle", "expireStyles");
        if (value == null) {
            return Collections.emptyList();
        }

        List<String> source = new ArrayList<>();
        if (value instanceof Collection<?>) {
            for (Object item : (Collection<?>) value) {
                if (item != null) {
                    source.add(String.valueOf(item));
                }
            }
        } else {
            source.addAll(Arrays.asList(String.valueOf(value).split(",")));
        }

        LinkedHashSet<String> normalized = new LinkedHashSet<>();
        for (String style : source) {
            if (!StringUtils.hasText(style)) {
                continue;
            }
            String lower = style.trim().toLowerCase(Locale.ROOT);
            if (!ALLOWED_EXPIRE_STYLES.contains(lower)) {
                throw new BusinessException("INVALID_EXPIRE_STYLE", "不支持的过期策略: " + lower);
            }
            normalized.add(lower);
        }

        return new ArrayList<>(normalized);
    }

    private String resolveClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (StringUtils.hasText(forwarded)) {
            return forwarded.split(",")[0].trim();
        }
        String realIp = request.getHeader("X-Real-IP");
        if (StringUtils.hasText(realIp)) {
            return realIp.trim();
        }
        return request.getRemoteAddr();
    }

    private void applyBoolean(Map<String, Object> body, String snakeKey, String camelKey, java.util.function.Consumer<Boolean> setter) {
        Object value = pick(body, snakeKey, camelKey);
        if (value != null) {
            setter.accept(Boolean.parseBoolean(String.valueOf(value)));
        }
    }

    private void applyInteger(Map<String, Object> body, String snakeKey, String camelKey, java.util.function.Consumer<Integer> setter) {
        Object value = pick(body, snakeKey, camelKey);
        if (value != null) {
            Integer result = toInteger(value);
            if (result != null) {
                setter.accept(result);
            }
        }
    }

    private void applyLong(Map<String, Object> body, String snakeKey, String camelKey, java.util.function.Consumer<Long> setter) {
        Object value = pick(body, snakeKey, camelKey);
        if (value != null) {
            Long result = toLong(value);
            if (result != null) {
                setter.accept(result);
            }
        }
    }

    private Object pick(Map<String, Object> body, String... keys) {
        if (body == null) {
            return null;
        }
        for (String key : keys) {
            if (body.containsKey(key)) {
                return body.get(key);
            }
        }
        return null;
    }

    private String toText(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private Integer toInteger(Object value) {
        try {
            if (value == null || !StringUtils.hasText(String.valueOf(value))) {
                return null;
            }
            return Integer.parseInt(String.valueOf(value).trim());
        } catch (NumberFormatException ex) {
            throw new BusinessException("INVALID_NUMBER", "数值格式错误: " + value);
        }
    }

    private Long toLong(Object value) {
        try {
            if (value == null || !StringUtils.hasText(String.valueOf(value))) {
                return null;
            }
            return Long.parseLong(String.valueOf(value).trim());
        } catch (NumberFormatException ex) {
            throw new BusinessException("INVALID_NUMBER", "数值格式错误: " + value);
        }
    }

    private LocalDateTime parseDateTime(Object value) {
        if (value == null) {
            return null;
        }

        String text = String.valueOf(value).trim();
        if (text.isEmpty()) {
            return null;
        }

        List<DateTimeFormatter> formatters = Arrays.asList(
                DateTimeFormatter.ISO_DATE_TIME,
                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
                DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")
        );

        for (DateTimeFormatter formatter : formatters) {
            try {
                return LocalDateTime.parse(text, formatter);
            } catch (DateTimeParseException ignored) {
                // 尝试下一个格式。
            }
        }

        throw new BusinessException("INVALID_DATETIME", "时间格式错误: " + text);
    }

    private String[] splitName(String fullName) {
        if (!StringUtils.hasText(fullName)) {
            return new String[]{"", ""};
        }
        int dot = fullName.lastIndexOf('.');
        if (dot <= 0) {
            return new String[]{fullName, ""};
        }
        return new String[]{fullName.substring(0, dot), fullName.substring(dot)};
    }

    private ResponseEntity<Map<String, Object>> legacyOk(Object detail) {
        return ResponseEntity.ok(legacyBody(200, "ok", detail));
    }

    private Map<String, Object> legacyBody(int code, String message, Object detail) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("code", code);
        body.put("message", message);
        body.put("detail", detail);
        return body;
    }

    @Data
    private static class LegacyAdminLoginRequest {
        private String identifier;
        private String password;
    }

    @Data
    private static class LegacyIdRequest {
        private Long id;
    }

    @Data
    private static class LegacyDeleteLocalRequest {
        private String filename;
    }

    @Data
    private static class LegacyShareLocalRequest {
        private String filename;

        @JsonProperty("expire_value")
        private Integer expireValue = 1;

        @JsonProperty("expire_style")
        private String expireStyle = "day";
    }
}
