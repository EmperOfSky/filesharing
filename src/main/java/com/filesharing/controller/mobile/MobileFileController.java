package com.filesharing.controller.mobile;

import com.filesharing.dto.ApiResponse;
import com.filesharing.dto.response.FileSimpleResponse;
import com.filesharing.entity.User;
import com.filesharing.service.FileService;
import com.filesharing.service.UserService;
// Swagger注解已移除
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * 移动端文件API控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/mobile/files")
@RequiredArgsConstructor

public class MobileFileController {
    
    private final FileService fileService;
    private final UserService userService;
    
    @GetMapping("/recent")
    // 获取最近文件列表
    public ResponseEntity<ApiResponse<List<FileSimpleResponse>>> getRecentFiles(
            HttpServletRequest request,
            @RequestParam(defaultValue = "10") Integer limit) {
        try {
            User currentUser = getCurrentUser(request);
            List<FileSimpleResponse> files = fileService.getRecentFiles(currentUser, limit);
            return ResponseEntity.ok(ApiResponse.success("获取成功", files));
        } catch (Exception e) {
            log.error("获取最近文件失败: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("获取失败: " + e.getMessage()));
        }
    }
    
    @GetMapping("/favorites")
    // 获取收藏文件列表
    public ResponseEntity<ApiResponse<List<FileSimpleResponse>>> getFavoriteFiles(
            HttpServletRequest request) {
        try {
            User currentUser = getCurrentUser(request);
            List<FileSimpleResponse> files = fileService.getFavoriteFiles(currentUser);
            return ResponseEntity.ok(ApiResponse.success("获取成功", files));
        } catch (Exception e) {
            log.error("获取收藏文件失败: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("获取失败: " + e.getMessage()));
        }
    }
    
    @PostMapping("/{fileId}/favorite")
    // 收藏文件
    public ResponseEntity<ApiResponse<String>> favoriteFile(
            HttpServletRequest request,
            @PathVariable Long fileId) {
        try {
            User currentUser = getCurrentUser(request);
            fileService.favoriteFile(fileId, currentUser);
            return ResponseEntity.ok(ApiResponse.success("收藏成功"));
        } catch (Exception e) {
            log.error("收藏文件失败: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("收藏失败: " + e.getMessage()));
        }
    }
    
    @DeleteMapping("/{fileId}/favorite")
    // 取消收藏文件
    public ResponseEntity<ApiResponse<String>> unfavoriteFile(
            HttpServletRequest request,
            @PathVariable Long fileId) {
        try {
            User currentUser = getCurrentUser(request);
            fileService.unfavoriteFile(fileId, currentUser);
            return ResponseEntity.ok(ApiResponse.success("取消收藏成功"));
        } catch (Exception e) {
            log.error("取消收藏文件失败: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("取消收藏失败: " + e.getMessage()));
        }
    }
    
    @GetMapping("/search")
    // 移动端文件搜索
    public ResponseEntity<ApiResponse<List<FileSimpleResponse>>> searchFiles(
            HttpServletRequest request,
            @RequestParam String keyword,
            @RequestParam(defaultValue = "10") Integer limit) {
        try {
            User currentUser = getCurrentUser(request);
            List<FileSimpleResponse> files = fileService.searchFiles(keyword, currentUser, limit);
            return ResponseEntity.ok(ApiResponse.success("搜索成功", files));
        } catch (Exception e) {
            log.error("文件搜索失败: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("搜索失败: " + e.getMessage()));
        }
    }
    
    @GetMapping("/offline-available")
    // 获取离线可用文件
    public ResponseEntity<ApiResponse<List<FileSimpleResponse>>> getOfflineAvailableFiles(
            HttpServletRequest request) {
        try {
            User currentUser = getCurrentUser(request);
            List<FileSimpleResponse> files = fileService.getOfflineAvailableFiles(currentUser);
            return ResponseEntity.ok(ApiResponse.success("获取成功", files));
        } catch (Exception e) {
            log.error("获取离线文件失败: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("获取失败: " + e.getMessage()));
        }
    }
    
    // 辅助方法：从请求中获取当前用户
    private User getCurrentUser(HttpServletRequest request) {
        return userService.getCurrentUser(request);
    }
}