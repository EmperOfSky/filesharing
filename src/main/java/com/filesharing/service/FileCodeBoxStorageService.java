package com.filesharing.service;

import com.filesharing.entity.CloudStorageConfig;
import com.filesharing.entity.PickupCodeRecord;
import com.filesharing.exception.BusinessException;
import com.filesharing.repository.CloudStorageConfigRepository;
import com.filesharing.util.FileStorageUtil;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.Locale;
import java.util.UUID;

/**
 * FileCodeBox 兼容的存储适配层。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FileCodeBoxStorageService {

    private final FileStorageUtil fileStorageUtil;
    private final CloudStorageService cloudStorageService;
    private final CloudStorageConfigRepository cloudStorageConfigRepository;

    public String sanitizeFileName(String fileName) {
        String raw = (fileName == null || fileName.isBlank()) ? "unnamed.bin" : fileName.trim();
        String safe = raw.replaceAll("[^a-zA-Z0-9._-]", "_");
        if (safe.isBlank()) {
            return "unnamed.bin";
        }
        return safe;
    }

    public String buildRelativePath(String fileName) {
        LocalDate today = LocalDate.now();
        String safeName = sanitizeFileName(fileName);
        String datePath = String.format(Locale.ROOT, "share/data/%04d/%02d/%02d",
                today.getYear(), today.getMonthValue(), today.getDayOfMonth());
        return datePath + "/" + UUID.randomUUID().toString().replace("-", "") + "/" + safeName;
    }

    public void saveLocalFile(MultipartFile file, String relativePath) throws IOException {
        Path uploadRoot = Paths.get(fileStorageUtil.getUploadPath()).toAbsolutePath().normalize();
        Path target = uploadRoot.resolve(relativePath).normalize();
        if (!target.startsWith(uploadRoot)) {
            throw new BusinessException("INVALID_PATH", "非法的文件保存路径");
        }

        Files.createDirectories(target.getParent());
        file.transferTo(target);
    }

    public Resource loadLocalResource(String relativePath) {
        try {
            Path uploadRoot = Paths.get(fileStorageUtil.getUploadPath()).toAbsolutePath().normalize();
            Path target = uploadRoot.resolve(relativePath).normalize();
            if (!target.startsWith(uploadRoot) || !Files.exists(target)) {
                return null;
            }
            return new UrlResource(target.toUri());
        } catch (MalformedURLException e) {
            log.error("加载本地文件资源失败: {}", e.getMessage(), e);
            return null;
        }
    }

    public void deleteLocalQuietly(String relativePath) {
        try {
            Path uploadRoot = Paths.get(fileStorageUtil.getUploadPath()).toAbsolutePath().normalize();
            Path target = uploadRoot.resolve(relativePath).normalize();
            if (target.startsWith(uploadRoot) && Files.exists(target)) {
                Files.delete(target);
            }
        } catch (Exception e) {
            log.warn("删除本地文件失败: path={}, error={}", relativePath, e.getMessage());
        }
    }

    public PresignTarget resolvePresignTarget(String relativePath, int expiresSeconds) {
        try {
            CloudStorageConfig config = cloudStorageConfigRepository.findFirstByIsDefaultTrueAndIsEnabledTrue().orElse(null);
            if (config != null
                    && Boolean.TRUE.equals(config.getIsEnabled())
                    && config.getProviderType() != CloudStorageConfig.ProviderType.LOCAL) {
                String cloudKey = buildCloudKey(config, relativePath);
                String signedUrl = cloudStorageService.getSignedUrl(
                        cloudKey,
                        config.getId(),
                        Math.max(1, expiresSeconds / 60)
                );
                if (signedUrl != null && !signedUrl.isBlank()) {
                    return PresignTarget.direct(signedUrl, cloudKey, config.getId());
                }
            }
        } catch (Exception e) {
            log.debug("未启用直传模式，回退到代理上传: {}", e.getMessage());
        }
        return PresignTarget.proxy();
    }

    public String buildCloudDownloadUrl(PickupCodeRecord record, int expireMinutes) {
        if (record.getCloudConfigId() == null || record.getStoragePath() == null) {
            return null;
        }
        try {
            return cloudStorageService.getSignedUrl(record.getStoragePath(), record.getCloudConfigId(), Math.max(1, expireMinutes));
        } catch (Exception e) {
            log.warn("生成云端下载 URL 失败: recordId={}, error={}", record.getId(), e.getMessage());
            return null;
        }
    }

    private String buildCloudKey(CloudStorageConfig config, String relativePath) {
        String basePath = config.getBasePath();
        if (basePath == null || basePath.isBlank()) {
            return relativePath;
        }
        String normalizedBase = basePath.endsWith("/") ? basePath.substring(0, basePath.length() - 1) : basePath;
        return normalizedBase + "/" + relativePath;
    }

    @Data
    @AllArgsConstructor
    public static class PresignTarget {
        private Mode mode;
        private String uploadUrl;
        private String cloudKey;
        private Long cloudConfigId;

        public static PresignTarget direct(String uploadUrl, String cloudKey, Long cloudConfigId) {
            return new PresignTarget(Mode.DIRECT, uploadUrl, cloudKey, cloudConfigId);
        }

        public static PresignTarget proxy() {
            return new PresignTarget(Mode.PROXY, null, null, null);
        }

        public enum Mode {
            DIRECT,
            PROXY
        }
    }
}
