package com.filesharing.util;

import io.minio.BucketExistsArgs;
import io.minio.GetObjectArgs;
import io.minio.GetObjectResponse;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.StatObjectArgs;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.PostConstruct;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * 文件存储工具类
 * 提供文件上传、下载、删除、MD5计算等核心功能
 */
@Slf4j
@Component
public class FileStorageUtil {

    @Value("${file.upload.path:./uploads}")
    private String uploadPath;

    @Value("${file.temp.path:./temp}")
    private String tempPath;

    @Value("${storage.type:local}")
    private String storageType;

    @Value("${storage.minio.endpoint:}")
    private String minioEndpoint;

    @Value("${storage.minio.access-key:}")
    private String minioAccessKey;

    @Value("${storage.minio.secret-key:}")
    private String minioSecretKey;

    @Value("${storage.minio.bucket:filesharing}")
    private String minioBucket;

    @Value("${storage.minio.base-path:}")
    private String minioBasePath;

    private MinioClient minioClient;
    private boolean minioEnabled;

    @PostConstruct
    public void init() {
        // 本地临时目录始终需要。
        createDirectories(tempPath);

        minioEnabled = tryInitMinio();
        if (!minioEnabled) {
            createDirectories(uploadPath);
        }

        log.info("文件存储路径初始化完成: storageType={}, minioEnabled={}, uploadPath={}, tempPath={}",
                storageType, minioEnabled, uploadPath, tempPath);
    }

    /**
     * 保存文件到本地存储
     */
    public String saveFile(MultipartFile file) throws IOException {
        String fileName = generateFileName(file.getOriginalFilename());

        if (minioEnabled) {
            String objectKey = toObjectKey(fileName);
            try (InputStream inputStream = file.getInputStream()) {
                minioClient.putObject(PutObjectArgs.builder()
                        .bucket(minioBucket)
                        .object(objectKey)
                        .stream(inputStream, file.getSize(), -1)
                        .contentType(defaultContentType(file.getContentType()))
                        .build());
            } catch (Exception e) {
                throw new IOException("上传到 MinIO 失败: " + e.getMessage(), e);
            }

            log.info("文件保存成功(MinIO): {}", objectKey);
            return fileName;
        }

        Path filePath = resolveLocalPathForWrite(fileName);
        file.transferTo(filePath);
        log.info("文件保存成功(LOCAL): {}", filePath);

        return fileName;
    }

    /**
     * 按指定相对路径保存文件。
     */
    public void saveFileAtPath(MultipartFile file, String relativePath) throws IOException {
        if (relativePath == null || relativePath.isBlank()) {
            throw new IOException("目标路径不能为空");
        }

        if (minioEnabled) {
            String objectKey = toObjectKey(relativePath);
            try (InputStream inputStream = file.getInputStream()) {
                minioClient.putObject(PutObjectArgs.builder()
                        .bucket(minioBucket)
                        .object(objectKey)
                        .stream(inputStream, file.getSize(), -1)
                        .contentType(defaultContentType(file.getContentType()))
                        .build());
            } catch (Exception e) {
                throw new IOException("上传到 MinIO 失败: " + e.getMessage(), e);
            }
            return;
        }

        Path target = resolveLocalPathForWrite(relativePath);
        file.transferTo(target);
    }

    /**
     * 按指定相对路径保存输入流。
     */
    public void saveInputStreamAtPath(InputStream inputStream,
                                      long size,
                                      String relativePath,
                                      String contentType) throws IOException {
        if (relativePath == null || relativePath.isBlank()) {
            throw new IOException("目标路径不能为空");
        }

        if (minioEnabled) {
            String objectKey = toObjectKey(relativePath);
            try {
                PutObjectArgs.Builder builder = PutObjectArgs.builder()
                        .bucket(minioBucket)
                        .object(objectKey)
                        .contentType(defaultContentType(contentType));

                if (size >= 0) {
                    builder.stream(inputStream, size, -1);
                } else {
                    builder.stream(inputStream, -1, 10 * 1024 * 1024);
                }

                minioClient.putObject(builder.build());
            } catch (Exception e) {
                throw new IOException("上传到 MinIO 失败: " + e.getMessage(), e);
            }
            return;
        }

        Path target = resolveLocalPathForWrite(relativePath);
        try (OutputStream outputStream = Files.newOutputStream(target)) {
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
        }
    }

    /**
     * 保存分片文件
     */
    public String saveChunk(MultipartFile chunk, String uploadId, Integer chunkIndex) throws IOException {
        String chunkFileName = uploadId + "_part_" + chunkIndex;
        Path chunkPath = Paths.get(tempPath, chunkFileName);
        
        // 创建临时目录
        Files.createDirectories(chunkPath.getParent());
        
        // 保存分片
        chunk.transferTo(chunkPath);
        log.info("分片保存成功: {}, 索引: {}", chunkFileName, chunkIndex);
        
        return chunkFileName;
    }

    /**
     * 合并分片文件
     */
    public String mergeChunks(String uploadId, Integer totalChunks, String originalFileName) throws IOException {
        String mergedFileName = generateFileName(originalFileName);

        Path mergedFilePath = resolveLocalPathForWrite("_merge_" + uploadId + "_" + UUID.randomUUID());
        try (OutputStream outputStream = Files.newOutputStream(mergedFilePath)) {
            mergeChunkPartsToStream(uploadId, totalChunks, outputStream);
        }

        if (minioEnabled) {
            long mergedSize = Files.size(mergedFilePath);
            try (InputStream inputStream = Files.newInputStream(mergedFilePath)) {
                saveInputStreamAtPath(inputStream, mergedSize, mergedFileName, null);
            } finally {
                Files.deleteIfExists(mergedFilePath);
            }
            log.info("分片合并完成(MinIO): {}, 总分片数: {}", mergedFileName, totalChunks);
            return mergedFileName;
        }

        Path targetPath = resolveLocalPathForWrite(mergedFileName);
        Files.move(mergedFilePath, targetPath);
        log.info("分片合并完成(LOCAL): {}, 总分片数: {}", mergedFileName, totalChunks);
        return mergedFileName;
    }

    /**
     * 计算文件MD5值（用于秒传功能）
     */
    public String calculateMD5(MultipartFile file) throws IOException {
        MessageDigest md = null;
        try {
            md = MessageDigest.getInstance("MD5");
        } catch (Exception e) {
            throw new RuntimeException("MD5算法不可用", e);
        }

        try (InputStream inputStream = file.getInputStream()) {
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                md.update(buffer, 0, bytesRead);
            }
        }

        byte[] digest = md.digest();
        StringBuilder sb = new StringBuilder();
        for (byte b : digest) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    /**
     * 根据MD5获取文件路径（秒传检查）
     */
    public String getFilePathByMD5(String md5Hash) {
        // 在实际应用中，这里应该查询数据库获取已存在的文件路径
        // 简化实现：根据MD5生成文件名
        return md5Hash + ".dat";
    }

    /**
     * 检查文件是否存在
     */
    public boolean fileExists(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            return false;
        }

        if (minioEnabled) {
            String objectKey = resolveExistingObjectKeyForRead(fileName, null);
            return objectKey != null;
        }

        try {
            return resolveLocalPathForRead(fileName, null) != null;
        } catch (IOException e) {
            return false;
        }
    }

    /**
     * 读取文件内容
     */
    public byte[] readFile(String fileName) throws IOException {
        if (minioEnabled) {
            String objectKey = resolveExistingObjectKeyForRead(fileName, null);
            if (objectKey == null) {
                throw new FileNotFoundException("MinIO 文件不存在: " + fileName);
            }
            try (GetObjectResponse object = minioClient.getObject(GetObjectArgs.builder()
                    .bucket(minioBucket)
                    .object(objectKey)
                    .build())) {
                return object.readAllBytes();
            } catch (Exception e) {
                throw new IOException("读取 MinIO 文件失败: " + e.getMessage(), e);
            }
        }

        Path filePath = resolveLocalPathForRead(fileName, null);
        if (filePath == null) {
            throw new FileNotFoundException("本地文件不存在: " + fileName);
        }
        return Files.readAllBytes(filePath);
    }

    /**
     * 删除文件
     */
    public boolean deleteFile(String fileName) {
        try {
            if (minioEnabled) {
                String objectKey = resolveExistingObjectKeyForRead(fileName, null);
                if (objectKey == null) {
                    return false;
                }
                minioClient.removeObject(RemoveObjectArgs.builder()
                        .bucket(minioBucket)
                        .object(objectKey)
                        .build());
                log.info("文件删除成功(MinIO): {}", objectKey);
                return true;
            }

            Path filePath = resolveLocalPathForRead(fileName, null);
            if (filePath != null) {
                Files.delete(filePath);
                log.info("文件删除成功(LOCAL): {}", fileName);
                return true;
            }
            return false;
        } catch (IOException e) {
            log.error("文件删除失败: {}", fileName, e);
            return false;
        } catch (Exception e) {
            log.error("文件删除失败: {}", fileName, e);
            return false;
        }
    }

    /**
     * 加载文件为 Resource，用于下载/预览。
     */
    public Resource loadAsResource(String storageName, String fallbackPath) {
        try {
            if (minioEnabled) {
                String objectKey = resolveExistingObjectKeyForRead(storageName, fallbackPath);
                if (objectKey == null) {
                    return null;
                }

                byte[] content;
                try (GetObjectResponse object = minioClient.getObject(GetObjectArgs.builder()
                        .bucket(minioBucket)
                        .object(objectKey)
                        .build())) {
                    content = object.readAllBytes();
                }

                return new NamedByteArrayResource(content, resolveResourceFilename(storageName, fallbackPath));
            }

            Path path = resolveLocalPathForRead(storageName, fallbackPath);
            if (path == null) {
                return null;
            }
            return new UrlResource(path.toUri());
        } catch (Exception e) {
            log.error("加载文件资源失败: storageName={}, fallbackPath={}", storageName, fallbackPath, e);
            return null;
        }
    }

    /**
     * 复制文件。
     */
    public void copyFile(String sourceStorageName, String targetStorageName, String sourceFallbackPath) throws IOException {
        if (targetStorageName == null || targetStorageName.isBlank()) {
            throw new IOException("目标存储名不能为空");
        }

        if (minioEnabled) {
            String sourceObject = resolveExistingObjectKeyForRead(sourceStorageName, sourceFallbackPath);
            if (sourceObject == null) {
                throw new IOException("源文件不存在: " + sourceStorageName);
            }

            String targetObject = toObjectKey(targetStorageName);
            try {
                minioClient.statObject(StatObjectArgs.builder().bucket(minioBucket).object(sourceObject).build());
                try (GetObjectResponse object = minioClient.getObject(GetObjectArgs.builder()
                        .bucket(minioBucket)
                        .object(sourceObject)
                        .build())) {
                    minioClient.putObject(PutObjectArgs.builder()
                            .bucket(minioBucket)
                            .object(targetObject)
                            .stream(object, -1, 10 * 1024 * 1024)
                            .contentType("application/octet-stream")
                            .build());
                }
            } catch (Exception e) {
                throw new IOException("复制 MinIO 文件失败: " + e.getMessage(), e);
            }
            return;
        }

        Path sourcePath = resolveLocalPathForRead(sourceStorageName, sourceFallbackPath);
        if (sourcePath == null) {
            throw new IOException("源文件不存在: " + sourceStorageName);
        }

        Path targetPath = resolveLocalPathForWrite(targetStorageName);
        Files.copy(sourcePath, targetPath);
    }

    /**
     * 清理临时文件
     */
    public void cleanupTempFiles() {
        try {
            Path tempDir = Paths.get(tempPath);
            if (Files.exists(tempDir)) {
                Files.walk(tempDir)
                    .filter(Files::isRegularFile)
                    .forEach(path -> {
                        try {
                            // 删除超过24小时的临时文件
                            if (System.currentTimeMillis() - Files.getLastModifiedTime(path).toMillis() > 24 * 60 * 60 * 1000) {
                                Files.delete(path);
                                log.debug("清理临时文件: {}", path.getFileName());
                            }
                        } catch (IOException e) {
                            log.warn("清理临时文件失败: {}", path.getFileName(), e);
                        }
                    });
            }
        } catch (IOException e) {
            log.error("清理临时文件目录失败", e);
        }
    }

    /**
     * 获取文件扩展名
     */
    public String getFileExtension(String fileName) {
        if (fileName == null || fileName.lastIndexOf('.') == -1) {
            return "";
        }
        return fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase();
    }

    /**
     * 生成唯一文件名
     */
    private String generateFileName(String originalFileName) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String uuid = UUID.randomUUID().toString().replace("-", "");
        String extension = getFileExtension(originalFileName);
        
        if (!extension.isEmpty()) {
            return timestamp + "_" + uuid + "." + extension;
        } else {
            return timestamp + "_" + uuid;
        }
    }

    /**
     * 创建目录
     */
    private void createDirectories(String path) {
        try {
            Path dirPath = Paths.get(path);
            if (!Files.exists(dirPath)) {
                Files.createDirectories(dirPath);
                log.info("创建目录: {}", path);
            }
        } catch (IOException e) {
            log.error("创建目录失败: {}", path, e);
            throw new RuntimeException("目录创建失败: " + path, e);
        }
    }

    /**
     * 获取上传路径
     */
    public String getUploadPath() {
        return uploadPath;
    }

    /**
     * 获取临时路径
     */
    public String getTempPath() {
        return tempPath;
    }

    private boolean tryInitMinio() {
        if (!"minio".equalsIgnoreCase(storageType)) {
            return false;
        }
        if (isBlank(minioEndpoint) || isBlank(minioAccessKey) || isBlank(minioSecretKey) || isBlank(minioBucket)) {
            log.error("MinIO 配置不完整，自动回退到本地存储");
            return false;
        }

        try {
            minioClient = MinioClient.builder()
                    .endpoint(minioEndpoint)
                    .credentials(minioAccessKey, minioSecretKey)
                    .build();

            boolean exists = minioClient.bucketExists(BucketExistsArgs.builder().bucket(minioBucket).build());
            if (!exists) {
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(minioBucket).build());
            }

            log.info("MinIO 初始化成功: endpoint={}, bucket={}, basePath={}",
                    minioEndpoint, minioBucket, minioBasePath);
            return true;
        } catch (Exception e) {
            log.error("MinIO 初始化失败，回退本地存储: {}", e.getMessage(), e);
            minioClient = null;
            return false;
        }
    }

    private void mergeChunkPartsToStream(String uploadId, Integer totalChunks, OutputStream outputStream) throws IOException {
        for (int i = 0; i < totalChunks; i++) {
            String chunkFileName = uploadId + "_part_" + i;
            Path chunkPath = Paths.get(tempPath, chunkFileName);

            if (!Files.exists(chunkPath)) {
                throw new IOException("分片文件缺失: " + chunkFileName);
            }

            try (InputStream inputStream = Files.newInputStream(chunkPath)) {
                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }
            }
            Files.delete(chunkPath);
        }
    }

    private Path resolveLocalPathForWrite(String relativePath) throws IOException {
        Path uploadRoot = Paths.get(uploadPath).toAbsolutePath().normalize();
        String normalized = normalizeRelativePath(relativePath);
        Path target = uploadRoot.resolve(normalized).normalize();
        if (!target.startsWith(uploadRoot)) {
            throw new IOException("非法文件路径: " + relativePath);
        }

        Path parent = target.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        return target;
    }

    private Path resolveLocalPathForRead(String storageName, String fallbackPath) throws IOException {
        Path uploadRoot = Paths.get(uploadPath).toAbsolutePath().normalize();

        String[] candidates = buildReadCandidates(storageName, fallbackPath);
        for (String candidate : candidates) {
            if (candidate == null || candidate.isBlank()) {
                continue;
            }

            Path path = uploadRoot.resolve(candidate).normalize();
            if (path.startsWith(uploadRoot) && Files.exists(path)) {
                return path;
            }
        }

        return null;
    }

    private String resolveExistingObjectKeyForRead(String storageName, String fallbackPath) {
        if (!minioEnabled) {
            return null;
        }

        String[] candidates = buildReadCandidates(storageName, fallbackPath);
        for (String candidate : candidates) {
            if (candidate == null || candidate.isBlank()) {
                continue;
            }

            String asIs = normalizeRelativePath(candidate);
            String withBase = toObjectKey(candidate);

            if (objectExists(asIs)) {
                return asIs;
            }
            if (!withBase.equals(asIs) && objectExists(withBase)) {
                return withBase;
            }
        }

        return null;
    }

    private boolean objectExists(String objectKey) {
        try {
            minioClient.statObject(StatObjectArgs.builder()
                    .bucket(minioBucket)
                    .object(objectKey)
                    .build());
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }

    private String[] buildReadCandidates(String storageName, String fallbackPath) {
        String fallbackCandidate = extractStorageNameFromPath(fallbackPath);
        if (fallbackCandidate == null || fallbackCandidate.isBlank()) {
            return new String[]{normalizeRelativePath(storageName)};
        }

        return new String[]{
                normalizeRelativePath(storageName),
                normalizeRelativePath(fallbackCandidate)
        };
    }

    private String extractStorageNameFromPath(String filePath) {
        if (filePath == null || filePath.isBlank()) {
            return null;
        }
        return normalizeRelativePath(filePath);
    }

    private String normalizeRelativePath(String rawPath) {
        if (rawPath == null) {
            return "";
        }

        String normalized = rawPath.trim().replace('\\', '/');
        while (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }
        if (normalized.startsWith("uploads/")) {
            normalized = normalized.substring("uploads/".length());
        }
        return normalized;
    }

    private String toObjectKey(String rawPath) {
        String normalized = normalizeRelativePath(rawPath);
        if (isBlank(minioBasePath)) {
            return normalized;
        }

        String base = normalizeRelativePath(minioBasePath);
        if (normalized.startsWith(base + "/")) {
            return normalized;
        }
        return base + "/" + normalized;
    }

    private String resolveResourceFilename(String storageName, String fallbackPath) {
        String candidate = storageName;
        if ((candidate == null || candidate.isBlank()) && fallbackPath != null) {
            candidate = extractStorageNameFromPath(fallbackPath);
        }
        if (candidate == null || candidate.isBlank()) {
            return "file";
        }

        String normalized = candidate.replace('\\', '/');
        int index = normalized.lastIndexOf('/');
        if (index >= 0 && index < normalized.length() - 1) {
            return normalized.substring(index + 1);
        }
        return normalized;
    }

    private String defaultContentType(String contentType) {
        if (contentType == null || contentType.isBlank()) {
            return "application/octet-stream";
        }
        return contentType;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private static class NamedByteArrayResource extends ByteArrayResource {
        private final String filename;

        private NamedByteArrayResource(byte[] byteArray, String filename) {
            super(byteArray);
            this.filename = filename;
        }

        @Override
        public String getFilename() {
            return filename;
        }
    }
}