package com.filesharing.controller;

import com.filesharing.dto.ApiResponse;
import com.filesharing.dto.PreviewResponse;
import com.filesharing.entity.User;
import com.filesharing.service.PreviewService;
import com.filesharing.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * 文件预览控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/preview")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class PreviewController {
    
    private final PreviewService previewService;
    private final UserService userService;
    
    /**
     * 预览文件
     */
    @GetMapping("/{fileId}")
    public ResponseEntity<ApiResponse<PreviewResponse>> previewFile(
            @PathVariable Long fileId,
            @RequestParam(required = false, defaultValue = "false") boolean getContent,
            @RequestHeader(value = "User-Agent", required = false) String userAgent,
            @RequestHeader(value = "X-Device-Type", required = false) String deviceType,
            HttpServletRequest request) {
        try {
            User currentUser = getCurrentUser(request.getHeader("Authorization"));
            String clientIp = getClientIpAddress(request);
            
            PreviewResponse response = previewService.previewFile(
                    fileId, currentUser, deviceType, userAgent, clientIp);
            
            return ResponseEntity.ok(ApiResponse.success(response));
        } catch (Exception e) {
            log.error("文件预览失败: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }
    
    /**
     * 获取文件预览内容（用于显示实际内容）
     */
    @GetMapping("/{fileId}/content")
    public ResponseEntity<Resource> getPreviewContent(
            @PathVariable Long fileId,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) Integer width,
            @RequestParam(required = false) Integer height,
            HttpServletRequest request) {
        try {
            Resource resource = previewService.getPreviewContent(fileId, type);
            
            // 获取文件信息以确定content-type
            var fileEntity = previewService.getClass().getMethod("getFileEntityById", Long.class)
                    .invoke(previewService, fileId);
            
            String contentType = "application/octet-stream";
            // 这里需要通过反射或其他方式获取contentType
            
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline")
                    .body(resource);
        } catch (Exception e) {
            log.error("获取预览内容失败: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }
    
    /**
     * 获取文本文件预览
     */
    @GetMapping("/{fileId}/text")
    public ResponseEntity<ApiResponse<String>> getTextPreview(@PathVariable Long fileId) {
        try {
            // 这里需要注入FileService来获取文件实体
            // String content = previewService.getTextPreview(fileEntity);
            return ResponseEntity.ok(ApiResponse.success("文本内容"));
        } catch (Exception e) {
            log.error("获取文本预览失败: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }
    
    /**
     * 获取PDF预览
     */
    @GetMapping("/{fileId}/pdf")
    public ResponseEntity<byte[]> getPdfPreview(@PathVariable Long fileId) {
        try {
            // byte[] pdfContent = previewService.getPdfPreview(fileEntity);
            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_PDF)
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline")
                    .body(new byte[0]);
        } catch (Exception e) {
            log.error("获取PDF预览失败: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }
    
    /**
     * 获取图片预览（支持尺寸调整）
     */
    @GetMapping("/{fileId}/image")
    public ResponseEntity<Resource> getImagePreview(
            @PathVariable Long fileId,
            @RequestParam(required = false) Integer width,
            @RequestParam(required = false) Integer height) {
        try {
            // Resource imageResource = previewService.getImagePreview(fileEntity, width, height);
            return ResponseEntity.ok()
                    .contentType(MediaType.IMAGE_JPEG)
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline")
                    .body(null);
        } catch (Exception e) {
            log.error("获取图片预览失败: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }
    
    /**
     * 获取文件预览统计
     */
    @GetMapping("/{fileId}/statistics")
    public ResponseEntity<ApiResponse<PreviewService.FilePreviewStatistics>> getFilePreviewStatistics(
            @PathVariable Long fileId) {
        try {
            PreviewService.FilePreviewStatistics statistics = 
                    previewService.getFilePreviewStatistics(fileId);
            return ResponseEntity.ok(ApiResponse.success(statistics));
        } catch (Exception e) {
            log.error("获取文件预览统计失败: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }
    
    /**
     * 获取用户预览统计
     */
    @GetMapping("/user/statistics")
    public ResponseEntity<ApiResponse<PreviewService.UserPreviewStatistics>> getUserPreviewStatistics(
            @RequestHeader("Authorization") String authorization) {
        try {
            User currentUser = getCurrentUser(authorization);
            PreviewService.UserPreviewStatistics statistics = 
                    previewService.getUserPreviewStatistics(currentUser.getId());
            return ResponseEntity.ok(ApiResponse.success(statistics));
        } catch (Exception e) {
            log.error("获取用户预览统计失败: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }
    
    /**
     * 获取热门预览文件
     */
    @GetMapping("/popular")
    public ResponseEntity<ApiResponse<List<PreviewResponse>>> getPopularPreviews(
            @RequestParam(defaultValue = "10") int limit) {
        try {
            List<PreviewResponse> popularPreviews = previewService.getPopularPreviews(limit);
            return ResponseEntity.ok(ApiResponse.success(popularPreviews));
        } catch (Exception e) {
            log.error("获取热门预览失败: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }
    
    /**
     * 获取客户端IP地址
     */
    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        
        return request.getRemoteAddr();
    }
    
    /**
     * 获取当前用户（简化实现）
     */
    private User getCurrentUser(String authorization) {
        // 实际项目中应该解析JWT token获取用户信息
        return userService.findUserById(1L); // 示例用户ID
    }
}