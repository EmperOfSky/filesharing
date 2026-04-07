package com.filesharing.controller;

import com.filesharing.dto.ApiResponse;
import com.filesharing.dto.FolderCreateRequest;
import com.filesharing.dto.FolderResponse;
import com.filesharing.dto.FileResponse;
import com.filesharing.entity.User;
import com.filesharing.service.FileService;
import com.filesharing.service.FolderService;
import com.filesharing.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 桌面端文件夹控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/folders")
@RequiredArgsConstructor
public class FolderController {

    private final FolderService folderService;
    private final FileService fileService;
    private final UserService userService;

    @PostMapping
    public ResponseEntity<ApiResponse<FolderResponse>> createFolder(
            @Valid @RequestBody FolderCreateRequest request,
            HttpServletRequest httpRequest) {
        try {
            User currentUser = userService.getCurrentUser(httpRequest);
            FolderResponse created = folderService.createFolder(request, currentUser);
            return ResponseEntity.ok(ApiResponse.success("文件夹创建成功", created));
        } catch (Exception e) {
            log.error("创建文件夹失败", e);
            return ResponseEntity.badRequest().body(ApiResponse.error("创建文件夹失败: " + e.getMessage()));
        }
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<FolderResponse>>> getFolders(HttpServletRequest httpRequest) {
        try {
            User currentUser = userService.getCurrentUser(httpRequest);
            List<FolderResponse> folders = folderService.getFolderTree(currentUser);
            return ResponseEntity.ok(ApiResponse.success(folders));
        } catch (Exception e) {
            log.error("获取文件夹列表失败", e);
            return ResponseEntity.badRequest().body(ApiResponse.error("获取文件夹列表失败: " + e.getMessage()));
        }
    }

    @GetMapping("/{folderId}")
    public ResponseEntity<ApiResponse<FolderResponse>> getFolderById(
            @PathVariable Long folderId,
            HttpServletRequest httpRequest) {
        try {
            User currentUser = userService.getCurrentUser(httpRequest);
            if (!folderService.hasFolderPermission(folderId, currentUser)) {
                return ResponseEntity.badRequest().body(ApiResponse.error("无权访问该文件夹"));
            }
            FolderResponse folder = folderService.getFolderById(folderId);
            return ResponseEntity.ok(ApiResponse.success(folder));
        } catch (Exception e) {
            log.error("获取文件夹详情失败", e);
            return ResponseEntity.badRequest().body(ApiResponse.error("获取文件夹详情失败: " + e.getMessage()));
        }
    }

    @GetMapping("/{folderId}/contents")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getFolderContents(
            @PathVariable Long folderId,
            HttpServletRequest httpRequest) {
        try {
            User currentUser = userService.getCurrentUser(httpRequest);
            if (!folderService.hasFolderPermission(folderId, currentUser)) {
                return ResponseEntity.badRequest().body(ApiResponse.error("无权访问该文件夹"));
            }

            FolderResponse folder = folderService.getFolderById(folderId);
            List<FolderResponse> allFolders = folderService.getFolderTree(currentUser);
            List<FolderResponse> subFolders = allFolders.stream()
                    .filter(f -> f.getParentId() != null && f.getParentId().equals(folderId))
                    .collect(Collectors.toList());

            List<FileResponse> files = fileService.getUserFiles(currentUser, "", 0, 1000).stream()
                    .filter(file -> file.getFolderId() != null && file.getFolderId().equals(folderId))
                    .collect(Collectors.toList());

            Map<String, Object> result = new HashMap<>();
            result.put("folder", folder);
            result.put("subFolders", subFolders);
            result.put("files", files);

            return ResponseEntity.ok(ApiResponse.success(result));
        } catch (Exception e) {
            log.error("获取文件夹内容失败", e);
            return ResponseEntity.badRequest().body(ApiResponse.error("获取文件夹内容失败: " + e.getMessage()));
        }
    }

    @PutMapping("/{folderId}/rename")
    public ResponseEntity<ApiResponse<FolderResponse>> renameFolder(
            @PathVariable Long folderId,
            @RequestBody Map<String, String> body,
            HttpServletRequest httpRequest) {
        try {
            User currentUser = userService.getCurrentUser(httpRequest);
            String newName = body.get("newName");
            FolderResponse renamed = folderService.renameFolder(folderId, newName, currentUser);
            return ResponseEntity.ok(ApiResponse.success("重命名成功", renamed));
        } catch (Exception e) {
            log.error("重命名文件夹失败", e);
            return ResponseEntity.badRequest().body(ApiResponse.error("重命名文件夹失败: " + e.getMessage()));
        }
    }

    @DeleteMapping("/{folderId}")
    public ResponseEntity<ApiResponse<String>> deleteFolder(
            @PathVariable Long folderId,
            HttpServletRequest httpRequest) {
        try {
            User currentUser = userService.getCurrentUser(httpRequest);
            folderService.deleteFolder(folderId, currentUser);
            return ResponseEntity.ok(ApiResponse.success("文件夹删除成功"));
        } catch (Exception e) {
            log.error("删除文件夹失败", e);
            return ResponseEntity.badRequest().body(ApiResponse.error("删除文件夹失败: " + e.getMessage()));
        }
    }
}
