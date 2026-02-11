package com.filesharing.controller;

import com.filesharing.dto.ApiResponse;
import com.filesharing.dto.FileResponse;
import com.filesharing.dto.FileUploadResponse;
import com.filesharing.entity.User;
import com.filesharing.service.FileService;
import com.filesharing.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.List;

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
    private final UserService userService;
    
    /**
     * 上传文件
     */
    @PostMapping("/upload")
    public ResponseEntity<ApiResponse<FileUploadResponse>> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "folderId", required = false) Long folderId,
            @RequestHeader("Authorization") String authorization) {
        try {
            // 解析用户信息（简化处理）
            User currentUser = getCurrentUser(authorization);
            FileUploadResponse response = fileService.uploadFile(file, folderId, currentUser);
            return ResponseEntity.ok(ApiResponse.success("文件上传成功", response));
        } catch (Exception e) {
            log.error("文件上传失败: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }
    
    /**
     * 下载文件
     */
    @GetMapping("/{fileId}/download")
    public ResponseEntity<Resource> downloadFile(
            @PathVariable Long fileId,
            HttpServletRequest request) {
        try {
            // 获取文件实体
            var fileEntity = fileService.downloadFile(fileId);
            
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
                    .body(null); // 实际项目中应该返回Resource对象
            
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
            @RequestHeader("Authorization") String authorization) {
        try {
            User currentUser = getCurrentUser(authorization);
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
            @RequestHeader("Authorization") String authorization) {
        try {
            User currentUser = getCurrentUser(authorization);
            fileService.deleteFile(fileId, currentUser);
            return ResponseEntity.ok(ApiResponse.success("文件删除成功"));
        } catch (Exception e) {
            log.error("文件删除失败: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
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
     * 模拟获取当前用户（实际项目中应从JWT解析）*/
    private User getCurrentUser(String authorization) {
        // 简化处理，实际应该解析JWT token
        return userService.findUserById(1L); // 示例用户ID
    }
}