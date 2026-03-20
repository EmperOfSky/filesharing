package com.filesharing.service.impl;

import com.filesharing.util.FileStorageUtil;
import com.filesharing.dto.FileUploadResponse;
import com.filesharing.dto.FileResponse;
import com.filesharing.dto.request.MobileUploadRequest;
import com.filesharing.dto.response.FileSimpleResponse;
import com.filesharing.entity.FileEntity;
import com.filesharing.entity.Folder;
import com.filesharing.entity.User;
import com.filesharing.exception.BusinessException;
import com.filesharing.repository.FileRepository;
import com.filesharing.repository.FolderRepository;
import com.filesharing.service.FileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

/**
 * 文件服务实现类
 */
@Slf4j
@Service
@Primary
@RequiredArgsConstructor
@Transactional
public class FileServiceImpl implements FileService {
    
    private final FileRepository fileRepository;
    private final FolderRepository folderRepository;
    private final FileStorageUtil fileStorageUtil;
    private final ConcurrentMap<Long, Set<Long>> userFavoriteFileIds = new ConcurrentHashMap<>();
    
    @Override
    public FileUploadResponse uploadFile(MultipartFile file, Long folderId, User uploader) {
        try {
            if (uploader == null) {
                throw new BusinessException("未获取到当前用户信息");
            }

            Folder targetFolder = null;
            if (folderId != null) {
                targetFolder = folderRepository.findById(folderId)
                        .orElseThrow(() -> new BusinessException("目标文件夹不存在"));
                if (!targetFolder.getOwner().getId().equals(uploader.getId())) {
                    throw new BusinessException("无权上传到该文件夹");
                }
            }

            // 计算文件MD5（用于秒传）
            String md5Hash = fileStorageUtil.calculateMD5(file);
            
            // 检查文件是否已存在（秒传功能）
            FileEntity existingFile = checkFileExists(md5Hash);
            if (existingFile != null) {
                boolean sameOwner = existingFile.getUploader() != null
                    && existingFile.getUploader().getId().equals(uploader.getId());
                boolean sameFolder = (existingFile.getFolder() == null && targetFolder == null)
                    || (existingFile.getFolder() != null && targetFolder != null
                    && existingFile.getFolder().getId().equals(targetFolder.getId()));

                if (sameOwner && sameFolder) {
                    log.info("文件秒传成功（复用已有记录）：{} -> {}", file.getOriginalFilename(), existingFile.getStorageName());
                    return FileUploadResponse.builder()
                        .id(existingFile.getId())
                        .originalName(existingFile.getOriginalName())
                        .storageName(existingFile.getStorageName())
                        .fileSize(existingFile.getFileSize())
                        .contentType(existingFile.getContentType())
                        .downloadUrl("/api/files/" + existingFile.getId() + "/download")
                        .previewUrl("/api/files/" + existingFile.getId() + "/preview")
                        .isNewlyUploaded(false)
                        .message("文件秒传成功")
                        .build();
                }

                FileEntity copiedMeta = createFastUploadCopy(existingFile, file, targetFolder, uploader, md5Hash);
                long usedStorage = uploader.getUsedStorage() == null ? 0L : uploader.getUsedStorage();
                uploader.setUsedStorage(usedStorage + copiedMeta.getFileSize());

                log.info("文件秒传成功（创建当前用户副本）：{} -> {}", file.getOriginalFilename(), copiedMeta.getStorageName());
                return FileUploadResponse.builder()
                    .id(copiedMeta.getId())
                    .originalName(copiedMeta.getOriginalName())
                    .storageName(copiedMeta.getStorageName())
                    .fileSize(copiedMeta.getFileSize())
                    .contentType(copiedMeta.getContentType())
                    .downloadUrl("/api/files/" + copiedMeta.getId() + "/download")
                    .previewUrl("/api/files/" + copiedMeta.getId() + "/preview")
                    .isNewlyUploaded(false)
                    .message("文件秒传成功")
                    .build();
            }
            
            // 实际上传文件
            String storageName = fileStorageUtil.saveFile(file);
            String originalFilename = file.getOriginalFilename();
            String extension = fileStorageUtil.getFileExtension(originalFilename);
            
            // 创建文件实体
            FileEntity fileEntity = new FileEntity();
            fileEntity.setOriginalName(originalFilename);
            fileEntity.setStorageName(storageName);
            fileEntity.setFilePath("/uploads/" + storageName);
            fileEntity.setFileSize(file.getSize());
            fileEntity.setContentType(file.getContentType());
            fileEntity.setExtension(extension);
            fileEntity.setMd5Hash(md5Hash);
            fileEntity.setStatus(FileEntity.FileStatus.AVAILABLE);
            fileEntity.setUploader(uploader);
            fileEntity.setFolder(targetFolder);
            
            // 保存文件实体
            FileEntity savedFile = fileRepository.save(fileEntity);
            
            // 更新用户存储使用量
            long usedStorage = uploader.getUsedStorage() == null ? 0L : uploader.getUsedStorage();
            uploader.setUsedStorage(usedStorage + file.getSize());
            
            log.info("文件上传成功：{} -> {}", originalFilename, storageName);
                        
            return FileUploadResponse.builder()
                    .id(savedFile.getId())
                    .originalName(savedFile.getOriginalName())
                    .storageName(savedFile.getStorageName())
                    .fileSize(savedFile.getFileSize())
                    .contentType(savedFile.getContentType())
                    .downloadUrl("/api/files/" + savedFile.getId() + "/download")
                    .previewUrl("/api/files/" + savedFile.getId() + "/preview")
                    .isNewlyUploaded(true)
                    .message("文件上传成功")
                    .build();
                    
        } catch (BusinessException e) {
            log.error("业务异常 - 文件上传失败: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("文件上传失败: {}", e.getMessage(), e);
            throw new BusinessException("文件上传失败: " + e.getMessage(), e);
        }
    }
    
    @Override
    @Transactional(readOnly = true)
    public FileResponse getFileById(Long fileId) {
        FileEntity fileEntity = fileRepository.findById(fileId)
                .orElseThrow(() -> new BusinessException("文件不存在"));
        
        return convertToFileResponse(fileEntity);
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<FileResponse> getUserFiles(User user, String fileName, int page, int size) {
        var pageable = PageRequest.of(Math.max(page, 0), Math.max(size, 1), Sort.by(Sort.Direction.DESC, "createdAt"));
        boolean hasKeyword = fileName != null && !fileName.trim().isEmpty();
        var pageResult = hasKeyword
            ? fileRepository.findByUploaderAndOriginalNameContaining(user, fileName.trim(), pageable)
            : fileRepository.findByUploader(user, pageable);

        return pageResult
                .getContent()
                .stream()
            .filter(file -> file.getStatus() == FileEntity.FileStatus.AVAILABLE)
                .map(this::convertToFileResponse)
                .collect(Collectors.toList());
    }
    
    @Override
    public FileEntity downloadFile(Long fileId) {
        FileEntity fileEntity = fileRepository.findById(fileId)
                .orElseThrow(() -> new BusinessException("文件不存在"));
        
        // 增加下载次数
        fileEntity.setDownloadCount(fileEntity.getDownloadCount() + 1);
        fileRepository.save(fileEntity);
        
        return fileEntity;
    }
    
    @Override
    public void deleteFile(Long fileId, User currentUser) {
        FileEntity fileEntity = fileRepository.findById(fileId)
                .orElseThrow(() -> new BusinessException("文件不存在"));
        
        // 验证权限
        if (!fileEntity.getUploader().getId().equals(currentUser.getId())) {
            throw new BusinessException("无权删除此文件");
        }
        
        // 物理删除文件
        boolean deleted = fileStorageUtil.deleteFile(fileEntity.getStorageName());
        if (deleted) {
            // 更新用户存储使用量
            long usedStorage = currentUser.getUsedStorage() == null ? 0L : currentUser.getUsedStorage();
            long fileSize = fileEntity.getFileSize() == null ? 0L : fileEntity.getFileSize();
            currentUser.setUsedStorage(Math.max(0, usedStorage - fileSize));
        }
        
        fileEntity.setStatus(FileEntity.FileStatus.DELETED);
        fileEntity.setDeletedAt(LocalDateTime.now());
        fileRepository.save(fileEntity);
        
        log.info("文件删除成功: {}", fileEntity.getOriginalName());
    }
    
    @Override
    public FileResponse moveFile(Long fileId, Long targetFolderId, User currentUser) {
        FileEntity fileEntity = requireOwnedAvailableFile(fileId, currentUser);
        Folder targetFolder = resolveTargetFolder(targetFolderId, currentUser);

        fileEntity.setFolder(targetFolder);
        FileEntity saved = fileRepository.save(fileEntity);
        return convertToFileResponse(saved);
    }
    
    @Override
    public FileResponse renameFile(Long fileId, String newName, User currentUser) {
        if (newName == null || newName.trim().isEmpty()) {
            throw new BusinessException("新文件名不能为空");
        }

        FileEntity fileEntity = requireOwnedAvailableFile(fileId, currentUser);
        String trimmedName = newName.trim();
        fileEntity.setOriginalName(trimmedName);
        fileEntity.setExtension(getFileExtension(trimmedName));

        FileEntity saved = fileRepository.save(fileEntity);
        return convertToFileResponse(saved);
    }

    @Override
    public FileResponse copyFile(Long fileId, Long targetFolderId, User currentUser) {
        FileEntity source = requireOwnedAvailableFile(fileId, currentUser);
        Folder targetFolder = resolveTargetFolder(targetFolderId, currentUser);

        String copiedName = appendCopySuffix(source.getOriginalName());
        FileEntity copied = createCopyFromExisting(source, copiedName, targetFolder, currentUser);
        long usedStorage = currentUser.getUsedStorage() == null ? 0L : currentUser.getUsedStorage();
        long fileSize = copied.getFileSize() == null ? 0L : copied.getFileSize();
        currentUser.setUsedStorage(usedStorage + fileSize);

        return convertToFileResponse(copied);
    }
    
    @Override
    public FileResponse setFilePublic(Long fileId, Boolean isPublic, User currentUser) {
        FileEntity fileEntity = requireOwnedAvailableFile(fileId, currentUser);
        fileEntity.setIsPublic(Boolean.TRUE.equals(isPublic));
        FileEntity saved = fileRepository.save(fileEntity);
        return convertToFileResponse(saved);
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<FileResponse> getPublicFiles(int page, int size) {
        return fileRepository.findPublicFiles(
                org.springframework.data.domain.PageRequest.of(page, size))
                .getContent()
                .stream()
                .map(this::convertToFileResponse)
                .collect(Collectors.toList());
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<FileResponse> searchFilesByExtension(String extension, int page, int size) {
        return fileRepository.findByExtension(extension,
                org.springframework.data.domain.PageRequest.of(page, size))
                .getContent()
                .stream()
                .map(this::convertToFileResponse)
                .collect(Collectors.toList());
    }
    
    @Override
    @Transactional(readOnly = true)
    public FileEntity checkFileExists(String md5Hash) {
        List<FileEntity> existingFiles = fileRepository.findByMd5Hash(md5Hash);
        return existingFiles.stream()
                .filter(f -> f.getStatus() == FileEntity.FileStatus.AVAILABLE)
                .findFirst()
                .orElse(null);
    }
    
    @Override
    @Transactional(readOnly = true)
    public FileEntity getFileEntityById(Long fileId) {
        return fileRepository.findById(fileId)
                .orElseThrow(() -> new BusinessException("文件不存在"));
    }
    
    @Override
    public void cleanupDeletedFiles() {
        try {
            // 查找标记为删除且超过保留期限的文件
            LocalDateTime retentionDate = LocalDateTime.now().minusDays(30);
            List<FileEntity> filesToDelete = fileRepository.findDeletedFilesBefore(retentionDate);
                
            int deletedCount = 0;
            for (FileEntity file : filesToDelete) {
                try {
                    // 物理删除文件
                    if (fileStorageUtil.deleteFile(file.getStorageName())) {
                        // 从数据库彻底删除记录
                        fileRepository.delete(file);
                        deletedCount++;
                        log.info("清理文件：{}", file.getOriginalName());
                    }
                } catch (Exception e) {
                    log.error("清理文件失败：{}", file.getOriginalName(), e);
                }
            }
                
            log.info("文件清理完成，共清理 {} 个文件", deletedCount);
        } catch (Exception e) {
            log.error("文件清理任务执行失败", e);
        }
    }
    
    @Override
    public FileUploadResponse initChunkUpload(String fileName, Long fileSize, Long chunkSize,
                                            Integer totalChunks, Long folderId, User uploader) {
        try {
            String uploadId = UUID.randomUUID().toString().replace("-", "");
            
            // 这里应该调用分片上传服务
            // 暂时返回简化实现
            return FileUploadResponse.builder()
                    .id(null)
                    .originalName(fileName)
                    .storageName(uploadId)
                    .fileSize(fileSize)
                    .isNewlyUploaded(true)
                    .message("分片上传初始化成功")
                    .build();
        } catch (Exception e) {
            log.error("分片上传初始化失败", e);
            throw new BusinessException("初始化失败: " + e.getMessage());
        }
    }
    
    @Override
    public FileUploadResponse uploadChunk(MultipartFile chunk, String uploadId, Integer chunkIndex,
                                        Integer totalChunks, User uploader) {
        try {
            // 这里应该调用分片上传服务
            // 暂时返回简化实现
            return FileUploadResponse.builder()
                    .id(null)
                    .originalName("chunk_" + chunkIndex)
                    .storageName(uploadId + "_" + chunkIndex)
                    .fileSize(chunk.getSize())
                    .isNewlyUploaded(true)
                    .message("分片上传成功")
                    .build();
        } catch (Exception e) {
            log.error("分片上传失败", e);
            throw new BusinessException("上传失败: " + e.getMessage());
        }
    }
    
    @Override
    public Double getUploadProgress(String uploadId) {
        // 这里应该查询分片上传记录
        // 暂时返回示例进度
        return 50.0;
    }
    
    // 移动端专用方法实现
    
    @Override
    public FileUploadResponse uploadFile(MobileUploadRequest request, User uploader) {
        if (request == null || request.getFile() == null || request.getFile().isEmpty()) {
            throw new BusinessException("文件不能为空");
        }

        // 复用统一上传逻辑，确保移动端与网页端行为一致。
        FileUploadResponse response = uploadFile(request.getFile(), request.getFolderId(), uploader);

        // 移动端可选公开参数，上传成功后再更新公开状态。
        if (Boolean.TRUE.equals(request.getIsPublic()) && response.getId() != null) {
            setFilePublic(response.getId(), true, uploader);
        }

        return response;
    }
    
    @Override
    public List<FileSimpleResponse> getRecentFiles(User user, Integer limit) {
        int safeLimit = Math.max(limit == null ? 10 : limit, 1);
        List<FileEntity> files = fileRepository.findByUploader(
                user,
                PageRequest.of(0, safeLimit, Sort.by(Sort.Direction.DESC, "createdAt")))
            .getContent();

        Set<Long> favorites = userFavoriteFileIds.getOrDefault(user.getId(), Collections.emptySet());
        return files.stream()
            .filter(file -> file.getStatus() == FileEntity.FileStatus.AVAILABLE)
            .map(file -> convertToSimpleResponse(file, favorites.contains(file.getId())))
            .collect(Collectors.toList());
    }
    
    @Override
    public List<FileSimpleResponse> getFavoriteFiles(User user) {
        Set<Long> favoriteIds = userFavoriteFileIds.getOrDefault(user.getId(), Collections.emptySet());
        if (favoriteIds.isEmpty()) {
            return Collections.emptyList();
        }

        List<FileEntity> entities = fileRepository.findAllById(favoriteIds);
        return entities.stream()
                .filter(file -> file.getStatus() == FileEntity.FileStatus.AVAILABLE)
                .filter(file -> file.getUploader() != null && file.getUploader().getId().equals(user.getId()))
                .sorted((a, b) -> b.getUpdatedAt().compareTo(a.getUpdatedAt()))
                .map(file -> convertToSimpleResponse(file, true))
                .collect(Collectors.toList());
    }
    
    @Override
    public void favoriteFile(Long fileId, User user) {
        FileEntity file = requireOwnedAvailableFile(fileId, user);
        userFavoriteFileIds.compute(user.getId(), (uid, ids) -> {
            Set<Long> next = ids == null ? new HashSet<>() : new HashSet<>(ids);
            next.add(file.getId());
            return next;
        });
        log.info("收藏文件成功: userId={}, fileId={}", user.getId(), fileId);
    }
    
    @Override
    public void unfavoriteFile(Long fileId, User user) {
        userFavoriteFileIds.computeIfPresent(user.getId(), (uid, ids) -> {
            Set<Long> next = new HashSet<>(ids);
            next.remove(fileId);
            return next;
        });
        log.info("取消收藏文件成功: userId={}, fileId={}", user.getId(), fileId);
    }
    
    @Override
    public List<FileSimpleResponse> searchFiles(String keyword, User user, Integer limit) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return getRecentFiles(user, limit);
        }

        int safeLimit = Math.max(limit == null ? 20 : limit, 1);
        List<FileEntity> files = fileRepository.findByUploaderAndOriginalNameContaining(
                        user,
                        keyword.trim(),
                        PageRequest.of(0, safeLimit, Sort.by(Sort.Direction.DESC, "updatedAt")))
                .getContent();

        Set<Long> favorites = userFavoriteFileIds.getOrDefault(user.getId(), Collections.emptySet());
        return files.stream()
                .filter(file -> file.getStatus() == FileEntity.FileStatus.AVAILABLE)
                .map(file -> convertToSimpleResponse(file, favorites.contains(file.getId())))
                .collect(Collectors.toList());
    }
    
    @Override
    public List<FileSimpleResponse> getOfflineAvailableFiles(User user) {
        // 简化实现
        return java.util.Collections.emptyList();
    }
    
    /**
     * 获取文件扩展名
     */
    private String getFileExtension(String filename) {
        if (filename == null || filename.lastIndexOf('.') == -1) {
            return "";
        }
        return filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
    }

    private FileEntity requireOwnedAvailableFile(Long fileId, User currentUser) {
        FileEntity fileEntity = fileRepository.findById(fileId)
                .orElseThrow(() -> new BusinessException("文件不存在"));

        if (fileEntity.getUploader() == null || !fileEntity.getUploader().getId().equals(currentUser.getId())) {
            throw new BusinessException("无权操作该文件");
        }
        if (fileEntity.getStatus() != FileEntity.FileStatus.AVAILABLE) {
            throw new BusinessException("文件不可用");
        }
        return fileEntity;
    }

    private Folder resolveTargetFolder(Long targetFolderId, User currentUser) {
        if (targetFolderId == null) {
            return null;
        }
        Folder folder = folderRepository.findById(targetFolderId)
                .orElseThrow(() -> new BusinessException("目标文件夹不存在"));
        if (folder.getOwner() == null || !folder.getOwner().getId().equals(currentUser.getId())) {
            throw new BusinessException("无权操作目标文件夹");
        }
        return folder;
    }

    private String appendCopySuffix(String originalName) {
        if (originalName == null || originalName.trim().isEmpty()) {
            return "copy";
        }

        int dot = originalName.lastIndexOf('.');
        if (dot <= 0 || dot == originalName.length() - 1) {
            return originalName + "_copy";
        }

        String base = originalName.substring(0, dot);
        String ext = originalName.substring(dot);
        return base + "_copy" + ext;
    }

    private FileEntity createCopyFromExisting(FileEntity source,
                                              String copiedOriginalName,
                                              Folder targetFolder,
                                              User owner) {
        try {
            Path sourcePath = resolvePhysicalFilePath(source);
            String newStorageName = UUID.randomUUID().toString();
            Path uploadBase = Paths.get(fileStorageUtil.getUploadPath()).toAbsolutePath().normalize();
            Files.createDirectories(uploadBase);
            Path targetPath = uploadBase.resolve(newStorageName).normalize();
            Files.copy(sourcePath, targetPath, StandardCopyOption.REPLACE_EXISTING);

            FileEntity copied = new FileEntity();
            copied.setOriginalName(copiedOriginalName);
            copied.setStorageName(newStorageName);
            copied.setFilePath("/uploads/" + newStorageName);
            copied.setFileSize(source.getFileSize());
            copied.setContentType(source.getContentType());
            copied.setExtension(getFileExtension(copiedOriginalName));
            copied.setMd5Hash(source.getMd5Hash());
            copied.setStatus(FileEntity.FileStatus.AVAILABLE);
            copied.setUploader(owner);
            copied.setFolder(targetFolder);
            copied.setIsPublic(false);
            copied.setDownloadCount(0);
            copied.setPreviewCount(0);
            copied.setShareCount(0);

            return fileRepository.save(copied);
        } catch (IOException e) {
            throw new BusinessException("复制文件失败: " + e.getMessage(), e);
        }
    }

    private FileSimpleResponse convertToSimpleResponse(FileEntity file, boolean isFavorite) {
        String extension = file.getExtension();
        return FileSimpleResponse.builder()
                .id(file.getId())
                .fileName(file.getOriginalName())
                .fileType(extension)
                .fileSize(file.getFileSize())
                .fileIcon(extension == null ? "file" : extension.toLowerCase())
                .isFavorite(isFavorite)
                .isOfflineAvailable(Boolean.TRUE)
                .createdAt(file.getCreatedAt())
                .updatedAt(file.getUpdatedAt())
                .folderPath(file.getFolder() != null ? file.getFolder().getName() : "/")
                .build();
    }

    private FileEntity createFastUploadCopy(FileEntity existingFile,
                                            MultipartFile incomingFile,
                                            Folder targetFolder,
                                            User uploader,
                                            String md5Hash) {
        try {
            Path sourcePath = resolvePhysicalFilePath(existingFile);
            String newStorageName = UUID.randomUUID().toString();
            Path uploadBase = Paths.get(fileStorageUtil.getUploadPath()).toAbsolutePath().normalize();
            Files.createDirectories(uploadBase);
            Path targetPath = uploadBase.resolve(newStorageName).normalize();

            Files.copy(sourcePath, targetPath, StandardCopyOption.REPLACE_EXISTING);

            String originalFilename = incomingFile.getOriginalFilename();
            FileEntity copied = new FileEntity();
            copied.setOriginalName(originalFilename != null ? originalFilename : existingFile.getOriginalName());
            copied.setStorageName(newStorageName);
            copied.setFilePath("/uploads/" + newStorageName);
            copied.setFileSize(existingFile.getFileSize() != null ? existingFile.getFileSize() : incomingFile.getSize());
            copied.setContentType(incomingFile.getContentType() != null ? incomingFile.getContentType() : existingFile.getContentType());
            copied.setExtension(getFileExtension(copied.getOriginalName()));
            copied.setMd5Hash(md5Hash);
            copied.setStatus(FileEntity.FileStatus.AVAILABLE);
            copied.setUploader(uploader);
            copied.setFolder(targetFolder);
            copied.setIsPublic(false);
            copied.setDownloadCount(0);
            copied.setPreviewCount(0);
            copied.setShareCount(0);

            return fileRepository.save(copied);
        } catch (IOException e) {
            throw new BusinessException("秒传文件副本创建失败: " + e.getMessage(), e);
        }
    }

    private Path resolvePhysicalFilePath(FileEntity fileEntity) {
        Path uploadBase = Paths.get(fileStorageUtil.getUploadPath()).toAbsolutePath().normalize();
        Path byStorageName = uploadBase.resolve(fileEntity.getStorageName()).normalize();
        if (Files.exists(byStorageName)) {
            return byStorageName;
        }

        String fallbackPath = fileEntity.getFilePath();
        if (fallbackPath != null && !fallbackPath.isBlank()) {
            String normalized = fallbackPath.replace("\\", "/");
            if (normalized.startsWith("/")) {
                normalized = normalized.substring(1);
            }
            if (normalized.startsWith("uploads/")) {
                normalized = normalized.substring("uploads/".length());
            }

            Path byFilePath = uploadBase.resolve(normalized).normalize();
            if (Files.exists(byFilePath)) {
                return byFilePath;
            }
        }

        throw new BusinessException("秒传源文件不存在，无法创建副本");
    }
    
    /**
     * 转换为 FileResponse
     */
    private FileResponse convertToFileResponse(FileEntity fileEntity) {
        return FileResponse.builder()
                .id(fileEntity.getId())
                .originalName(fileEntity.getOriginalName())
                .storageName(fileEntity.getStorageName())
                .filePath(fileEntity.getFilePath())
                .fileSize(fileEntity.getFileSize())
                .contentType(fileEntity.getContentType())
                .extension(fileEntity.getExtension())
                .md5Hash(fileEntity.getMd5Hash())
                .status(fileEntity.getStatus().name())
                .isPublic(fileEntity.getIsPublic())
                .downloadCount(fileEntity.getDownloadCount())
                .previewCount(fileEntity.getPreviewCount())
                .shareCount(fileEntity.getShareCount())
                .lastDownloadAt(fileEntity.getLastDownloadAt())
                .lastPreviewAt(fileEntity.getLastPreviewAt())
                .uploaderName(fileEntity.getUploader().getUsername())
                .uploaderId(fileEntity.getUploader().getId())
                .folderId(fileEntity.getFolder() != null ? fileEntity.getFolder().getId() : null)
                .folderName(fileEntity.getFolder() != null ? fileEntity.getFolder().getName() : null)
                .createdAt(fileEntity.getCreatedAt())
                .updatedAt(fileEntity.getUpdatedAt())
                .deletedAt(fileEntity.getDeletedAt())
                .build();
    }
}