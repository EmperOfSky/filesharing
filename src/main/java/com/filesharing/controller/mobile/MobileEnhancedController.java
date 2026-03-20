package com.filesharing.controller.mobile;

import com.filesharing.dto.ApiResponse;
import com.filesharing.dto.response.FileSimpleResponse;
import com.filesharing.entity.User;
import com.filesharing.service.UserService;
import com.filesharing.service.impl.MobileFileServiceImpl;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 移动端增强功能控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/mobile/enhanced")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Tag(name = "移动端增强功能", description = "移动端专用的增强功能API")
public class MobileEnhancedController {
    
    private final MobileFileServiceImpl mobileFileService;
    private final UserService userService;
    
    /**
     * 获取文件缩略图信息
     */
    @GetMapping("/files/{fileId}/thumbnail-info")
    @Operation(summary = "获取文件缩略图信息", description = "获取移动端显示所需的文件缩略图信息")
    public ResponseEntity<ApiResponse<MobileFileServiceImpl.FileThumbnailInfo>> getFileThumbnailInfo(
            HttpServletRequest request,
            @Parameter(description = "文件ID") @PathVariable Long fileId) {
        try {
            User currentUser = getCurrentUser(request);
            MobileFileServiceImpl.FileThumbnailInfo thumbnailInfo = 
                mobileFileService.getFileThumbnail(fileId, currentUser);
            return ResponseEntity.ok(ApiResponse.success("获取成功", thumbnailInfo));
        } catch (Exception e) {
            log.error("获取文件缩略图信息失败: 文件ID={}", fileId, e);
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("获取失败: " + e.getMessage()));
        }
    }
    
    /**
     * 获取文件预览信息
     */
    @GetMapping("/files/{fileId}/preview-info")
    @Operation(summary = "获取文件预览信息", description = "获取文件预览所需的信息")
    public ResponseEntity<ApiResponse<MobileFileServiceImpl.FilePreviewInfo>> getFilePreviewInfo(
            HttpServletRequest request,
            @Parameter(description = "文件ID") @PathVariable Long fileId) {
        try {
            User currentUser = getCurrentUser(request);
            MobileFileServiceImpl.FilePreviewInfo previewInfo = 
                mobileFileService.getFilePreview(fileId, currentUser);
            return ResponseEntity.ok(ApiResponse.success("获取成功", previewInfo));
        } catch (Exception e) {
            log.error("获取文件预览信息失败: 文件ID={}", fileId, e);
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("获取失败: " + e.getMessage()));
        }
    }
    
    /**
     * 批量文件操作
     */
    @PostMapping("/files/batch-operate")
    @Operation(summary = "批量文件操作", description = "支持批量删除、收藏等操作")
    public ResponseEntity<ApiResponse<MobileFileServiceImpl.BatchMobileOperationResult>> batchOperateFiles(
            HttpServletRequest request,
            @RequestBody MobileFileServiceImpl.BatchMobileOperationRequest batchRequest) {
        try {
            User currentUser = getCurrentUser(request);
            MobileFileServiceImpl.BatchMobileOperationResult result = 
                mobileFileService.batchOperateFiles(batchRequest, currentUser);
            return ResponseEntity.ok(ApiResponse.success("操作完成", result));
        } catch (Exception e) {
            log.error("批量文件操作失败", e);
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("操作失败: " + e.getMessage()));
        }
    }
    
    /**
     * 文件状态同步
     */
    @PostMapping("/sync")
    @Operation(summary = "文件状态同步", description = "同步客户端和服务器端的文件状态")
    public ResponseEntity<ApiResponse<MobileFileServiceImpl.SyncResult>> syncFileStatus(
            HttpServletRequest request,
            @RequestBody MobileFileServiceImpl.SyncRequest syncRequest) {
        try {
            User currentUser = getCurrentUser(request);
            MobileFileServiceImpl.SyncResult result = 
                mobileFileService.syncFileStatus(syncRequest, currentUser);
            return ResponseEntity.ok(ApiResponse.success("同步完成", result));
        } catch (Exception e) {
            log.error("文件状态同步失败", e);
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("同步失败: " + e.getMessage()));
        }
    }
    
    /**
     * 获取用户存储使用情况
     */
    @GetMapping("/storage/usage")
    @Operation(summary = "获取存储使用情况", description = "获取用户存储空间使用详情")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getStorageUsage(
            HttpServletRequest request) {
        try {
            User currentUser = getCurrentUser(request);
            
            Map<String, Object> usageInfo = new HashMap<>();
            usageInfo.put("userId", currentUser.getId());
            usageInfo.put("username", currentUser.getUsername());
            usageInfo.put("storageQuota", currentUser.getStorageQuota());
            usageInfo.put("usedStorage", currentUser.getUsedStorage());
            usageInfo.put("availableStorage", currentUser.getStorageQuota() - currentUser.getUsedStorage());
            
            double usagePercentage = currentUser.getStorageQuota() > 0 ? 
                (double) currentUser.getUsedStorage() / currentUser.getStorageQuota() * 100 : 0;
            usageInfo.put("usagePercentage", String.format("%.2f", usagePercentage));
            
            // 存储预警
            String warningLevel = "NORMAL";
            if (usagePercentage >= 90) {
                warningLevel = "CRITICAL";
            } else if (usagePercentage >= 80) {
                warningLevel = "WARNING";
            }
            usageInfo.put("warningLevel", warningLevel);
            
            return ResponseEntity.ok(ApiResponse.success("获取成功", usageInfo));
        } catch (Exception e) {
            log.error("获取存储使用情况失败", e);
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("获取失败: " + e.getMessage()));
        }
    }
    
    /**
     * 获取用户最近活动
     */
    @GetMapping("/activity/recent")
    @Operation(summary = "获取最近活动", description = "获取用户的最近文件操作活动")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getRecentActivity(
            HttpServletRequest request,
            @Parameter(description = "活动数量") @RequestParam(defaultValue = "10") Integer limit) {
        try {
            User currentUser = getCurrentUser(request);
            
            // 这里应该查询用户的操作日志
            // 暂时返回示例数据
            List<Map<String, Object>> activities = java.util.Arrays.asList(
                createActivity("上传文件", "document.pdf", System.currentTimeMillis() - 300000),
                createActivity("下载文件", "image.jpg", System.currentTimeMillis() - 600000),
                createActivity("删除文件", "old_file.txt", System.currentTimeMillis() - 900000)
            );
            
            return ResponseEntity.ok(ApiResponse.success("获取成功", activities));
        } catch (Exception e) {
            log.error("获取最近活动失败", e);
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("获取失败: " + e.getMessage()));
        }
    }
    
    /**
     * 获取移动设备配置
     */
    @GetMapping("/config")
    @Operation(summary = "获取移动端配置", description = "获取移动端应用的相关配置信息")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getMobileConfig() {
        try {
            Map<String, Object> config = new HashMap<>();
            
            // 文件上传配置
            Map<String, Object> uploadConfig = new HashMap<>();
            uploadConfig.put("maxFileSize", 100 * 1024 * 1024); // 100MB
            uploadConfig.put("allowedExtensions", java.util.Arrays.asList(
                "jpg", "jpeg", "png", "gif", "pdf", "doc", "docx", "txt", "zip", "rar"));
            uploadConfig.put("supportChunkUpload", true);
            uploadConfig.put("chunkSize", 5 * 1024 * 1024); // 5MB
            config.put("upload", uploadConfig);
            
            // 网络配置
            Map<String, Object> networkConfig = new HashMap<>();
            networkConfig.put("timeout", 30000); // 30秒
            networkConfig.put("retryCount", 3);
            networkConfig.put("retryDelay", 1000); // 1秒
            config.put("network", networkConfig);
            
            // 缓存配置
            Map<String, Object> cacheConfig = new HashMap<>();
            cacheConfig.put("maxCacheSize", 500 * 1024 * 1024); // 500MB
            cacheConfig.put("cacheThumbnails", true);
            cacheConfig.put("cachePreviews", true);
            config.put("cache", cacheConfig);
            
            // 功能开关
            Map<String, Object> featureFlags = new HashMap<>();
            featureFlags.put("enableOfflineMode", true);
            featureFlags.put("enableFileSync", true);
            featureFlags.put("enablePushNotifications", true);
            featureFlags.put("enableBiometricAuth", true);
            config.put("features", featureFlags);
            
            return ResponseEntity.ok(ApiResponse.success("获取成功", config));
        } catch (Exception e) {
            log.error("获取移动端配置失败", e);
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("获取失败: " + e.getMessage()));
        }
    }
    
    /**
     * 检查应用更新
     */
    @GetMapping("/version/check")
    @Operation(summary = "检查应用更新", description = "检查移动端应用是否有新版本")
    public ResponseEntity<ApiResponse<Map<String, Object>>> checkAppUpdate(
            @Parameter(description = "当前应用版本") @RequestParam String currentVersion,
            @Parameter(description = "设备类型") @RequestParam String deviceType) {
        try {
            Map<String, Object> updateInfo = new HashMap<>();
            
            // 这里应该查询版本管理系统
            // 暂时返回示例数据
            String latestVersion = "2.1.0";
            boolean hasUpdate = !currentVersion.equals(latestVersion);
            
            updateInfo.put("hasUpdate", hasUpdate);
            updateInfo.put("latestVersion", latestVersion);
            updateInfo.put("currentVersion", currentVersion);
            updateInfo.put("updateRequired", false);
            updateInfo.put("updateUrl", "https://example.com/download/app-" + latestVersion + ".apk");
            updateInfo.put("releaseNotes", "1. 修复了一些bug\n2. 优化了性能\n3. 新增了离线同步功能");
            updateInfo.put("size", "25.5MB");
            
            return ResponseEntity.ok(ApiResponse.success("检查完成", updateInfo));
        } catch (Exception e) {
            log.error("检查应用更新失败", e);
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("检查失败: " + e.getMessage()));
        }
    }
    
    /**
     * 上报用户反馈
     */
    @PostMapping("/feedback")
    @Operation(summary = "提交用户反馈", description = "提交用户的意见和建议")
    public ResponseEntity<ApiResponse<String>> submitFeedback(
            HttpServletRequest request,
            @RequestBody Map<String, Object> feedback) {
        try {
            User currentUser = getCurrentUser(request);
            
            // 这里应该保存反馈到数据库
            log.info("收到用户反馈: 用户ID={}, 反馈内容={}", 
                currentUser.getId(), feedback.get("content"));
            
            return ResponseEntity.ok(ApiResponse.success("反馈提交成功"));
        } catch (Exception e) {
            log.error("提交反馈失败", e);
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("提交失败: " + e.getMessage()));
        }
    }
    
    /**
     * 获取系统公告
     */
    @GetMapping("/announcements")
    @Operation(summary = "获取系统公告", description = "获取最新的系统公告和通知")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getAnnouncements(
            @Parameter(description = "公告数量") @RequestParam(defaultValue = "5") Integer limit) {
        try {
            // 这里应该查询公告系统
            // 暂时返回示例数据
            List<Map<String, Object>> announcements = java.util.Arrays.asList(
                createAnnouncement("系统维护通知", "系统将于今晚02:00-04:00进行例行维护", 
                    System.currentTimeMillis() - 86400000, "INFO"),
                createAnnouncement("新功能上线", "新增了文件预览和离线同步功能", 
                    System.currentTimeMillis() - 172800000, "SUCCESS")
            );
            
            return ResponseEntity.ok(ApiResponse.success("获取成功", announcements));
        } catch (Exception e) {
            log.error("获取公告失败", e);
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("获取失败: " + e.getMessage()));
        }
    }
    
    // ==================== 辅助方法 ====================
    
    private User getCurrentUser(HttpServletRequest request) {
        return userService.getCurrentUser(request);
    }
    
    private Map<String, Object> createActivity(String action, String fileName, long timestamp) {
        Map<String, Object> activity = new HashMap<>();
        activity.put("action", action);
        activity.put("fileName", fileName);
        activity.put("timestamp", timestamp);
        activity.put("formattedTime", new java.util.Date(timestamp).toString());
        return activity;
    }
    
    private Map<String, Object> createAnnouncement(String title, String content, long publishTime, String type) {
        Map<String, Object> announcement = new HashMap<>();
        announcement.put("title", title);
        announcement.put("content", content);
        announcement.put("publishTime", publishTime);
        announcement.put("type", type);
        announcement.put("isRead", false);
        return announcement;
    }
}