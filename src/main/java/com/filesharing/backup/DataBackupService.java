package com.filesharing.backup;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.filesharing.entity.FileEntity;
import com.filesharing.entity.User;
import com.filesharing.exception.BusinessException;
import com.filesharing.repository.FileRepository;
import com.filesharing.repository.UserRepository;
import com.filesharing.util.FileStorageUtil;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.zip.ZipInputStream;

/**
 * 数据备份恢复服务 - 提供完整的数据备份和灾难恢复功能
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DataBackupService {
    
    private final FileRepository fileRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;
    private final BackupTaskRepository backupTaskRepository;
    private final FileStorageUtil fileStorageUtil;
    
    // 备份配置（从 application.yml 注入）
    @Value("${backup.base-path:./backups}")
    private String backupBasePath;

    @Value("${backup.max-size:10737418240}")
    private long maxBackupSize;

    @Value("${backup.compression-level:6}")
    private int compressionLevel;
    
    @Value("${file.upload.path:./uploads/}")
    private String storageBasePath;
    
    /**
     * 创建完整备份
     */
    public BackupResult createFullBackup(String backupName, boolean includeFiles) {
        String taskId = UUID.randomUUID().toString();
        BackupTask task = new BackupTask(taskId, backupName, "FULL");
        // 持久化任务初始状态
        BackupTaskEntity entity = new BackupTaskEntity();
        entity.setTaskId(taskId);
        entity.setBackupName(backupName);
        entity.setBackupType("FULL");
        entity.setStatus("PENDING");
        backupTaskRepository.save(entity);
        
        try {
            task.setStatus("RUNNING");
            task.setStartTime(LocalDateTime.now());
            entity.setStatus("RUNNING");
            entity.setStartTime(task.getStartTime());
            backupTaskRepository.save(entity);
            
            // 创建备份目录
            Path backupDir = createBackupDirectory(backupName);
            
            // 备份数据库数据
            Path dbBackupFile = backupDir.resolve("database.json");
            backupDatabase(dbBackupFile);
            task.setDbBackupPath(dbBackupFile.toString());
            
            // 备份文件数据（如果需要）
            if (includeFiles) {
                Path filesBackupFile = backupDir.resolve("files.zip");
                long fileCount = backupFiles(filesBackupFile);
                task.setFilesBackupPath(filesBackupFile.toString());
                task.setBackedUpFileCount(fileCount);
            }
            
            // 生成备份元数据
            BackupMetadata metadata = generateBackupMetadata(backupName, includeFiles);
            // 计算并写入校验和
            if (Files.exists(dbBackupFile)) {
                String dbChecksum = computeSHA256(dbBackupFile);
                metadata.setDbChecksum(dbChecksum);
            }

            if (includeFiles) {
                Path filesBackup = backupDir.resolve("files.zip");
                if (Files.exists(filesBackup)) {
                    String filesChecksum = computeSHA256(filesBackup);
                    metadata.setFilesChecksum(filesChecksum);
                }
            }

            Path metadataFile = backupDir.resolve("metadata.json");
            objectMapper.writeValue(metadataFile.toFile(), metadata);
            task.setMetadataPath(metadataFile.toString());
            
            task.setStatus("COMPLETED");
            task.setEndTime(LocalDateTime.now());
            task.setSuccess(true);
            entity.setStatus("COMPLETED");
            entity.setEndTime(task.getEndTime());
            entity.setSuccess(true);
            entity.setDbBackupPath(task.getDbBackupPath());
            entity.setFilesBackupPath(task.getFilesBackupPath());
            entity.setMetadataPath(task.getMetadataPath());
            entity.setBackedUpFileCount(task.getBackedUpFileCount());
            backupTaskRepository.save(entity);
            
            log.info("完整备份创建成功: 任务ID={}, 备份名称={}", taskId, backupName);
            
            return new BackupResult(taskId, true, "备份创建成功", backupDir.toString());
            
        } catch (Exception e) {
            task.setStatus("FAILED");
            task.setEndTime(LocalDateTime.now());
            task.setSuccess(false);
            task.setErrorMessage(e.getMessage());
            entity.setStatus("FAILED");
            entity.setEndTime(task.getEndTime());
            entity.setSuccess(false);
            entity.setErrorMessage(e.getMessage());
            backupTaskRepository.save(entity);
            
            log.error("完整备份创建失败: 任务ID={}, 错误={}", taskId, e.getMessage(), e);
            throw new BusinessException("备份创建失败: " + e.getMessage());
        }
    }
    
    /**
     * 创建增量备份
     */
    public BackupResult createIncrementalBackup(String backupName, LocalDateTime sinceTime) {
        String taskId = UUID.randomUUID().toString();
        BackupTask task = new BackupTask(taskId, backupName, "INCREMENTAL");
        BackupTaskEntity entity = new BackupTaskEntity();
        entity.setTaskId(taskId);
        entity.setBackupName(backupName);
        entity.setBackupType("INCREMENTAL");
        entity.setStatus("PENDING");
        backupTaskRepository.save(entity);
        
        try {
            task.setStatus("RUNNING");
            task.setStartTime(LocalDateTime.now());
            entity.setStatus("RUNNING");
            entity.setStartTime(task.getStartTime());
            backupTaskRepository.save(entity);
            
            // 创建备份目录
            Path backupDir = createBackupDirectory(backupName);
            
            // 备份增量数据库数据
            Path dbBackupFile = backupDir.resolve("database_incremental.json");
            backupIncrementalDatabase(dbBackupFile, sinceTime);
            task.setDbBackupPath(dbBackupFile.toString());
            
            // 备份增量文件
            Path filesBackupFile = backupDir.resolve("files_incremental.zip");
            long fileCount = backupIncrementalFiles(filesBackupFile, sinceTime);
            task.setFilesBackupPath(filesBackupFile.toString());
            task.setBackedUpFileCount(fileCount);
            
            // 生成备份元数据
            BackupMetadata metadata = generateIncrementalBackupMetadata(backupName, sinceTime);
            // 计算并写入校验和
            if (Files.exists(dbBackupFile)) {
                String dbChecksum = computeSHA256(dbBackupFile);
                metadata.setDbChecksum(dbChecksum);
            }
            Path filesBackup = backupDir.resolve("files_incremental.zip");
            if (Files.exists(filesBackup)) {
                String filesChecksum = computeSHA256(filesBackup);
                metadata.setFilesChecksum(filesChecksum);
            }
            Path metadataFile = backupDir.resolve("metadata.json");
            objectMapper.writeValue(metadataFile.toFile(), metadata);
            task.setMetadataPath(metadataFile.toString());
            
            task.setStatus("COMPLETED");
            task.setEndTime(LocalDateTime.now());
            task.setSuccess(true);
            entity.setStatus("COMPLETED");
            entity.setEndTime(task.getEndTime());
            entity.setSuccess(true);
            entity.setDbBackupPath(task.getDbBackupPath());
            entity.setFilesBackupPath(task.getFilesBackupPath());
            entity.setMetadataPath(task.getMetadataPath());
            entity.setBackedUpFileCount(task.getBackedUpFileCount());
            backupTaskRepository.save(entity);
            
            log.info("增量备份创建成功: 任务ID={}, 备份名称={}", taskId, backupName);
            
            return new BackupResult(taskId, true, "增量备份创建成功", backupDir.toString());
            
        } catch (Exception e) {
            task.setStatus("FAILED");
            task.setEndTime(LocalDateTime.now());
            task.setSuccess(false);
            task.setErrorMessage(e.getMessage());
            entity.setStatus("FAILED");
            entity.setEndTime(task.getEndTime());
            entity.setSuccess(false);
            entity.setErrorMessage(e.getMessage());
            backupTaskRepository.save(entity);
            
            log.error("增量备份创建失败: 任务ID={}, 错误={}", taskId, e.getMessage(), e);
            throw new BusinessException("增量备份创建失败: " + e.getMessage());
        }
    }
    
    /**
     * 异步创建备份
     */
    public CompletableFuture<BackupResult> createBackupAsync(String backupName, String backupType, 
                                                           boolean includeFiles, LocalDateTime sinceTime) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                if ("FULL".equals(backupType)) {
                    return createFullBackup(backupName, includeFiles);
                } else if ("INCREMENTAL".equals(backupType)) {
                    return createIncrementalBackup(backupName, sinceTime);
                } else {
                    throw new BusinessException("不支持的备份类型: " + backupType);
                }
            } catch (Exception e) {
                throw new RuntimeException("异步备份失败: " + e.getMessage(), e);
            }
        });
    }
    
    /**
     * 恢复数据
     */
    public RestoreResult restoreFromBackup(String backupPath, boolean restoreFiles) {
        String taskId = UUID.randomUUID().toString();
        RestoreTask task = new RestoreTask(taskId, backupPath);
        task.setStatus("RUNNING");
        task.setStartTime(LocalDateTime.now());
        
        try {
            Path backupDir = Paths.get(backupPath);
            if (!Files.exists(backupDir)) {
                throw new BusinessException("备份目录不存在: " + backupPath);
            }
            
            // 读取元数据
            Path metadataFile = backupDir.resolve("metadata.json");
            BackupMetadata metadata = objectMapper.readValue(metadataFile.toFile(), BackupMetadata.class);
            task.setBackupMetadata(metadata);
            
            // 校验并恢复文件先于数据库，方便插入文件元数据时使用
            Map<String, String> restoredFilesMap = Collections.emptyMap();
            if (restoreFiles) {
                Path filesBackupFile = backupDir.resolve("files.zip");
                if (Files.exists(filesBackupFile)) {
                    if (metadata.getFilesChecksum() != null) {
                        String filesChecksum = computeSHA256(filesBackupFile);
                        if (!metadata.getFilesChecksum().equals(filesChecksum)) {
                            throw new BusinessException("文件备份校验失败: checksum 不匹配");
                        }
                    }
                    restoredFilesMap = restoreFiles(filesBackupFile);
                    task.setFilesRestored(true);
                }
            }
            
            // 恢复数据库数据（并校验 checksum）
            Path dbBackupFile = backupDir.resolve("database.json");
            if (Files.exists(dbBackupFile)) {
                if (metadata.getDbChecksum() != null) {
                    String dbChecksum = computeSHA256(dbBackupFile);
                    if (!metadata.getDbChecksum().equals(dbChecksum)) {
                        throw new BusinessException("数据库备份校验失败: checksum 不匹配");
                    }
                }
                // 传入 restoredFilesMap 以便恢复文件元数据时使用
                restoreDatabase(dbBackupFile, restoredFilesMap);
                task.setDbRestored(true);
            }
            
            task.setStatus("COMPLETED");
            task.setEndTime(LocalDateTime.now());
            task.setSuccess(true);
            
            log.info("数据恢复成功: 任务ID={}, 备份路径={}", taskId, backupPath);
            
            return new RestoreResult(taskId, true, "数据恢复成功");
            
        } catch (Exception e) {
            task.setStatus("FAILED");
            task.setEndTime(LocalDateTime.now());
            task.setSuccess(false);
            task.setErrorMessage(e.getMessage());
            
            log.error("数据恢复失败: 任务ID={}, 备份路径={}, 错误={}", taskId, backupPath, e.getMessage(), e);
            throw new BusinessException("数据恢复失败: " + e.getMessage());
        }
    }
    
    /**
     * 解压文件备份到 storageBasePath/restored/{timestamp}/ 下，返回 storageName->相对路径 映射
     */
    private Map<String, String> restoreFiles(Path backupFile) throws IOException {
        Map<String, String> restored = new HashMap<>();
        String timestamp = LocalDateTime.now().toString().replace(":", "-");
        Path targetDir = Paths.get(storageBasePath).toAbsolutePath().normalize().resolve("restored").resolve(timestamp);
        Files.createDirectories(targetDir);

        try (ZipInputStream zis = new ZipInputStream(Files.newInputStream(backupFile))) {
            java.util.zip.ZipEntry entry;
            byte[] buffer = new byte[8192];
            while ((entry = zis.getNextEntry()) != null) {
                String entryName = entry.getName();
                // 防止zip穿越
                Path resolved = targetDir.resolve(entryName).normalize();
                if (!resolved.startsWith(targetDir)) {
                    log.warn("跳过不安全的压缩条目: {}", entryName);
                    continue;
                }

                if (entry.isDirectory()) {
                    Files.createDirectories(resolved);
                } else {
                    Files.createDirectories(resolved.getParent());
                    try (OutputStream out = Files.newOutputStream(resolved)) {
                        int len;
                        while ((len = zis.read(buffer)) != -1) {
                            out.write(buffer, 0, len);
                        }
                    }
                    // 存储相对路径，便于后续插入元数据
                    Path relative = Paths.get(storageBasePath).toAbsolutePath().normalize().relativize(resolved);
                    restored.put(entryName, relative.toString());
                }
                zis.closeEntry();
            }
        }

        log.info("文件恢复完成，解压到: {}，文件数量={}", targetDir, restored.size());
        return restored;
    }

    /**
     * 恢复数据库中用户和文件元数据。filesMap 中的 key 为 storageName，value 为相对路径（相对于 storageBasePath）
     */
    @Transactional
    private void restoreDatabase(Path backupFile, Map<String, String> filesMap) throws IOException {
        Map<String, Object> data = objectMapper.readValue(backupFile.toFile(), new TypeReference<Map<String, Object>>() {});

        // 恢复用户
        if (data.containsKey("userData")) {
            Map<?,?> userData = (Map<?,?>) data.get("userData");
            Object usersObj = userData.get("users");
            if (usersObj instanceof Collection) {
                Collection<?> usersCol = (Collection<?>) usersObj;
                for (Object uObj : usersCol) {
                    User u = objectMapper.convertValue(uObj, User.class);
                    // 尝试按 username 或 email 查找
                    Optional<User> exist = Optional.empty();
                    if (u.getUsername() != null) exist = userRepository.findByUsername(u.getUsername());
                    if (!exist.isPresent() && u.getEmail() != null) exist = userRepository.findByEmail(u.getEmail());

                    if (exist.isPresent()) {
                        User ex = exist.get();
                        // 更新非敏感字段
                        ex.setNickname(u.getNickname());
                        ex.setAvatar(u.getAvatar());
                        ex.setRole(u.getRole());
                        ex.setStatus(u.getStatus());
                        userRepository.save(ex);
                    } else {
                        // 新建用户，密码设置为随机值，激活状态
                        u.setPassword(UUID.randomUUID().toString());
                        if (u.getStatus() == null) u.setStatus(User.UserStatus.ACTIVE);
                        userRepository.save(u);
                    }
                }
            }
        }

        // 恢复文件元数据（如果存在），仅当对应恢复后的文件路径可用且 uploader 能解析时创建
        if (data.containsKey("fileData")) {
            Map<?,?> fileData = (Map<?,?>) data.get("fileData");
            Object filesObj = fileData.get("files");
            if (filesObj instanceof Collection) {
                Collection<?> filesCol = (Collection<?>) filesObj;
                for (Object fObj : filesCol) {
                    Map<?,?> fMap = objectMapper.convertValue(fObj, new TypeReference<Map<String,Object>>() {});
                    String storageName = (String) fMap.get("storageName");
                    if (storageName == null) continue;
                    // 如果文件元数据已存在则跳过
                    if (fileRepository.findByStorageName(storageName).isPresent()) continue;

                    // 找到恢复后的文件路径
                    String relPath = filesMap.get(storageName);
                    if (relPath == null) {
                        log.warn("找不到恢复文件的路径，跳过元数据恢复: {}", storageName);
                        continue;
                    }

                    // 解析 uploader
                    User uploader = null;
                    Object uploaderObj = fMap.get("uploader");
                    if (uploaderObj instanceof Map) {
                        Map<?,?> up = (Map<?,?>) uploaderObj;
                        String username = (String) up.get("username");
                        String email = (String) up.get("email");
                        if (username != null) uploader = userRepository.findByUsername(username).orElse(null);
                        if (uploader == null && email != null) uploader = userRepository.findByEmail(email).orElse(null);
                    }

                    if (uploader == null) {
                        log.warn("无法解析上传者，跳过文件元数据恢复: {}", storageName);
                        continue;
                    }

                    FileEntity fe = new FileEntity();
                    fe.setStorageName(storageName);
                    Object originalNameObj = fMap.get("originalName");
                    fe.setOriginalName(originalNameObj instanceof String ? (String) originalNameObj : storageName);
                    fe.setFilePath("/" + relPath.replace('\\', '/'));
                    fe.setFileSize(fMap.get("fileSize") != null ? ((Number) fMap.get("fileSize")).longValue() : Files.size(Paths.get(storageBasePath).resolve(relPath)));
                    Object contentTypeObj = fMap.get("contentType");
                    fe.setContentType(contentTypeObj instanceof String ? (String) contentTypeObj : null);
                    Object extensionObj = fMap.get("extension");
                    fe.setExtension(extensionObj instanceof String ? (String) extensionObj : null);
                    Object md5HashObj = fMap.get("md5Hash");
                    fe.setMd5Hash(md5HashObj instanceof String ? (String) md5HashObj : null);
                    fe.setStatus(FileEntity.FileStatus.AVAILABLE);
                    Object isPublicObj = fMap.get("isPublic");
                    fe.setIsPublic(isPublicObj instanceof Boolean ? (Boolean) isPublicObj : false);
                    fe.setDownloadCount(fMap.get("downloadCount") != null ? ((Number) fMap.get("downloadCount")).intValue() : 0);
                    fe.setPreviewCount(fMap.get("previewCount") != null ? ((Number) fMap.get("previewCount")).intValue() : 0);
                    fe.setShareCount(fMap.get("shareCount") != null ? ((Number) fMap.get("shareCount")).intValue() : 0);
                    fe.setUploader(uploader);

                    fileRepository.save(fe);
                }
            }
        }
    }
    
    /**
     * 获取备份列表
     */
    public List<BackupInfo> listBackups() {
        List<BackupInfo> backups = new ArrayList<>();
        
        try {
            Path backupBaseDir = Paths.get(backupBasePath);
            if (!Files.exists(backupBaseDir)) {
                return backups;
            }
            
            Files.list(backupBaseDir)
                .filter(Files::isDirectory)
                .forEach(dir -> {
                    try {
                        BackupInfo info = new BackupInfo();
                        info.setBackupPath(dir.toString());
                        info.setBackupName(dir.getFileName().toString());
                        
                        // 读取元数据
                        Path metadataFile = dir.resolve("metadata.json");
                        if (Files.exists(metadataFile)) {
                            BackupMetadata metadata = objectMapper.readValue(metadataFile.toFile(), BackupMetadata.class);
                            info.setBackupType(metadata.getBackupType());
                            info.setCreateTime(metadata.getCreateTime());
                            info.setIncludeFiles(metadata.getIncludeFiles());
                            info.setTotalFileSize(metadata.getTotalFileSize());
                            info.setFileCount(metadata.getFileCount());
                        }
                        
                        // 检查备份完整性
                        info.setValid(checkBackupIntegrity(dir));
                        
                        backups.add(info);
                    } catch (Exception e) {
                        log.warn("读取备份信息失败: 目录={}", dir, e);
                    }
                });
                
        } catch (Exception e) {
            log.error("获取备份列表失败", e);
        }
        
        return backups;
    }
    
    /**
     * 删除备份
     */
    public void deleteBackup(String backupPath) {
        try {
            Path backupDir = Paths.get(backupPath);
            if (Files.exists(backupDir)) {
                // 递归删除备份目录
                Files.walk(backupDir)
                    .sorted(Comparator.reverseOrder())
                    .forEach(path -> {
                        try {
                            Files.delete(path);
                        } catch (IOException e) {
                            log.warn("删除文件失败: {}", path, e);
                        }
                    });
                log.info("备份删除成功: {}", backupPath);
            }
        } catch (Exception e) {
            log.error("删除备份失败: {}", backupPath, e);
            throw new BusinessException("删除备份失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取备份任务状态
     */
    public BackupTask getBackupTask(String taskId) {
        // 从数据库读取任务信息
        Optional<BackupTaskEntity> opt = backupTaskRepository.findById(taskId);
        if (opt.isPresent()) {
            BackupTaskEntity e = opt.get();
            BackupTask t = new BackupTask();
            t.setTaskId(e.getTaskId());
            t.setBackupName(e.getBackupName());
            t.setBackupType(e.getBackupType());
            t.setStatus(e.getStatus());
            t.setStartTime(e.getStartTime());
            t.setEndTime(e.getEndTime());
            t.setSuccess(e.getSuccess());
            t.setErrorMessage(e.getErrorMessage());
            t.setDbBackupPath(e.getDbBackupPath());
            t.setFilesBackupPath(e.getFilesBackupPath());
            t.setMetadataPath(e.getMetadataPath());
            t.setBackedUpFileCount(e.getBackedUpFileCount());
            return t;
        }

        return null;
    }
    
    /**
     * 清理过期备份
     */
    public CleanupResult cleanupExpiredBackups(int daysToKeep) {
        CleanupResult result = new CleanupResult();
        result.setStartTime(LocalDateTime.now());
        
        try {
            LocalDateTime cutoffTime = LocalDateTime.now().minusDays(daysToKeep);
            List<BackupInfo> backups = listBackups();
            
            for (BackupInfo backup : backups) {
                LocalDateTime createTime = backup.getCreateTime();
                if (createTime != null && createTime.isBefore(cutoffTime)) {
                    try {
                        deleteBackup(backup.getBackupPath());
                        result.getDeletedBackups().add(backup.getBackupName());
                        result.setDeletedCount(result.getDeletedCount() + 1);
                    } catch (Exception e) {
                        log.warn("删除过期备份失败: {}", backup.getBackupName(), e);
                        result.getFailedDeletions().add(backup.getBackupName() + ": " + e.getMessage());
                    }
                }
            }
            
            result.setEndTime(LocalDateTime.now());
            result.setSuccess(true);
            result.setMessage("清理完成");
            
        } catch (Exception e) {
            result.setEndTime(LocalDateTime.now());
            result.setSuccess(false);
            result.setMessage("清理失败: " + e.getMessage());
        }
        
        return result;
    }
    
    // ==================== 私有辅助方法 ====================
    
    private Path createBackupDirectory(String backupName) throws IOException {
        Path backupBaseDir = Paths.get(backupBasePath);
        if (!Files.exists(backupBaseDir)) {
            Files.createDirectories(backupBaseDir);
        }
        
        String timestamp = LocalDateTime.now().toString().replace(":", "-");
        Path backupDir = backupBaseDir.resolve(backupName + "_" + timestamp);
        Files.createDirectories(backupDir);
        
        return backupDir;
    }
    
    private void backupDatabase(Path outputFile) throws IOException {
        // 备份用户数据
        List<User> users = userRepository.findAll();
        Map<String, Object> userData = new HashMap<>();
        userData.put("users", users);
        userData.put("exportTime", LocalDateTime.now());
        
        // 备份文件数据
        List<FileEntity> files = fileRepository.findAll();
        Map<String, Object> fileData = new HashMap<>();
        fileData.put("files", files);
        fileData.put("exportTime", LocalDateTime.now());
        
        // 合并数据
        Map<String, Object> backupData = new HashMap<>();
        backupData.put("userData", userData);
        backupData.put("fileData", fileData);
        backupData.put("version", "1.0");
        
        objectMapper.writeValue(outputFile.toFile(), backupData);
    }
    
    private void backupIncrementalDatabase(Path outputFile, LocalDateTime sinceTime) throws IOException {
        // 备份增量用户数据
        Iterable<User> usersIterable = userRepository.findByCreatedAtAfter(sinceTime);
        List<User> users = new ArrayList<>();
        usersIterable.forEach(users::add);
        Map<String, Object> userData = new HashMap<>();
        userData.put("users", users);
        userData.put("exportTime", LocalDateTime.now());
        userData.put("sinceTime", sinceTime);
        
        // 备份增量文件数据
        Iterable<FileEntity> filesIterable = fileRepository.findByCreatedAtAfter(sinceTime);
        List<FileEntity> files = new ArrayList<>();
        filesIterable.forEach(files::add);
        Map<String, Object> fileData = new HashMap<>();
        fileData.put("files", files);
        fileData.put("exportTime", LocalDateTime.now());
        fileData.put("sinceTime", sinceTime);
        
        // 合并数据
        Map<String, Object> backupData = new HashMap<>();
        backupData.put("userData", userData);
        backupData.put("fileData", fileData);
        backupData.put("version", "1.0");
        backupData.put("incremental", true);
        
        objectMapper.writeValue(outputFile.toFile(), backupData);
    }
    
    private long backupFiles(Path outputFile) throws IOException {
        List<FileEntity> files = fileRepository.findAll();
        long totalSize = 0;
        long backedUpCount = 0;

        try (ZipOutputStream zipOut = new ZipOutputStream(new FileOutputStream(outputFile.toFile()))) {
            zipOut.setLevel(compressionLevel);
            byte[] buffer = new byte[8192];

            for (FileEntity file : files) {
                Resource resource = fileStorageUtil.loadAsResource(file.getStorageName(), file.getFilePath());
                if (resource == null || !resource.exists()) {
                    log.warn("备份时文件不存在或不可访问: storageName={}, filePath={}", file.getStorageName(), file.getFilePath());
                    continue;
                }

                ZipEntry zipEntry = new ZipEntry(file.getStorageName());
                zipOut.putNextEntry(zipEntry);

                try (InputStream in = resource.getInputStream()) {
                    int len;
                    while ((len = in.read(buffer)) != -1) {
                        zipOut.write(buffer, 0, len);
                        totalSize += len;

                        if (totalSize > maxBackupSize) {
                            zipOut.closeEntry();
                            throw new BusinessException("备份文件大小超过限制: " + maxBackupSize);
                        }
                    }
                }

                zipOut.closeEntry();
                backedUpCount++;
            }
        }

        return backedUpCount;
    }
    
    private long backupIncrementalFiles(Path outputFile, LocalDateTime sinceTime) throws IOException {
        Iterable<FileEntity> filesIterable = fileRepository.findByCreatedAtAfter(sinceTime);
        List<FileEntity> files = new ArrayList<>();
        filesIterable.forEach(files::add);
        long totalSize = 0;
        long backedUpCount = 0;

        try (ZipOutputStream zipOut = new ZipOutputStream(new FileOutputStream(outputFile.toFile()))) {
            zipOut.setLevel(compressionLevel);
            byte[] buffer = new byte[8192];

            for (FileEntity file : files) {
                Resource resource = fileStorageUtil.loadAsResource(file.getStorageName(), file.getFilePath());
                if (resource == null || !resource.exists()) {
                    log.warn("备份增量时文件不存在或不可访问: storageName={}, filePath={}", file.getStorageName(), file.getFilePath());
                    continue;
                }

                ZipEntry zipEntry = new ZipEntry(file.getStorageName());
                zipOut.putNextEntry(zipEntry);

                try (InputStream in = resource.getInputStream()) {
                    int len;
                    while ((len = in.read(buffer)) != -1) {
                        zipOut.write(buffer, 0, len);
                        totalSize += len;

                        if (totalSize > maxBackupSize) {
                            zipOut.closeEntry();
                            throw new BusinessException("备份文件大小超过限制: " + maxBackupSize);
                        }
                    }
                }

                zipOut.closeEntry();
                backedUpCount++;
            }
        }

        return backedUpCount;
    }
    
    private BackupMetadata generateBackupMetadata(String backupName, boolean includeFiles) {
        BackupMetadata metadata = new BackupMetadata();
        metadata.setBackupName(backupName);
        metadata.setBackupType("FULL");
        metadata.setCreateTime(LocalDateTime.now());
        metadata.setIncludeFiles(includeFiles);
        metadata.setVersion("1.0");
        
        if (includeFiles) {
            List<FileEntity> files = fileRepository.findAll();
            metadata.setFileCount((long) files.size());
            long totalSize = files.stream().mapToLong(FileEntity::getFileSize).sum();
            metadata.setTotalFileSize(totalSize);
        }
        
        return metadata;
    }
    
    private BackupMetadata generateIncrementalBackupMetadata(String backupName, LocalDateTime sinceTime) {
        BackupMetadata metadata = new BackupMetadata();
        metadata.setBackupName(backupName);
        metadata.setBackupType("INCREMENTAL");
        metadata.setCreateTime(LocalDateTime.now());
        metadata.setSinceTime(sinceTime);
        metadata.setIncludeFiles(true);
        metadata.setVersion("1.0");
        metadata.setIncremental(true);

        Iterable<FileEntity> filesIterable = fileRepository.findByCreatedAtAfter(sinceTime);
        List<FileEntity> files = new ArrayList<>();
        filesIterable.forEach(files::add);
        metadata.setFileCount((long) files.size());
        long totalSize = files.stream().mapToLong(FileEntity::getFileSize).sum();
        metadata.setTotalFileSize(totalSize);
        
        return metadata;
    }
    
    private String computeSHA256(Path file) throws IOException {
        try (InputStream in = Files.newInputStream(file)) {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] buffer = new byte[8192];
            int read;
            while ((read = in.read(buffer)) != -1) {
                md.update(buffer, 0, read);
            }
            byte[] digest = md.digest();
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IOException(e);
        }
    }

    /**
     * 检查备份完整性
     */
    private boolean checkBackupIntegrity(Path backupDir) {
        try {
            if (!Files.exists(backupDir)) {
                return false;
            }
            
            // 检查 metadata.json 文件是否存在
            Path metadataPath = backupDir.resolve("metadata.json");
            if (!Files.exists(metadataPath)) {
                return false;
            }
            
            // 检查备份文件是否完整（至少包含一个文件）
            try (var stream = Files.list(backupDir)) {
                return stream.anyMatch(p -> !p.getFileName().toString().equals("metadata.json"));
            }
        } catch (Exception e) {
            log.warn("检查备份完整性时出错: {}", backupDir, e);
            return false;
        }
    }

    // ==================== DTO类定义 ====================
    
    @Data
    public static class BackupResult {
        private String taskId;
        private Boolean success;
        private String message;
        private String backupPath;
        
        public BackupResult() {}
        
        public BackupResult(String taskId, Boolean success, String message, String backupPath) {
            this.taskId = taskId;
            this.success = success;
            this.message = message;
            this.backupPath = backupPath;
        }
    }
    
    @Data
    public static class RestoreResult {
        private String taskId;
        private Boolean success;
        private String message;
        
        public RestoreResult() {}
        
        public RestoreResult(String taskId, Boolean success, String message) {
            this.taskId = taskId;
            this.success = success;
            this.message = message;
        }
    }
    
    @Data
    public static class BackupTask {
        private String taskId;
        private String backupName;
        private String backupType;
        private String status;
        private LocalDateTime startTime;
        private LocalDateTime endTime;
        private Boolean success;
        private String errorMessage;
        private String dbBackupPath;
        private String filesBackupPath;
        private String metadataPath;
        private Long backedUpFileCount;
        
        public BackupTask() {}
        
        public BackupTask(String taskId, String backupName, String backupType) {
            this.taskId = taskId;
            this.backupName = backupName;
            this.backupType = backupType;
        }
    }
    
    @Data
    public static class RestoreTask {
        private String taskId;
        private String backupPath;
        private String status;
        private LocalDateTime startTime;
        private LocalDateTime endTime;
        private Boolean success;
        private String errorMessage;
        private Boolean dbRestored;
        private Boolean filesRestored;
        private BackupMetadata backupMetadata;
        
        public RestoreTask() {}
        
        public RestoreTask(String taskId, String backupPath) {
            this.taskId = taskId;
            this.backupPath = backupPath;
        }
    }
    
    @Data
    public static class BackupInfo {
        private String backupName;
        private String backupPath;
        private String backupType;
        private LocalDateTime createTime;
        private Boolean includeFiles;
        private Long totalFileSize;
        private Long fileCount;
        private Boolean valid;
    }
    
    @Data
    public static class BackupMetadata {
        private String backupName;
        private String backupType;
        private LocalDateTime createTime;
        private LocalDateTime sinceTime;
        private Boolean includeFiles;
        private Boolean incremental;
        private String version;
        private Long fileCount;
        private Long totalFileSize;
        private String dbChecksum;
        private String filesChecksum;
    }
    
    @Data
    public static class CleanupResult {
        private LocalDateTime startTime;
        private LocalDateTime endTime;
        private Boolean success;
        private String message;
        private Integer deletedCount = 0;
        private List<String> deletedBackups = new ArrayList<>();
        private List<String> failedDeletions = new ArrayList<>();
    }
}