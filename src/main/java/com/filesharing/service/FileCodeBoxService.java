package com.filesharing.service;

import com.filesharing.config.FileCodeBoxProperties;
import com.filesharing.entity.PickupCodeRecord;
import com.filesharing.exception.BusinessException;
import com.filesharing.repository.PickupCodeRecordRepository;
import com.filesharing.security.FileCodeBoxSecurityService;
import com.filesharing.security.FileUploadSecurityService;
import com.filesharing.util.FileStorageUtil;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * FileCodeBox 兼容业务服务。
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class FileCodeBoxService {

    private static final String PICKUP_CODE_CHARS = "0123456789";
    private static final int PICKUP_CODE_LENGTH = 8;
    private static final int FIXED_LINK_EXPIRE_HOURS = 1;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final PickupCodeRecordRepository pickupCodeRecordRepository;
    private final FileCodeBoxProperties properties;
    private final FileCodeBoxSecurityService securityService;
    private final FileUploadSecurityService fileUploadSecurityService;
    private final FileCodeBoxStorageService storageService;
    private final FileStorageUtil fileStorageUtil;

    private final Map<String, PresignSession> presignSessions = new ConcurrentHashMap<>();
    private final Map<String, ChunkUploadSession> chunkUploadSessions = new ConcurrentHashMap<>();
    private final Map<String, DownloadGrant> downloadGrants = new ConcurrentHashMap<>();

    public Map<String, Object> shareText(String text,
                                         Integer expireValue,
                                         String expireStyle,
                                         String ip,
                                         Long creatorUserId) {
        if (text == null || text.isBlank()) {
            throw new BusinessException("TEXT_EMPTY", "文本内容不能为空");
        }

        int textBytes = text.getBytes(StandardCharsets.UTF_8).length;
        if (textBytes > 222 * 1024) {
            throw new BusinessException("TEXT_TOO_LARGE", "内容过大，请改用文件上传");
        }

        securityService.ensureUploadAllowed(ip);
        ExpirePolicy policy = resolveExpirePolicy(expireValue, expireStyle);

        PickupCodeRecord record = new PickupCodeRecord();
        record.setCode(generateUniqueCode(policy.style));
        record.setShareType(PickupCodeRecord.ShareType.TEXT);
        record.setTextContent(text);
        record.setDisplayName("Text");
        record.setContentType("text/plain;charset=UTF-8");
        record.setSizeBytes((long) textBytes);
        record.setExpireAt(policy.expireAt);
        record.setExpiredCount(policy.expiredCount);
        record.setUsedCount(0);
        record.setCreatedIp(ip);
        record.setCreatorUserId(creatorUserId);
        record.setStatus(PickupCodeRecord.ShareStatus.ACTIVE);

        pickupCodeRecordRepository.save(record);
        securityService.markUpload(ip);

        Map<String, Object> detail = new HashMap<>();
        detail.put("code", record.getCode());
        return detail;
    }

    public Map<String, Object> shareFile(MultipartFile file,
                                         Integer expireValue,
                                         String expireStyle,
                                         String ip,
                                         Long creatorUserId) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("FILE_EMPTY", "未检测到上传文件");
        }

        if (file.getSize() > properties.getUploadSize()) {
            throw new BusinessException("FILE_TOO_LARGE", "文件大小超过限制");
        }

        fileUploadSecurityService.validateAndScan(file);

        securityService.ensureUploadAllowed(ip);
        ExpirePolicy policy = resolveExpirePolicy(expireValue, expireStyle);

        String originalName = storageService.sanitizeFileName(file.getOriginalFilename());
        String relativePath = storageService.buildRelativePath(originalName);
        storageService.saveLocalFile(file, relativePath);

        PickupCodeRecord record = buildFileRecord(
                generateUniqueCode(policy.style),
                originalName,
                file.getContentType(),
                file.getSize(),
                policy,
                ip,
                creatorUserId,
                PickupCodeRecord.StorageMode.LOCAL,
                relativePath,
                null
        );

        pickupCodeRecordRepository.save(record);
        securityService.markUpload(ip);

        Map<String, Object> detail = new HashMap<>();
        detail.put("code", record.getCode());
        detail.put("name", record.getDisplayName());
        return detail;
    }

    public Map<String, Object> initPresignUpload(String fileName,
                                                  Long fileSize,
                                                  Integer expireValue,
                                                  String expireStyle,
                                                  String ip,
                                                  Long creatorUserId) {
        if (fileName == null || fileName.isBlank()) {
            throw new BusinessException("INVALID_FILE_NAME", "file_name 不能为空");
        }
        if (fileSize == null || fileSize <= 0) {
            throw new BusinessException("INVALID_FILE_SIZE", "file_size 必须大于0");
        }
        if (fileSize > properties.getUploadSize()) {
            throw new BusinessException("FILE_TOO_LARGE", "文件大小超过限制");
        }

        securityService.ensureUploadAllowed(ip);
        ExpirePolicy policy = resolveExpirePolicy(expireValue, expireStyle);

        String uploadId = UUID.randomUUID().toString().replace("-", "");
        String safeName = storageService.sanitizeFileName(fileName);
        String relativePath = storageService.buildRelativePath(safeName);

        FileCodeBoxStorageService.PresignTarget target =
                storageService.resolvePresignTarget(relativePath, properties.getPresignExpireSeconds());

        LocalDateTime now = LocalDateTime.now();
        PresignSession session = new PresignSession(
                uploadId,
                safeName,
                fileSize,
                relativePath,
                target.getMode(),
                target.getCloudKey(),
                target.getCloudConfigId(),
                now,
                now.plusSeconds(properties.getPresignExpireSeconds()),
                policy.style,
                policy.expireAt,
                policy.expiredCount,
                creatorUserId
        );

        presignSessions.put(uploadId, session);

        Map<String, Object> detail = new HashMap<>();
        detail.put("upload_id", uploadId);
        detail.put("upload_url", target.getMode() == FileCodeBoxStorageService.PresignTarget.Mode.DIRECT
                ? target.getUploadUrl()
                : "/api/public/presign/upload/proxy/" + uploadId);
        detail.put("mode", target.getMode() == FileCodeBoxStorageService.PresignTarget.Mode.DIRECT ? "direct" : "proxy");
        detail.put("save_path", relativePath);
        detail.put("expires_in", properties.getPresignExpireSeconds());
        return detail;
    }

    public Map<String, Object> uploadPresignProxy(String uploadId, MultipartFile file, String ip) throws IOException {
        PresignSession session = getValidPresignSession(uploadId);
        if (session.mode != FileCodeBoxStorageService.PresignTarget.Mode.PROXY) {
            throw new BusinessException("INVALID_UPLOAD_MODE", "当前会话不支持代理上传");
        }

        if (file == null || file.isEmpty()) {
            throw new BusinessException("FILE_EMPTY", "未检测到上传文件");
        }

        if (file.getSize() > properties.getUploadSize()) {
            throw new BusinessException("FILE_TOO_LARGE", "文件大小超过限制");
        }

        if (file.getSize() != session.fileSize) {
            throw new BusinessException("FILE_SIZE_MISMATCH", "文件大小与初始化声明不一致");
        }

        fileUploadSecurityService.validateAndScan(file);

        storageService.saveLocalFile(file, session.relativePath);
        session.uploaded = true;
        session.contentType = file.getContentType();

        if (session.generatedCode == null) {
            session.generatedCode = createRecordFromSession(session, PickupCodeRecord.StorageMode.LOCAL, session.relativePath, null);
        }
        session.confirmed = true;

        securityService.markUpload(ip);

        Map<String, Object> detail = new HashMap<>();
        detail.put("code", session.generatedCode);
        detail.put("name", session.fileName);
        return detail;
    }

    public Map<String, Object> confirmPresignUpload(String uploadId, Integer expireValue, String expireStyle) {
        PresignSession session = getValidPresignSession(uploadId);
        if (session.mode != FileCodeBoxStorageService.PresignTarget.Mode.DIRECT) {
            throw new BusinessException("INVALID_UPLOAD_MODE", "当前会话不支持直传确认");
        }

        if (session.generatedCode == null) {
            if (expireValue != null || (expireStyle != null && !expireStyle.isBlank())) {
                ExpirePolicy overridePolicy = resolveExpirePolicy(
                        expireValue == null ? 1 : expireValue,
                        expireStyle == null ? session.expireStyle : expireStyle
                );
                session.expireStyle = overridePolicy.style;
                session.expireAt = overridePolicy.expireAt;
                session.expiredCount = overridePolicy.expiredCount;
            }

            session.generatedCode = createRecordFromSession(
                    session,
                    PickupCodeRecord.StorageMode.CLOUD_DIRECT,
                    session.cloudKey,
                    session.cloudConfigId
            );
        }

        session.confirmed = true;

        Map<String, Object> detail = new HashMap<>();
        detail.put("code", session.generatedCode);
        detail.put("name", session.fileName);
        return detail;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> listRecords(String keyword,
                                           String status,
                                           String shareType,
                                           Integer page,
                                           Integer size) {
        PickupCodeRecord.ShareStatus statusEnum = parseShareStatus(status);
        PickupCodeRecord.ShareType shareTypeEnum = parseShareType(shareType);

        int safePage = page == null || page < 0 ? 0 : page;
        int safeSize = size == null ? 20 : Math.max(1, Math.min(size, 200));

        Page<PickupCodeRecord> recordPage = pickupCodeRecordRepository.searchRecords(
                keyword,
                statusEnum,
                shareTypeEnum,
                PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.DESC, "createdAt"))
        );

        LocalDateTime now = LocalDateTime.now();
        List<Map<String, Object>> content = new ArrayList<>();
        for (PickupCodeRecord record : recordPage.getContent()) {
            content.add(toRecordPayload(record, now));
        }

        Map<String, Object> detail = new HashMap<>();
        detail.put("content", content);
        detail.put("number", recordPage.getNumber());
        detail.put("size", recordPage.getSize());
        detail.put("totalElements", recordPage.getTotalElements());
        detail.put("totalPages", recordPage.getTotalPages());
        detail.put("first", recordPage.isFirst());
        detail.put("last", recordPage.isLast());
        return detail;
    }

    public Map<String, Object> updateRecordStatus(Long recordId, String status) {
        PickupCodeRecord record = pickupCodeRecordRepository.findById(recordId)
                .orElseThrow(() -> new BusinessException("NOT_FOUND", "记录不存在"));

        PickupCodeRecord.ShareStatus targetStatus = parseShareStatus(status);
        if (targetStatus == null) {
            throw new BusinessException("INVALID_STATUS", "状态不能为空");
        }
        if (targetStatus == PickupCodeRecord.ShareStatus.EXPIRED) {
            throw new BusinessException("INVALID_STATUS", "不支持手动设置为 EXPIRED");
        }

        if (targetStatus == PickupCodeRecord.ShareStatus.ACTIVE && isExpired(record, LocalDateTime.now())) {
            throw new BusinessException("EXPIRED_RECORD", "记录已过期，无法重新启用");
        }

        record.setStatus(targetStatus);
        PickupCodeRecord saved = pickupCodeRecordRepository.save(record);
        return toRecordPayload(saved, LocalDateTime.now());
    }

    public Map<String, Object> initChunkUpload(String fileName,
                                               Long fileSize,
                                               String fileHash,
                                               Integer chunkSize,
                                               String expireStyle,
                                               Integer expireValue,
                                               String ip,
                                               Long creatorUserId) {
        if (fileName == null || fileName.isBlank()) {
            throw new BusinessException("INVALID_FILE_NAME", "file_name 不能为空");
        }
        if (fileSize == null || fileSize <= 0) {
            throw new BusinessException("INVALID_FILE_SIZE", "file_size 必须大于0");
        }
        if (fileSize > properties.getUploadSize()) {
            throw new BusinessException("FILE_TOO_LARGE", "文件大小超过限制");
        }

        int safeChunkSize = chunkSize == null || chunkSize <= 0 ? 5 * 1024 * 1024 : chunkSize;
        if (safeChunkSize > properties.getUploadSize()) {
            safeChunkSize = (int) Math.min(properties.getUploadSize(), Integer.MAX_VALUE);
        }

        int totalChunks = (int) ((fileSize + safeChunkSize - 1) / safeChunkSize);
        if (totalChunks <= 0) {
            throw new BusinessException("INVALID_CHUNK_CONFIG", "无效的分片参数");
        }

        long maxPossibleSize = (long) totalChunks * safeChunkSize;
        if (maxPossibleSize > properties.getUploadSize()) {
            throw new BusinessException("FILE_TOO_LARGE", "文件大小超过限制");
        }

        securityService.ensureUploadAllowed(ip);
        ExpirePolicy policy = resolveExpirePolicy(expireValue == null ? 1 : expireValue,
                expireStyle == null ? "day" : expireStyle);

        String safeName = storageService.sanitizeFileName(fileName);
        String normalizedFileHash = fileHash == null ? null : fileHash.trim().toLowerCase(Locale.ROOT);
        ChunkUploadSession resumable = findResumableChunkSession(safeName, fileSize, safeChunkSize, normalizedFileHash);
        if (resumable != null) {
            return buildChunkInitPayload(resumable);
        }

        String uploadId = UUID.randomUUID().toString().replace("-", "");
        String relativePath = storageService.buildRelativePath(safeName);
        LocalDateTime now = LocalDateTime.now();

        ChunkUploadSession session = new ChunkUploadSession(
                uploadId,
                safeName,
                normalizedFileHash,
                fileSize,
                safeChunkSize,
                totalChunks,
                relativePath,
                policy.style,
                policy.expireAt,
                policy.expiredCount,
                creatorUserId,
                now,
                now.plusHours(24)
        );

        chunkUploadSessions.put(uploadId, session);
        securityService.markUpload(ip);
        return buildChunkInitPayload(session);
    }

    public Map<String, Object> uploadChunk(String uploadId,
                                           Integer chunkIndex,
                                           MultipartFile chunk) throws IOException {
        ChunkUploadSession session = getValidChunkSession(uploadId);

        if (chunk == null || chunk.isEmpty()) {
            throw new BusinessException("CHUNK_EMPTY", "分片不能为空");
        }
        if (chunkIndex == null || chunkIndex < 0 || chunkIndex >= session.totalChunks) {
            throw new BusinessException("INVALID_CHUNK_INDEX", "无效的分片索引");
        }
        if (chunk.getSize() > session.chunkSize) {
            throw new BusinessException("CHUNK_TOO_LARGE", "分片大小超过限制");
        }

        Path chunkDir = getChunkTempDir(uploadId);
        Files.createDirectories(chunkDir);

        Path chunkPath = chunkDir.resolve("part_" + chunkIndex + ".part").normalize();
        if (!chunkPath.startsWith(chunkDir)) {
            throw new BusinessException("INVALID_PATH", "非法分片路径");
        }

        if (session.uploadedChunks.contains(chunkIndex) && Files.exists(chunkPath)) {
            byte[] existing = Files.readAllBytes(chunkPath);
            Map<String, Object> detail = new HashMap<>();
            detail.put("chunk_hash", sha256Hex(existing));
            detail.put("skipped", true);
            return detail;
        }

        byte[] bytes = chunk.getBytes();
        Files.write(chunkPath, bytes);
        session.uploadedChunks.add(chunkIndex);

        Map<String, Object> detail = new HashMap<>();
        detail.put("chunk_hash", sha256Hex(bytes));
        return detail;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getChunkUploadStatus(String uploadId) {
        ChunkUploadSession session = getValidChunkSession(uploadId);

        List<Integer> uploaded = new ArrayList<>(session.uploadedChunks);
        Collections.sort(uploaded);

        double progress = session.totalChunks == 0 ? 0D : ((double) uploaded.size() / session.totalChunks) * 100D;
        Map<String, Object> detail = new HashMap<>();
        detail.put("upload_id", uploadId);
        detail.put("file_name", session.fileName);
        detail.put("file_size", session.fileSize);
        detail.put("chunk_size", session.chunkSize);
        detail.put("total_chunks", session.totalChunks);
        detail.put("uploaded_chunks", uploaded);
        detail.put("progress", progress);
        return detail;
    }

    public Map<String, Object> completeChunkUpload(String uploadId,
                                                   Integer expireValue,
                                                   String expireStyle,
                                                   String ip) throws IOException {
        ChunkUploadSession session = getValidChunkSession(uploadId);

        if (session.uploadedChunks.size() != session.totalChunks) {
            throw new BusinessException("CHUNK_INCOMPLETE", "分片不完整");
        }

        if (expireValue != null || (expireStyle != null && !expireStyle.isBlank())) {
            ExpirePolicy overridePolicy = resolveExpirePolicy(
                    expireValue == null ? 1 : expireValue,
                    expireStyle == null ? session.expireStyle : expireStyle
            );
            session.expireStyle = overridePolicy.style;
            session.expireAt = overridePolicy.expireAt;
            session.expiredCount = overridePolicy.expiredCount;
        }

        Path chunkDir = getChunkTempDir(uploadId);
        byte[] mergedBytes;
        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            for (int i = 0; i < session.totalChunks; i++) {
                Path chunkPath = chunkDir.resolve("part_" + i + ".part").normalize();
                if (!chunkPath.startsWith(chunkDir) || !Files.exists(chunkPath)) {
                    throw new BusinessException("CHUNK_MISSING", "分片缺失: " + i);
                }
                Files.copy(chunkPath, output);
            }
            mergedBytes = output.toByteArray();
        }

        String contentType = detectContentType(session.fileName);
        fileUploadSecurityService.validateAndScan(
            session.fileName,
            mergedBytes.length,
            contentType,
            new ByteArrayInputStream(mergedBytes)
        );

        try (ByteArrayInputStream inputStream = new ByteArrayInputStream(mergedBytes)) {
            fileStorageUtil.saveInputStreamAtPath(
                    inputStream,
                    mergedBytes.length,
                    session.relativePath,
                    contentType
            );
        }

        Map<String, Object> detail = createLocalFileShareRecord(
                session.fileName,
                contentType,
                mergedBytes.length,
                session.relativePath,
                session.expireStyle,
                session.expireAt,
                session.expiredCount,
                ip,
                session.creatorUserId
        );

        cleanupChunkFiles(uploadId);
        chunkUploadSessions.remove(uploadId);
        securityService.markUpload(ip);
        return detail;
    }

    public void cancelChunkUpload(String uploadId) {
        getValidChunkSession(uploadId);
        cleanupChunkFiles(uploadId);
        chunkUploadSessions.remove(uploadId);
    }

    public Map<String, Object> createLocalFileShareRecord(String fileName,
                                                           String contentType,
                                                           long fileSize,
                                                           String relativePath,
                                                           Integer expireValue,
                                                           String expireStyle,
                                                           String ip,
                                                           Long creatorUserId) {
        ExpirePolicy policy = resolveExpirePolicy(expireValue == null ? 1 : expireValue,
                expireStyle == null ? "day" : expireStyle);
        return createLocalFileShareRecord(fileName, contentType, fileSize, relativePath,
                policy.style, policy.expireAt, policy.expiredCount, ip, creatorUserId);
    }

    @Transactional(readOnly = true)
    public CsvExportResult exportRecordsCsv(String keyword, String status, String shareType) {
        PickupCodeRecord.ShareStatus statusEnum = parseShareStatus(status);
        PickupCodeRecord.ShareType shareTypeEnum = parseShareType(shareType);

        List<PickupCodeRecord> records = pickupCodeRecordRepository.searchRecordsForExport(keyword, statusEnum, shareTypeEnum);
        StringBuilder csv = new StringBuilder();
        csv.append('\uFEFF');
        csv.append("ID,取件码,名称,类型,状态,大小(bytes),已用次数,剩余次数,过期时间,创建时间,创建IP,创建人ID\n");

        LocalDateTime now = LocalDateTime.now();
        for (PickupCodeRecord record : records) {
            int expiredCount = record.getExpiredCount() == null ? -1 : record.getExpiredCount();
            int usedCount = record.getUsedCount() == null ? 0 : record.getUsedCount();
            String remainCount = expiredCount >= 0 ? String.valueOf(Math.max(expiredCount - usedCount, 0)) : "不限";

            csv.append(csvEscape(record.getId())).append(',')
                    .append(csvEscape(record.getCode())).append(',')
                    .append(csvEscape(record.getDisplayName())).append(',')
                    .append(csvEscape(record.getShareType() == null ? "" : record.getShareType().name())).append(',')
                    .append(csvEscape(resolveStatusForExport(record, now))).append(',')
                    .append(csvEscape(record.getSizeBytes() == null ? 0 : record.getSizeBytes())).append(',')
                    .append(csvEscape(usedCount)).append(',')
                    .append(csvEscape(remainCount)).append(',')
                    .append(csvEscape(formatDateTime(record.getExpireAt()))).append(',')
                    .append(csvEscape(formatDateTime(record.getCreatedAt()))).append(',')
                    .append(csvEscape(record.getCreatedIp())).append(',')
                    .append(csvEscape(record.getCreatorUserId()))
                    .append('\n');
        }

        String fileName = "fcb_records_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".csv";
        return new CsvExportResult(fileName, csv.toString(), records.size());
    }

    public void deleteRecord(Long recordId) {
        PickupCodeRecord record = pickupCodeRecordRepository.findById(recordId)
                .orElseThrow(() -> new BusinessException("NOT_FOUND", "记录不存在"));

        if (record.getShareType() == PickupCodeRecord.ShareType.FILE
                && record.getStorageMode() == PickupCodeRecord.StorageMode.LOCAL
                && record.getStoragePath() != null
                && !record.getStoragePath().isBlank()) {
            storageService.deleteLocalQuietly(record.getStoragePath());
        }

        pickupCodeRecordRepository.delete(record);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getPresignStatus(String uploadId) {
        PresignSession session = presignSessions.get(uploadId);
        if (session == null) {
            throw new BusinessException("UPLOAD_SESSION_NOT_FOUND", "上传会话不存在或已过期");
        }

        Map<String, Object> detail = new HashMap<>();
        detail.put("upload_id", session.uploadId);
        detail.put("file_name", session.fileName);
        detail.put("file_size", session.fileSize);
        detail.put("mode", session.mode == FileCodeBoxStorageService.PresignTarget.Mode.DIRECT ? "direct" : "proxy");
        detail.put("created_at", session.createdAt.toString());
        detail.put("expires_at", session.expiresAt.toString());
        detail.put("is_expired", LocalDateTime.now().isAfter(session.expiresAt));
        detail.put("uploaded", session.uploaded);
        detail.put("confirmed", session.confirmed);
        return detail;
    }

    public void cancelPresignUpload(String uploadId) {
        PresignSession session = presignSessions.remove(uploadId);
        if (session == null) {
            throw new BusinessException("UPLOAD_SESSION_NOT_FOUND", "上传会话不存在或已过期");
        }

        if (session.mode == FileCodeBoxStorageService.PresignTarget.Mode.PROXY
                && session.uploaded
                && session.relativePath != null
                && !session.relativePath.isBlank()) {
            storageService.deleteLocalQuietly(session.relativePath);
        }
    }

    public Map<String, Object> selectByCode(String code, String ip) {
        try {
            securityService.ensureSelectAllowed(ip);
            PickupCodeRecord record = findRecordForAccess(code, true);
            securityService.clearSelectError(ip);
            return buildSelectPayload(record);
        } catch (BusinessException e) {
            securityService.markSelectError(ip);
            throw e;
        }
    }

    public PickupCodeRecord directFetchByCode(String code, String ip) {
        try {
            securityService.ensureSelectAllowed(ip);
            PickupCodeRecord record = findRecordForAccess(code, true);
            securityService.clearSelectError(ip);
            return record;
        } catch (BusinessException e) {
            securityService.markSelectError(ip);
            throw e;
        }
    }

    public PickupCodeRecord validateDownload(String key, String code, String ip) {
        try {
            securityService.ensureSelectAllowed(ip);

            if (key == null || key.isBlank()) {
                throw new BusinessException("INVALID_DOWNLOAD_KEY", "下载鉴权失败");
            }

            DownloadGrant grant = downloadGrants.remove(key);
            if (grant == null || LocalDateTime.now().isAfter(grant.expireAt) || !grant.code.equals(code)) {
                throw new BusinessException("INVALID_DOWNLOAD_KEY", "下载鉴权失败");
            }

            PickupCodeRecord record = findRecordForAccess(code, false);
            securityService.clearSelectError(ip);
            return record;
        } catch (BusinessException e) {
            securityService.markSelectError(ip);
            throw e;
        }
    }

    @Transactional(readOnly = true)
    public Resource loadLocalResource(PickupCodeRecord record) {
        if (record.getStoragePath() == null || record.getStoragePath().isBlank()) {
            return null;
        }
        return storageService.loadLocalResource(record.getStoragePath());
    }

    @Transactional(readOnly = true)
    public String buildCloudDownloadUrl(PickupCodeRecord record) {
        return storageService.buildCloudDownloadUrl(record, 10);
    }

    public void cleanupExpiredData() {
        LocalDateTime now = LocalDateTime.now();

        securityService.cleanup();
        presignSessions.entrySet().removeIf(entry -> now.isAfter(entry.getValue().expiresAt));
        chunkUploadSessions.entrySet().removeIf(entry -> {
            boolean expired = now.isAfter(entry.getValue().expiresAt);
            if (expired) {
                cleanupChunkFiles(entry.getKey());
            }
            return expired;
        });
        downloadGrants.entrySet().removeIf(entry -> now.isAfter(entry.getValue().expireAt));

        List<PickupCodeRecord> activeRecords = pickupCodeRecordRepository.findByStatus(PickupCodeRecord.ShareStatus.ACTIVE);
        for (PickupCodeRecord record : activeRecords) {
            if (isExpired(record, now)) {
                record.setStatus(PickupCodeRecord.ShareStatus.EXPIRED);
                if (record.getShareType() == PickupCodeRecord.ShareType.FILE
                        && record.getStorageMode() == PickupCodeRecord.StorageMode.LOCAL
                        && record.getStoragePath() != null) {
                    storageService.deleteLocalQuietly(record.getStoragePath());
                }
            }
        }

        pickupCodeRecordRepository.saveAll(activeRecords);
    }

    @Transactional(readOnly = true)
    public PickupCodeRecord getRecordById(Long recordId) {
        return pickupCodeRecordRepository.findById(recordId)
                .orElseThrow(() -> new BusinessException("NOT_FOUND", "记录不存在"));
    }

    private Map<String, Object> toRecordPayload(PickupCodeRecord record, LocalDateTime now) {
        Map<String, Object> item = new HashMap<>();
        item.put("id", record.getId());
        item.put("code", record.getCode());
        item.put("share_type", record.getShareType() == null ? null : record.getShareType().name());
        item.put("text_content", record.getTextContent());
        item.put("display_name", record.getDisplayName());
        item.put("content_type", record.getContentType());
        item.put("storage_mode", record.getStorageMode() == null ? null : record.getStorageMode().name());
        item.put("storage_path", record.getStoragePath());
        item.put("cloud_config_id", record.getCloudConfigId());
        item.put("size_bytes", record.getSizeBytes());
        item.put("expire_at", record.getExpireAt());
        item.put("expired_count", record.getExpiredCount());
        item.put("used_count", record.getUsedCount());
        item.put("created_ip", record.getCreatedIp());
        item.put("creator_user_id", record.getCreatorUserId());
        item.put("status", record.getStatus() == null ? null : record.getStatus().name());
        item.put("created_at", record.getCreatedAt());
        item.put("updated_at", record.getUpdatedAt());

        boolean expired = isExpired(record, now);
        item.put("is_expired", expired);

        int expiredCount = record.getExpiredCount() == null ? -1 : record.getExpiredCount();
        int usedCount = record.getUsedCount() == null ? 0 : record.getUsedCount();
        item.put("is_count_limited", expiredCount >= 0);
        item.put("remain_count", expiredCount >= 0 ? Math.max(expiredCount - usedCount, 0) : null);
        return item;
    }

    private PickupCodeRecord.ShareStatus parseShareStatus(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }
        try {
            return PickupCodeRecord.ShareStatus.valueOf(status.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new BusinessException("INVALID_STATUS", "不支持的状态: " + status);
        }
    }

    private PickupCodeRecord.ShareType parseShareType(String shareType) {
        if (shareType == null || shareType.isBlank()) {
            return null;
        }
        try {
            return PickupCodeRecord.ShareType.valueOf(shareType.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new BusinessException("INVALID_SHARE_TYPE", "不支持的分享类型: " + shareType);
        }
    }

    private Map<String, Object> buildSelectPayload(PickupCodeRecord record) {
        Map<String, Object> detail = new HashMap<>();
        detail.put("code", record.getCode());
        detail.put("name", record.getDisplayName());
        detail.put("size", record.getSizeBytes());
        detail.put("share_type", record.getShareType().name());

        if (record.getShareType() == PickupCodeRecord.ShareType.TEXT) {
            detail.put("text", record.getTextContent());
            return detail;
        }

        String key = createDownloadGrant(record.getCode());
        detail.put("key", key);

        if (record.getStorageMode() == PickupCodeRecord.StorageMode.CLOUD_DIRECT) {
            String downloadUrl = storageService.buildCloudDownloadUrl(record, 10);
            if (downloadUrl != null) {
                detail.put("text", downloadUrl);
                return detail;
            }
        }

        detail.put("text", "/api/public/share/download?key=" + key + "&code=" + record.getCode());
        return detail;
    }

    private String createRecordFromSession(PresignSession session,
                                           PickupCodeRecord.StorageMode storageMode,
                                           String storagePath,
                                           Long cloudConfigId) {
        PickupCodeRecord record = buildFileRecord(
                generateUniqueCode(session.expireStyle),
                session.fileName,
                session.contentType,
                session.fileSize,
                new ExpirePolicy(session.expireStyle, session.expireAt, session.expiredCount),
                "presign",
                session.creatorUserId,
                storageMode,
                storagePath,
                cloudConfigId
        );

        pickupCodeRecordRepository.save(record);
        return record.getCode();
    }

    private PickupCodeRecord buildFileRecord(String code,
                                             String fileName,
                                             String contentType,
                                             long size,
                                             ExpirePolicy policy,
                                             String ip,
                                             Long creatorUserId,
                                             PickupCodeRecord.StorageMode storageMode,
                                             String storagePath,
                                             Long cloudConfigId) {
        PickupCodeRecord record = new PickupCodeRecord();
        record.setCode(code);
        record.setShareType(PickupCodeRecord.ShareType.FILE);
        record.setDisplayName(fileName);
        record.setContentType(contentType == null || contentType.isBlank() ? "application/octet-stream" : contentType);
        record.setSizeBytes(size);
        record.setExpireAt(policy.expireAt);
        record.setExpiredCount(policy.expiredCount);
        record.setUsedCount(0);
        record.setCreatedIp(ip);
        record.setCreatorUserId(creatorUserId);
        record.setStorageMode(storageMode);
        record.setStoragePath(storagePath);
        record.setCloudConfigId(cloudConfigId);
        record.setStatus(PickupCodeRecord.ShareStatus.ACTIVE);
        return record;
    }

    private PickupCodeRecord findRecordForAccess(String code, boolean increaseUsage) {
        PickupCodeRecord record = pickupCodeRecordRepository.findByCode(code)
                .orElseThrow(() -> new BusinessException("NOT_FOUND", "文件不存在"));

        if (record.getStatus() != PickupCodeRecord.ShareStatus.ACTIVE) {
            throw new BusinessException("SHARE_DISABLED", "文件已失效");
        }

        LocalDateTime now = LocalDateTime.now();
        if (isExpired(record, now)) {
            record.setStatus(PickupCodeRecord.ShareStatus.EXPIRED);
            pickupCodeRecordRepository.save(record);
            throw new BusinessException("SHARE_EXPIRED", "文件已过期");
        }

        if (increaseUsage) {
            int usedCount = record.getUsedCount() == null ? 0 : record.getUsedCount();
            record.setUsedCount(usedCount + 1);

            int expiredCount = record.getExpiredCount() == null ? -1 : record.getExpiredCount();
            if (expiredCount >= 0 && record.getUsedCount() >= expiredCount) {
                record.setStatus(PickupCodeRecord.ShareStatus.EXPIRED);
            }

            pickupCodeRecordRepository.save(record);
        }

        return record;
    }

    private boolean isExpired(PickupCodeRecord record, LocalDateTime now) {
        if (record.getExpireAt() != null && now.isAfter(record.getExpireAt())) {
            return true;
        }

        int expiredCount = record.getExpiredCount() == null ? -1 : record.getExpiredCount();
        int usedCount = record.getUsedCount() == null ? 0 : record.getUsedCount();
        return expiredCount >= 0 && usedCount >= expiredCount;
    }

    private PresignSession getValidPresignSession(String uploadId) {
        PresignSession session = presignSessions.get(uploadId);
        if (session == null) {
            throw new BusinessException("UPLOAD_SESSION_NOT_FOUND", "上传会话不存在或已过期");
        }

        if (LocalDateTime.now().isAfter(session.expiresAt)) {
            presignSessions.remove(uploadId);
            throw new BusinessException("UPLOAD_SESSION_EXPIRED", "上传会话已过期");
        }

        return session;
    }

    private ExpirePolicy resolveExpirePolicy(Integer expireValue, String expireStyle) {
        String style = normalizeExpireStyle(expireStyle);
        int value = expireValue == null ? 1 : expireValue;

        if ("count".equals(style) && value <= 0) {
            throw new BusinessException("INVALID_EXPIRE_VALUE", "过期时间值必须大于0");
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expireAt = now.plusHours(FIXED_LINK_EXPIRE_HOURS);
        int expiredCount = -1;

        switch (style) {
            case "count":
                expiredCount = value;
                break;
            case "day":
            case "hour":
            case "minute":
            case "forever":
                // 统一固定为 1 小时过期，访问次数限制沿用原逻辑。
                break;
            default:
                throw new BusinessException("INVALID_EXPIRE_STYLE", "不支持的过期策略");
        }

        long maxSaveSeconds = properties.getMaxSaveSeconds();
        if (maxSaveSeconds > 0 && expireAt != null) {
            long requestedSeconds = Duration.between(now, expireAt).getSeconds();
            if (requestedSeconds > maxSaveSeconds) {
                throw new BusinessException("EXPIRE_TOO_LONG", "超出允许的最长保存时间");
            }
        }

        return new ExpirePolicy(style, expireAt, expiredCount);
    }

    private String normalizeExpireStyle(String expireStyle) {
        String style = (expireStyle == null || expireStyle.isBlank()) ? "hour" : expireStyle.trim().toLowerCase(Locale.ROOT);
        if (!properties.getExpireStyles().contains(style)) {
            throw new BusinessException("INVALID_EXPIRE_STYLE", "过期时间类型错误");
        }
        return style;
    }

    private String generateUniqueCode(String expireStyle) {
        for (int i = 0; i < 20; i++) {
            String candidate = randomCode(PICKUP_CODE_LENGTH, PICKUP_CODE_CHARS);
            if (!pickupCodeRecordRepository.existsByCode(candidate)) {
                return candidate;
            }
        }
        throw new BusinessException("CODE_GENERATE_FAILED", "生成取件码失败，请稍后重试");
    }

    private String randomCode(int length, String chars) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(chars.charAt(RANDOM.nextInt(chars.length())));
        }
        return sb.toString();
    }

    private String createDownloadGrant(String code) {
        String token = UUID.randomUUID().toString().replace("-", "");
        LocalDateTime expireAt = LocalDateTime.now().plusSeconds(properties.getDownloadTokenTtlSeconds());
        downloadGrants.put(token, new DownloadGrant(code, expireAt));
        return token;
    }

    private Map<String, Object> createLocalFileShareRecord(String fileName,
                                                            String contentType,
                                                            long fileSize,
                                                            String relativePath,
                                                            String expireStyle,
                                                            LocalDateTime expireAt,
                                                            Integer expiredCount,
                                                            String ip,
                                                            Long creatorUserId) {
        PickupCodeRecord record = buildFileRecord(
                generateUniqueCode(expireStyle),
                storageService.sanitizeFileName(fileName),
                contentType,
                fileSize,
                new ExpirePolicy(expireStyle, expireAt, expiredCount),
                ip,
                creatorUserId,
                PickupCodeRecord.StorageMode.LOCAL,
                relativePath,
                null
        );
        pickupCodeRecordRepository.save(record);

        Map<String, Object> detail = new HashMap<>();
        detail.put("code", record.getCode());
        detail.put("name", record.getDisplayName());
        return detail;
    }

    private ChunkUploadSession getValidChunkSession(String uploadId) {
        ChunkUploadSession session = chunkUploadSessions.get(uploadId);
        if (session == null) {
            throw new BusinessException("UPLOAD_SESSION_NOT_FOUND", "上传会话不存在或已过期");
        }

        if (LocalDateTime.now().isAfter(session.expiresAt)) {
            cleanupChunkFiles(uploadId);
            chunkUploadSessions.remove(uploadId);
            throw new BusinessException("UPLOAD_SESSION_EXPIRED", "上传会话已过期");
        }
        return session;
    }

    private ChunkUploadSession findResumableChunkSession(String fileName,
                                                         long fileSize,
                                                         int chunkSize,
                                                         String fileHash) {
        if (fileHash == null || fileHash.isBlank()) {
            return null;
        }

        LocalDateTime now = LocalDateTime.now();
        for (ChunkUploadSession session : chunkUploadSessions.values()) {
            if (now.isAfter(session.expiresAt)) {
                continue;
            }
            if (!fileHash.equals(session.fileHash)) {
                continue;
            }
            if (!fileName.equals(session.fileName)) {
                continue;
            }
            if (fileSize != session.fileSize) {
                continue;
            }
            if (chunkSize != session.chunkSize) {
                continue;
            }
            return session;
        }

        return null;
    }

    private Map<String, Object> buildChunkInitPayload(ChunkUploadSession session) {
        List<Integer> uploadedChunks = new ArrayList<>(session.uploadedChunks);
        Collections.sort(uploadedChunks);

        Map<String, Object> detail = new HashMap<>();
        detail.put("existed", false);
        detail.put("upload_id", session.uploadId);
        detail.put("chunk_size", session.chunkSize);
        detail.put("total_chunks", session.totalChunks);
        detail.put("uploaded_chunks", uploadedChunks);
        return detail;
    }

    private Path getChunkTempDir(String uploadId) {
        Path tempRoot = Paths.get(fileStorageUtil.getTempPath()).toAbsolutePath().normalize();
        return tempRoot.resolve("fcb-chunk").resolve(uploadId).normalize();
    }

    private void cleanupChunkFiles(String uploadId) {
        Path chunkDir = getChunkTempDir(uploadId);
        if (!Files.exists(chunkDir)) {
            return;
        }
        try {
            Files.walk(chunkDir)
                    .sorted((a, b) -> b.getNameCount() - a.getNameCount())
                    .forEach(path -> {
                        try {
                            Files.deleteIfExists(path);
                        } catch (IOException e) {
                            log.warn("删除分片临时文件失败: path={}, error={}", path, e.getMessage());
                        }
                    });
        } catch (IOException e) {
            log.warn("清理分片目录失败: uploadId={}, error={}", uploadId, e.getMessage());
        }
    }

    private String detectContentType(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            return "application/octet-stream";
        }

        String detected = URLConnection.guessContentTypeFromName(fileName);
        return (detected == null || detected.isBlank()) ? "application/octet-stream" : detected;
    }

    private String sha256Hex(byte[] data) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(data);
            StringBuilder sb = new StringBuilder(digest.length * 2);
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception ex) {
            throw new BusinessException("HASH_FAILED", "分片哈希计算失败");
        }
    }

    private String csvEscape(Object value) {
        String text = value == null ? "" : String.valueOf(value);
        if (text.contains(",") || text.contains("\"") || text.contains("\n") || text.contains("\r")) {
            return "\"" + text.replace("\"", "\"\"") + "\"";
        }
        return text;
    }

    private String formatDateTime(LocalDateTime dateTime) {
        return dateTime == null ? "永久有效" : dateTime.toString();
    }

    private String resolveStatusForExport(PickupCodeRecord record, LocalDateTime now) {
        if (record == null || record.getStatus() == null) {
            return "";
        }
        if (record.getStatus() == PickupCodeRecord.ShareStatus.ACTIVE && isExpired(record, now)) {
            return PickupCodeRecord.ShareStatus.EXPIRED.name();
        }
        return record.getStatus().name();
    }

    private static class PresignSession {
        private final String uploadId;
        private final String fileName;
        private final long fileSize;
        private final String relativePath;
        private final FileCodeBoxStorageService.PresignTarget.Mode mode;
        private final String cloudKey;
        private final Long cloudConfigId;
        private final LocalDateTime createdAt;
        private final LocalDateTime expiresAt;
        private String expireStyle;
        private LocalDateTime expireAt;
        private Integer expiredCount;
        private final Long creatorUserId;
        private boolean uploaded;
        private boolean confirmed;
        private String generatedCode;
        private String contentType;

        private PresignSession(String uploadId,
                               String fileName,
                               long fileSize,
                               String relativePath,
                               FileCodeBoxStorageService.PresignTarget.Mode mode,
                               String cloudKey,
                               Long cloudConfigId,
                               LocalDateTime createdAt,
                               LocalDateTime expiresAt,
                               String expireStyle,
                               LocalDateTime expireAt,
                               Integer expiredCount,
                               Long creatorUserId) {
            this.uploadId = uploadId;
            this.fileName = fileName;
            this.fileSize = fileSize;
            this.relativePath = relativePath;
            this.mode = mode;
            this.cloudKey = cloudKey;
            this.cloudConfigId = cloudConfigId;
            this.createdAt = createdAt;
            this.expiresAt = expiresAt;
            this.expireStyle = expireStyle;
            this.expireAt = expireAt;
            this.expiredCount = expiredCount;
            this.creatorUserId = creatorUserId;
            this.uploaded = false;
            this.confirmed = false;
        }
    }

    @AllArgsConstructor
    private static class DownloadGrant {
        private final String code;
        private final LocalDateTime expireAt;
    }

    private static class ExpirePolicy {
        private final String style;
        private final LocalDateTime expireAt;
        private final Integer expiredCount;

        private ExpirePolicy(String style, LocalDateTime expireAt, Integer expiredCount) {
            this.style = style;
            this.expireAt = expireAt;
            this.expiredCount = expiredCount;
        }
    }

    private static class ChunkUploadSession {
        private final String uploadId;
        private final String fileName;
        private final String fileHash;
        private final long fileSize;
        private final int chunkSize;
        private final int totalChunks;
        private final String relativePath;
        private String expireStyle;
        private LocalDateTime expireAt;
        private Integer expiredCount;
        private final Long creatorUserId;
        @SuppressWarnings("unused")
        private final LocalDateTime createdAt;
        private final LocalDateTime expiresAt;
        private final Set<Integer> uploadedChunks = Collections.newSetFromMap(new ConcurrentHashMap<>());

        private ChunkUploadSession(String uploadId,
                                   String fileName,
                                   String fileHash,
                                   long fileSize,
                                   int chunkSize,
                                   int totalChunks,
                                   String relativePath,
                                   String expireStyle,
                                   LocalDateTime expireAt,
                                   Integer expiredCount,
                                   Long creatorUserId,
                                   LocalDateTime createdAt,
                                   LocalDateTime expiresAt) {
            this.uploadId = uploadId;
            this.fileName = fileName;
            this.fileHash = fileHash;
            this.fileSize = fileSize;
            this.chunkSize = chunkSize;
            this.totalChunks = totalChunks;
            this.relativePath = relativePath;
            this.expireStyle = expireStyle;
            this.expireAt = expireAt;
            this.expiredCount = expiredCount;
            this.creatorUserId = creatorUserId;
            this.createdAt = createdAt;
            this.expiresAt = expiresAt;
        }
    }

    @AllArgsConstructor
    public static class CsvExportResult {
        private final String fileName;
        private final String content;
        private final int rowCount;

        public String getFileName() {
            return fileName;
        }

        public String getContent() {
            return content;
        }

        public int getRowCount() {
            return rowCount;
        }
    }
}
