package com.filesharing.controller;

import com.filesharing.dto.ApiResponse;
import com.filesharing.dto.FileResponse;
import com.filesharing.dto.FileUploadResponse;
import com.filesharing.dto.FolderCreateRequest;
import com.filesharing.dto.FolderResponse;
import com.filesharing.dto.response.FileSimpleResponse;
import com.filesharing.entity.FileEntity;
import com.filesharing.entity.User;
import com.filesharing.service.FileService;
import com.filesharing.service.FolderService;
import com.filesharing.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 文件控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class FileController {
    
    private final FileService fileService;
    private final FolderService folderService;
    private final UserService userService;
    
    /**
     * 上传文件
     */
    @PostMapping("/upload")
    public ResponseEntity<ApiResponse<FileUploadResponse>> uploadFile (
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "folderId", required = false) Long folderId,
            HttpServletRequest request) {
        User currentUser = userService.getCurrentUser(request);
        
        if (file == null || file.isEmpty()) {
            log.error("上传文件失败，文件为空: {}", file.getOriginalFilename());
            return ResponseEntity.badRequest().body(ApiResponse.error("FILE_ERROR"));
        }
        
        FileUploadResponse response = fileService.uploadFile(file, folderId, currentUser);
        return ResponseEntity.ok(ApiResponse.success("文件上传成功", response));
    }

    /**
     * 获取用户文件列表（分页结构，兼容前端）
     */
    @GetMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> getFiles(
            @RequestParam(defaultValue = "") String fileName,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Long folderId,
            HttpServletRequest request) {
        try {
            User currentUser = userService.getCurrentUser(request);
            List<FileResponse> allFiles = fileService.getUserFiles(currentUser, fileName, 0, 5000);

            List<FileResponse> filtered = allFiles;
            if (folderId != null) {
                filtered = allFiles.stream()
                        .filter(file -> folderId.equals(file.getFolderId()))
                        .collect(Collectors.toList());
            }

            int totalElements = filtered.size();
            int safeSize = Math.max(size, 1);
            int safePage = Math.max(page, 0);
            int from = Math.min(safePage * safeSize, totalElements);
            int to = Math.min(from + safeSize, totalElements);
            List<FileResponse> content = filtered.subList(from, to);
            int totalPages = totalElements == 0 ? 0 : (int) Math.ceil(totalElements * 1.0 / safeSize);

            Map<String, Object> result = new HashMap<>();
            result.put("content", new ArrayList<>(content));
            result.put("totalPages", totalPages);
            result.put("totalElements", totalElements);
            result.put("numberOfElements", content.size());
            result.put("size", safeSize);
            result.put("number", safePage);
            result.put("first", safePage == 0);
            result.put("last", totalPages == 0 || safePage >= totalPages - 1);

            return ResponseEntity.ok(ApiResponse.success(result));
        } catch (Exception e) {
            log.error("获取文件列表失败: {}", e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * 兼容旧调用：POST /api/files。
     * 当前系统只支持 /api/files/upload 进行真实文件上传。
     */
    @PostMapping
    public ResponseEntity<ApiResponse<String>> createFileCompat(@RequestBody(required = false) Map<String, Object> body) {
        return ResponseEntity.badRequest()
                .body(ApiResponse.error("INVALID_REQUEST", "请使用 /api/files/upload 上传文件"));
    }
    
    /**
     * 下载文件
     */
    @GetMapping("/{fileId}/download")
    public ResponseEntity<Resource> downloadFile(
            @PathVariable Long fileId,
            HttpServletRequest request) {
        try {
            User currentUser = userService.getCurrentUser(request);
            FileResponse fileInfo = fileService.getFileById(fileId);
            if (fileInfo.getUploaderId() == null || !fileInfo.getUploaderId().equals(currentUser.getId())) {
                return ResponseEntity.status(403).build();
            }

            // 获取文件实体
            var fileEntity = fileService.downloadFile(fileId);
            Resource resource = resolveFileResource(fileEntity);
            if (resource == null) {
                return ResponseEntity.notFound().build();
            }
            
            // 确定文件的content type
            String contentType = fileEntity.getContentType();
            if (contentType == null) {
                contentType = request.getServletContext().getMimeType(fileEntity.getFilePath());
            }
            if (contentType == null) {
                contentType = "application/octet-stream";
            }
            
            // 构建响应
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION, 
                           "attachment; filename=\"" + fileEntity.getOriginalName() + "\"")
                    .body(resource);
            
        } catch (Exception e) {
            log.error("文件下载失败: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }
    
    /**
     * 获取文件信息
     */
    @GetMapping("/{fileId}")
    public ResponseEntity<ApiResponse<FileResponse>> getFile(
            @PathVariable Long fileId) {
        try {
            FileResponse response = fileService.getFileById(fileId);
            return ResponseEntity.ok(ApiResponse.success(response));
        } catch (Exception e) {
            log.error("获取文件信息失败: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }
    
    /**
     * 获取用户文件列表
     */
    @GetMapping("/my")
    public ResponseEntity<ApiResponse<List<FileResponse>>> getUserFiles(
            @RequestParam(defaultValue = "") String fileName,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            HttpServletRequest request) {
        try {
            User currentUser = userService.getCurrentUser(request);
            List<FileResponse> files = fileService.getUserFiles(currentUser, fileName, page, size);
            return ResponseEntity.ok(ApiResponse.success(files));
        } catch (Exception e) {
            log.error("获取用户文件列表失败: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }
    
    /**
     * 删除文件
     */
    @DeleteMapping("/{fileId}")
    public ResponseEntity<ApiResponse<String>> deleteFile(
            @PathVariable Long fileId,
            HttpServletRequest request) {
        try {
            User currentUser = userService.getCurrentUser(request);
            fileService.deleteFile(fileId, currentUser);
            return ResponseEntity.ok(ApiResponse.success("文件删除成功"));
        } catch (Exception e) {
            log.error("文件删除失败: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * 批量删除文件
     */
    @PostMapping("/batch-delete")
    public ResponseEntity<ApiResponse<Map<String, Object>>> batchDeleteFiles(
            @RequestBody Map<String, List<Long>> body,
            HttpServletRequest request) {
        try {
            User currentUser = userService.getCurrentUser(request);
            List<Long> fileIds = body == null ? List.of() : body.getOrDefault("fileIds", List.of());

            int success = 0;
            List<Long> failedIds = new ArrayList<>();
            for (Long fileId : fileIds) {
                try {
                    fileService.deleteFile(fileId, currentUser);
                    success++;
                } catch (Exception ex) {
                    failedIds.add(fileId);
                }
            }

            Map<String, Object> result = new HashMap<>();
            result.put("total", fileIds.size());
            result.put("success", success);
            result.put("failed", failedIds.size());
            result.put("failedIds", failedIds);
            return ResponseEntity.ok(ApiResponse.success("批量删除完成", result));
        } catch (Exception e) {
            log.error("批量删除文件失败: {}", e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * 重命名文件
     */
    @PutMapping("/{fileId}/rename")
    public ResponseEntity<ApiResponse<FileResponse>> renameFile(
            @PathVariable Long fileId,
            @RequestBody Map<String, String> body,
            HttpServletRequest request) {
        try {
            User currentUser = userService.getCurrentUser(request);
            String newName = body == null ? null : body.get("newName");
            FileResponse response = fileService.renameFile(fileId, newName, currentUser);
            return ResponseEntity.ok(ApiResponse.success("重命名成功", response));
        } catch (Exception e) {
            log.error("重命名文件失败: {}", e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * 移动文件
     */
    @PutMapping("/{fileId}/move")
    public ResponseEntity<ApiResponse<FileResponse>> moveFile(
            @PathVariable Long fileId,
            @RequestBody Map<String, Long> body,
            HttpServletRequest request) {
        try {
            User currentUser = userService.getCurrentUser(request);
            Long targetFolderId = body == null ? null : body.get("targetFolderId");
            FileResponse response = fileService.moveFile(fileId, targetFolderId, currentUser);
            return ResponseEntity.ok(ApiResponse.success("移动成功", response));
        } catch (Exception e) {
            log.error("移动文件失败: {}", e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * 复制文件
     */
    @PostMapping("/{fileId}/copy")
    public ResponseEntity<ApiResponse<FileResponse>> copyFile(
            @PathVariable Long fileId,
            @RequestBody(required = false) Map<String, Long> body,
            HttpServletRequest request) {
        try {
            User currentUser = userService.getCurrentUser(request);
            Long targetFolderId = body == null ? null : body.get("targetFolderId");
            FileResponse response = fileService.copyFile(fileId, targetFolderId, currentUser);
            return ResponseEntity.ok(ApiResponse.success("复制成功", response));
        } catch (Exception e) {
            log.error("复制文件失败: {}", e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * 文件预览（兼容前端 iframe 路径）
     */
    @GetMapping("/{fileId}/preview")
    public ResponseEntity<Resource> previewFile(
            @PathVariable Long fileId,
            HttpServletRequest request) {
        try {
            User currentUser = userService.getCurrentUser(request);
            FileResponse fileInfo = fileService.getFileById(fileId);
            if (fileInfo.getUploaderId() == null || !fileInfo.getUploaderId().equals(currentUser.getId())) {
                return ResponseEntity.status(403).build();
            }

            FileEntity fileEntity = fileService.getFileEntityById(fileId);
            Resource resource = resolveFileResource(fileEntity);
            if (resource == null) {
                return ResponseEntity.notFound().build();
            }

            String contentType = fileEntity.getContentType();
            if (contentType == null || contentType.isBlank()) {
                contentType = request.getServletContext().getMimeType(fileEntity.getOriginalName());
            }
            if (contentType == null || contentType.isBlank()) {
                contentType = MediaType.APPLICATION_OCTET_STREAM_VALUE;
            }

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + fileEntity.getOriginalName() + "\"")
                    .body(resource);
        } catch (Exception e) {
            log.error("预览文件失败: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * 获取最近文件
     */
    @GetMapping("/recent")
    public ResponseEntity<ApiResponse<List<FileSimpleResponse>>> getRecentFiles(
            @RequestParam(defaultValue = "10") Integer limit,
            HttpServletRequest request) {
        try {
            User currentUser = userService.getCurrentUser(request);
            List<FileSimpleResponse> files = fileService.getRecentFiles(currentUser, limit);
            return ResponseEntity.ok(ApiResponse.success(files));
        } catch (Exception e) {
            log.error("获取最近文件失败: {}", e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * 获取收藏文件
     */
    @GetMapping("/favorites")
    public ResponseEntity<ApiResponse<List<FileSimpleResponse>>> getFavoriteFiles(HttpServletRequest request) {
        try {
            User currentUser = userService.getCurrentUser(request);
            List<FileSimpleResponse> files = fileService.getFavoriteFiles(currentUser);
            return ResponseEntity.ok(ApiResponse.success(files));
        } catch (Exception e) {
            log.error("获取收藏文件失败: {}", e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * 收藏文件
     */
    @PostMapping("/{fileId}/favorite")
    public ResponseEntity<ApiResponse<String>> favoriteFile(
            @PathVariable Long fileId,
            HttpServletRequest request) {
        try {
            User currentUser = userService.getCurrentUser(request);
            fileService.favoriteFile(fileId, currentUser);
            return ResponseEntity.ok(ApiResponse.success("收藏成功"));
        } catch (Exception e) {
            log.error("收藏文件失败: {}", e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * 取消收藏文件
     */
    @DeleteMapping("/{fileId}/favorite")
    public ResponseEntity<ApiResponse<String>> unfavoriteFile(
            @PathVariable Long fileId,
            HttpServletRequest request) {
        try {
            User currentUser = userService.getCurrentUser(request);
            fileService.unfavoriteFile(fileId, currentUser);
            return ResponseEntity.ok(ApiResponse.success("取消收藏成功"));
        } catch (Exception e) {
            log.error("取消收藏文件失败: {}", e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * 搜索文件（兼容前端 SearchResponse 结构）
     */
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<Map<String, Object>>> searchFiles(
            @RequestParam String keyword,
            @RequestParam(required = false) String fileType,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            HttpServletRequest request) {
        try {
            User currentUser = userService.getCurrentUser(request);
            List<FileResponse> files = fileService.getUserFiles(currentUser, keyword, page, size);

            if (fileType != null && !fileType.trim().isEmpty()) {
                List<String> allowed = Arrays.stream(fileType.split(","))
                        .map(String::trim)
                        .map(String::toLowerCase)
                        .collect(Collectors.toList());
                files = files.stream()
                        .filter(f -> f.getExtension() != null && allowed.contains(f.getExtension().toLowerCase()))
                        .collect(Collectors.toList());
            }

            Map<String, Object> result = new HashMap<>();
            result.put("files", files);
            result.put("folders", new ArrayList<>());
            result.put("totalResults", files.size());
            return ResponseEntity.ok(ApiResponse.success(result));
        } catch (Exception e) {
            log.error("搜索文件失败: {}", e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * 获取文件版本（兼容接口）
     */
    @GetMapping("/{fileId}/versions")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getFileVersions(
            @PathVariable Long fileId,
            HttpServletRequest request) {
        try {
            User currentUser = userService.getCurrentUser(request);
            FileResponse file = fileService.getFileById(fileId);
            if (file.getUploaderId() == null || !file.getUploaderId().equals(currentUser.getId())) {
                return ResponseEntity.status(403).body(ApiResponse.error("无权访问该文件"));
            }

            Map<String, Object> version = new HashMap<>();
            version.put("versionId", 1);
            version.put("fileId", file.getId());
            version.put("name", file.getOriginalName());
            version.put("updatedAt", file.getUpdatedAt());
            version.put("size", file.getFileSize());

            return ResponseEntity.ok(ApiResponse.success(List.of(version)));
        } catch (Exception e) {
            log.error("获取文件版本失败: {}", e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * 恢复文件版本（兼容接口）
     */
    @PostMapping("/{fileId}/versions/{versionId}/restore")
    public ResponseEntity<ApiResponse<FileResponse>> restoreFileVersion(
            @PathVariable Long fileId,
            @PathVariable Long versionId,
            HttpServletRequest request) {
        try {
            User currentUser = userService.getCurrentUser(request);
            FileResponse file = fileService.getFileById(fileId);
            if (file.getUploaderId() == null || !file.getUploaderId().equals(currentUser.getId())) {
                return ResponseEntity.status(403).body(ApiResponse.error("无权访问该文件"));
            }
            return ResponseEntity.ok(ApiResponse.success("版本恢复成功", file));
        } catch (Exception e) {
            log.error("恢复文件版本失败: fileId={}, versionId={}, err={}", fileId, versionId, e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * 文件统计信息
     */
    @GetMapping("/statistics/files")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getFileStatistics(HttpServletRequest request) {
        try {
            User currentUser = userService.getCurrentUser(request);
            List<FileResponse> allFiles = fileService.getUserFiles(currentUser, "", 0, 5000);

            long totalSize = allFiles.stream().mapToLong(f -> f.getFileSize() == null ? 0L : f.getFileSize()).sum();
            Map<String, Object> result = new HashMap<>();
            result.put("totalFiles", allFiles.size());
            result.put("totalSize", totalSize);
            result.put("totalDownloads", allFiles.stream().mapToLong(f -> f.getDownloadCount() == null ? 0 : f.getDownloadCount()).sum());
            result.put("totalPreviews", allFiles.stream().mapToLong(f -> f.getPreviewCount() == null ? 0 : f.getPreviewCount()).sum());
            return ResponseEntity.ok(ApiResponse.success(result));
        } catch (Exception e) {
            log.error("获取文件统计失败: {}", e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * 存储使用统计
     */
    @GetMapping("/statistics/storage")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getStorageUsage(HttpServletRequest request) {
        try {
            User currentUser = userService.getCurrentUser(request);
            long used = currentUser.getUsedStorage() == null ? 0L : currentUser.getUsedStorage();
            long quota = currentUser.getStorageQuota() == null ? 0L : currentUser.getStorageQuota();

            Map<String, Object> result = new HashMap<>();
            result.put("used", used);
            result.put("quota", quota);
            result.put("available", Math.max(0, quota - used));
            result.put("usagePercent", quota <= 0 ? 0 : Math.round((used * 100.0 / quota) * 100) / 100.0);
            return ResponseEntity.ok(ApiResponse.success(result));
        } catch (Exception e) {
            log.error("获取存储使用失败: {}", e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }
    
    /**
     * 获取公开文件列表
     */
    @GetMapping("/public")
    public ResponseEntity<ApiResponse<List<FileResponse>>> getPublicFiles(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        try {
            List<FileResponse> files = fileService.getPublicFiles(page, size);
            return ResponseEntity.ok(ApiResponse.success(files));
        } catch (Exception e) {
            log.error("获取公开文件列表失败: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * 兼容旧前端路径：创建文件夹
     */
    @PostMapping("/folders")
    public ResponseEntity<ApiResponse<FolderResponse>> createFolderInFilesNamespace(
            @RequestBody FolderCreateRequest request,
            HttpServletRequest httpRequest) {
        try {
            User currentUser = userService.getCurrentUser(httpRequest);
            FolderResponse created = folderService.createFolder(request, currentUser);
            return ResponseEntity.ok(ApiResponse.success("文件夹创建成功", created));
        } catch (Exception e) {
            log.error("创建文件夹失败: {}", e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error("创建文件夹失败: " + e.getMessage()));
        }
    }

    /**
     * 兼容旧前端路径：获取文件夹内容
     */
    @GetMapping("/folders/{folderId}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getFolderContentsInFilesNamespace(
            @PathVariable Long folderId,
            HttpServletRequest httpRequest) {
        try {
            User currentUser = userService.getCurrentUser(httpRequest);
            FolderResponse folder = folderService.getFolderById(folderId);
            List<FolderResponse> allFolders = folderService.getFolderTree(currentUser);
            List<FolderResponse> subFolders = allFolders.stream()
                    .filter(f -> f.getParentId() != null && f.getParentId().equals(folderId))
                    .collect(Collectors.toList());

            List<FileResponse> files = fileService.getUserFiles(currentUser, "", 0, 5000).stream()
                    .filter(file -> file.getFolderId() != null && file.getFolderId().equals(folderId))
                    .collect(Collectors.toList());

            Map<String, Object> result = new HashMap<>();
            result.put("folder", folder);
            result.put("subFolders", subFolders);
            result.put("files", files);
            return ResponseEntity.ok(ApiResponse.success(result));
        } catch (Exception e) {
            log.error("获取文件夹内容失败: {}", e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error("获取文件夹内容失败: " + e.getMessage()));
        }
    }

    /**
     * 兼容旧前端路径：删除文件夹
     */
    @DeleteMapping("/folders/{folderId}")
    public ResponseEntity<ApiResponse<String>> deleteFolderInFilesNamespace(
            @PathVariable Long folderId,
            HttpServletRequest httpRequest) {
        try {
            User currentUser = userService.getCurrentUser(httpRequest);
            folderService.deleteFolder(folderId, currentUser);
            return ResponseEntity.ok(ApiResponse.success("文件夹删除成功"));
        } catch (Exception e) {
            log.error("删除文件夹失败: {}", e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error("删除文件夹失败: " + e.getMessage()));
        }
    }

    /**
     * 兼容旧前端路径：重命名文件夹
     */
    @PutMapping("/folders/{folderId}/rename")
    public ResponseEntity<ApiResponse<FolderResponse>> renameFolderInFilesNamespace(
            @PathVariable Long folderId,
            @RequestBody Map<String, String> body,
            HttpServletRequest httpRequest) {
        try {
            User currentUser = userService.getCurrentUser(httpRequest);
            FolderResponse renamed = folderService.renameFolder(folderId, body.get("newName"), currentUser);
            return ResponseEntity.ok(ApiResponse.success("重命名成功", renamed));
        } catch (Exception e) {
            log.error("重命名文件夹失败: {}", e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error("重命名文件夹失败: " + e.getMessage()));
        }
    }

    private Resource resolveFileResource(FileEntity fileEntity) {
        try {
            Path uploadPath = Paths.get("./uploads").toAbsolutePath().normalize();
            Path byStorageName = uploadPath.resolve(fileEntity.getStorageName()).normalize();
            if (Files.exists(byStorageName)) {
                return new UrlResource(byStorageName.toUri());
            }

            String filePath = fileEntity.getFilePath();
            if (filePath != null && !filePath.isBlank()) {
                String normalized = filePath.replace("\\", "/");
                if (normalized.startsWith("/")) {
                    normalized = normalized.substring(1);
                }
                if (normalized.startsWith("uploads/")) {
                    normalized = normalized.substring("uploads/".length());
                }

                Path byFilePath = uploadPath.resolve(normalized).normalize();
                if (Files.exists(byFilePath)) {
                    return new UrlResource(byFilePath.toUri());
                }
            }
            return null;
        } catch (MalformedURLException e) {
            log.error("解析文件资源失败: {}", e.getMessage());
            return null;
        }
    }
}