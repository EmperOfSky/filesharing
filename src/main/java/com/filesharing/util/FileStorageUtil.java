package com.filesharing.util;

import lombok.extern.slf4j.Slf4j;
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

    @PostConstruct
    public void init() {
        // 创建必要的目录
        createDirectories(uploadPath);
        createDirectories(tempPath);
        log.info("文件存储路径初始化完成: uploadPath={}, tempPath={}", uploadPath, tempPath);
    }

    /**
     * 保存文件到本地存储
     */
    public String saveFile(MultipartFile file) throws IOException {
        String fileName = generateFileName(file.getOriginalFilename());
        Path filePath = Paths.get(uploadPath, fileName);
        
        // 创建父目录
        Files.createDirectories(filePath.getParent());
        
        // 保存文件
        file.transferTo(filePath);
        log.info("文件保存成功: {}", filePath.toString());
        
        return fileName;
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
        Path mergedFilePath = Paths.get(uploadPath, mergedFileName);
        
        // 创建目标目录
        Files.createDirectories(mergedFilePath.getParent());
        
        try (OutputStream outputStream = Files.newOutputStream(mergedFilePath)) {
            // 按顺序合并所有分片
            for (int i = 0; i < totalChunks; i++) {
                String chunkFileName = uploadId + "_part_" + i;
                Path chunkPath = Paths.get(tempPath, chunkFileName);
                
                if (Files.exists(chunkPath)) {
                    try (InputStream inputStream = Files.newInputStream(chunkPath)) {
                        byte[] buffer = new byte[8192];
                        int bytesRead;
                        while ((bytesRead = inputStream.read(buffer)) != -1) {
                            outputStream.write(buffer, 0, bytesRead);
                        }
                    }
                    // 删除已合并的分片
                    Files.delete(chunkPath);
                } else {
                    throw new IOException("分片文件缺失: " + chunkFileName);
                }
            }
        }
        
        log.info("分片合并完成: {}, 总分片数: {}", mergedFileName, totalChunks);
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
        Path filePath = Paths.get(uploadPath, fileName);
        return Files.exists(filePath);
    }

    /**
     * 读取文件内容
     */
    public byte[] readFile(String fileName) throws IOException {
        Path filePath = Paths.get(uploadPath, fileName);
        return Files.readAllBytes(filePath);
    }

    /**
     * 删除文件
     */
    public boolean deleteFile(String fileName) {
        try {
            Path filePath = Paths.get(uploadPath, fileName);
            if (Files.exists(filePath)) {
                Files.delete(filePath);
                log.info("文件删除成功: {}", fileName);
                return true;
            }
            return false;
        } catch (IOException e) {
            log.error("文件删除失败: {}", fileName, e);
            return false;
        }
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
}