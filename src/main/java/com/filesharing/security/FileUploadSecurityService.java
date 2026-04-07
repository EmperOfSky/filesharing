package com.filesharing.security;

import com.filesharing.config.SecurityHardeningProperties;
import com.filesharing.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileUploadSecurityService {

    private static final int SCAN_BYTES_LIMIT = 8192;

    private static final Set<String> BLOCKED_MIME_PREFIXES = Set.of(
            "application/x-msdownload",
            "application/x-dosexec",
            "application/x-sh",
            "application/x-bat"
    );

    private static final Set<String> SUSPICIOUS_MARKERS = Set.of(
            "<%",
            "<?php",
            "#!/bin/sh",
            "#!/bin/bash",
            "powershell",
            "cmd.exe",
            "javascript:",
            "<script"
    );

    private final SecurityHardeningProperties securityHardeningProperties;

    public void validateAndScan(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("FILE_EMPTY", "上传文件不能为空");
        }

        String filename = safeFilename(file.getOriginalFilename());
        validateBasic(filename, file.getSize(), file.getContentType());

        if (!securityHardeningProperties.getUpload().isEnableMalwareScan()) {
            return;
        }

        try (InputStream inputStream = file.getInputStream()) {
            scanForMaliciousPayload(inputStream, filename);
        } catch (IOException e) {
            throw new BusinessException("FILE_SCAN_FAILED", "文件安全扫描失败");
        }
    }

    public void validateAndScan(String filename, long size, String contentType, InputStream stream) {
        String safeName = safeFilename(filename);
        validateBasic(safeName, size, contentType);

        if (!securityHardeningProperties.getUpload().isEnableMalwareScan()) {
            return;
        }

        try {
            scanForMaliciousPayload(stream, safeName);
        } catch (IOException e) {
            throw new BusinessException("FILE_SCAN_FAILED", "文件安全扫描失败");
        }
    }

    public void validateBasic(String filename, long size, String contentType) {
        String safeName = safeFilename(filename);
        String extension = getExtension(safeName);

        long maxFileSize = securityHardeningProperties.getUpload().getMaxFileSizeBytes();
        if (size <= 0 || size > maxFileSize) {
            throw new BusinessException("FILE_TOO_LARGE", "文件大小超出限制");
        }

        if (securityHardeningProperties.getUpload().getBlockedExtensions().contains(extension)) {
            throw new BusinessException("FILE_TYPE_BLOCKED", "禁止上传可执行或脚本文件");
        }

        if (!securityHardeningProperties.getUpload().getAllowedExtensions().isEmpty()
                && !securityHardeningProperties.getUpload().getAllowedExtensions().contains(extension)) {
            throw new BusinessException("FILE_TYPE_NOT_ALLOWED", "不允许的文件后缀");
        }

        String normalizedContentType = contentType == null ? "" : contentType.toLowerCase(Locale.ROOT);
        if (!normalizedContentType.isBlank()) {
            for (String blockedPrefix : BLOCKED_MIME_PREFIXES) {
                if (normalizedContentType.startsWith(blockedPrefix)) {
                    throw new BusinessException("FILE_TYPE_BLOCKED", "检测到危险文件类型");
                }
            }
        }
    }

    private void scanForMaliciousPayload(InputStream inputStream, String filename) throws IOException {
        byte[] headerBytes = inputStream.readNBytes(SCAN_BYTES_LIMIT);
        if (headerBytes.length == 0) {
            throw new BusinessException("FILE_EMPTY", "上传文件不能为空");
        }

        if (!isLikelyText(headerBytes)) {
            return;
        }

        String text = new String(headerBytes, StandardCharsets.UTF_8).toLowerCase(Locale.ROOT);
        for (String marker : SUSPICIOUS_MARKERS) {
            if (text.contains(marker)) {
                log.warn("阻断疑似恶意上传: filename={}, marker={}", filename, marker);
                throw new BusinessException("MALICIOUS_FILE_DETECTED", "检测到疑似恶意脚本，上传已拒绝");
            }
        }
    }

    private boolean isLikelyText(byte[] bytes) {
        int printableCount = 0;
        for (byte b : bytes) {
            int value = b & 0xFF;
            if (value == 9 || value == 10 || value == 13 || (value >= 32 && value <= 126)) {
                printableCount++;
            }
        }
        return printableCount >= Math.max(8, bytes.length / 2);
    }

    private String safeFilename(String filename) {
        if (filename == null || filename.isBlank()) {
            return "unnamed.bin";
        }

        String normalized = filename.replace('\\', '/');
        int lastSlash = normalized.lastIndexOf('/');
        if (lastSlash >= 0) {
            normalized = normalized.substring(lastSlash + 1);
        }

        return normalized.trim();
    }

    private String getExtension(String filename) {
        int index = filename.lastIndexOf('.');
        if (index < 0 || index == filename.length() - 1) {
            return "";
        }

        return filename.substring(index + 1).toLowerCase(Locale.ROOT);
    }
}
