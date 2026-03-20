package com.filesharing.controller.mobile;

import com.filesharing.dto.ApiResponse;
import com.filesharing.dto.response.FolderSimpleResponse;
import com.filesharing.entity.User;
import com.filesharing.service.FolderService;
import com.filesharing.service.UserService;
// Swagger注解已移除
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * 移动端文件夹API控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/mobile/folders")
@RequiredArgsConstructor

public class MobileFolderController {
    
    private final FolderService folderService;
    private final UserService userService;
    
    @GetMapping
    // 获取用户文件夹树
    public ResponseEntity<ApiResponse<List<FolderSimpleResponse>>> getUserFolders(
            HttpServletRequest request) {
        try {
            User currentUser = getCurrentUser(request);
            List<FolderSimpleResponse> folders = folderService.getUserFolderTree(currentUser);
            return ResponseEntity.ok(ApiResponse.success("获取成功", folders));
        } catch (Exception e) {
            log.error("获取文件夹树失败: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("获取失败: " + e.getMessage()));
        }
    }
    
    @GetMapping("/{folderId}/subfolders")
    // 获取子文件夹
    public ResponseEntity<ApiResponse<List<FolderSimpleResponse>>> getSubFolders(
            HttpServletRequest request,
            @PathVariable Long folderId) {
        try {
            User currentUser = getCurrentUser(request);
            List<FolderSimpleResponse> folders = folderService.getSubFolders(folderId, currentUser);
            return ResponseEntity.ok(ApiResponse.success("获取成功", folders));
        } catch (Exception e) {
            log.error("获取子文件夹失败: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("获取失败: " + e.getMessage()));
        }
    }
    
    @GetMapping("/{folderId}/breadcrumb")
    // 获取文件夹面包屑路径
    public ResponseEntity<ApiResponse<List<FolderSimpleResponse>>> getBreadcrumb(
            HttpServletRequest request,
            @PathVariable Long folderId) {
        try {
            User currentUser = getCurrentUser(request);
            List<FolderSimpleResponse> breadcrumb = folderService.getFolderBreadcrumb(folderId, currentUser);
            return ResponseEntity.ok(ApiResponse.success("获取成功", breadcrumb));
        } catch (Exception e) {
            log.error("获取面包屑失败: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("获取失败: " + e.getMessage()));
        }
    }
    
    @GetMapping("/quick-access")
    // 获取快捷访问文件夹
    public ResponseEntity<ApiResponse<List<FolderSimpleResponse>>> getQuickAccessFolders(
            HttpServletRequest request) {
        try {
            User currentUser = getCurrentUser(request);
            List<FolderSimpleResponse> folders = folderService.getQuickAccessFolders(currentUser);
            return ResponseEntity.ok(ApiResponse.success("获取成功", folders));
        } catch (Exception e) {
            log.error("获取快捷文件夹失败: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("获取失败: " + e.getMessage()));
        }
    }
    
    // 辅助方法：从请求中获取当前用户
    private User getCurrentUser(HttpServletRequest request) {
        return userService.getCurrentUser(request);
    }
}