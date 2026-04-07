package com.filesharing.controller;

import com.filesharing.dto.ApiResponse;
import com.filesharing.entity.User;
import com.filesharing.service.RecycleBinService;
import com.filesharing.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 回收站控制器
 */
@Slf4j
@RestController
@RequestMapping({"/api/recycle-bin", "/api/files/recycle-bin"})
@RequiredArgsConstructor
public class RecycleBinController {

    private final RecycleBinService recycleBinService;
    private final UserService userService;

    /**
     * 将文件移动到回收站
     */
    @PostMapping("/files/{fileId}")
    public ResponseEntity<ApiResponse<String>> moveFileToRecycleBin(
            @PathVariable Long fileId,
            @RequestBody(required = false) Map<String, String> body,
            HttpServletRequest request) {
        try {
            User currentUser = userService.getCurrentUser(request);
            String deleteReason = body == null ? null : body.get("deleteReason");
            recycleBinService.moveToRecycleBin(fileId, currentUser, deleteReason);
            return ResponseEntity.ok(ApiResponse.success("文件已移入回收站"));
        } catch (Exception e) {
            log.error("文件移入回收站失败: fileId={}, err={}", fileId, e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * 将文件夹移动到回收站
     */
    @PostMapping("/folders/{folderId}")
    public ResponseEntity<ApiResponse<String>> moveFolderToRecycleBin(
            @PathVariable Long folderId,
            @RequestBody(required = false) Map<String, String> body,
            HttpServletRequest request) {
        try {
            User currentUser = userService.getCurrentUser(request);
            String deleteReason = body == null ? null : body.get("deleteReason");
            recycleBinService.moveFolderToRecycleBin(folderId, currentUser, deleteReason);
            return ResponseEntity.ok(ApiResponse.success("文件夹已移入回收站"));
        } catch (Exception e) {
            log.error("文件夹移入回收站失败: folderId={}, err={}", folderId, e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * 获取回收站列表
     */
    @GetMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> getRecycleBinItems(
            @RequestParam(required = false) String itemType,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            HttpServletRequest request) {
        try {
            User currentUser = userService.getCurrentUser(request);
            Page<RecycleBinService.RecycleBinItem> items = recycleBinService.getUserRecycleBin(
                    currentUser,
                    itemType,
                    Math.max(page, 0),
                    Math.max(size, 1)
            );
            return ResponseEntity.ok(ApiResponse.success(buildPageResult(items)));
        } catch (Exception e) {
            log.error("获取回收站列表失败: {}", e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * 搜索回收站
     */
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<Map<String, Object>>> searchRecycleBin(
            @RequestParam(defaultValue = "") String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            HttpServletRequest request) {
        try {
            User currentUser = userService.getCurrentUser(request);
            Page<RecycleBinService.RecycleBinItem> items = recycleBinService.searchRecycleBin(
                    currentUser,
                    keyword,
                    Math.max(page, 0),
                    Math.max(size, 1)
            );
            return ResponseEntity.ok(ApiResponse.success(buildPageResult(items)));
        } catch (Exception e) {
            log.error("搜索回收站失败: {}", e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * 恢复回收站项目
     */
    @PostMapping("/{recycleBinId}/restore")
    public ResponseEntity<ApiResponse<RecycleBinService.RestoreResult>> restoreItem(
            @PathVariable Long recycleBinId,
            HttpServletRequest request) {
        try {
            User currentUser = userService.getCurrentUser(request);
            RecycleBinService.RestoreResult result = recycleBinService.restoreItem(recycleBinId, currentUser);
            return ResponseEntity.ok(ApiResponse.success(result));
        } catch (Exception e) {
            log.error("恢复回收站项目失败: id={}, err={}", recycleBinId, e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * 恢复到指定文件夹
     */
    @PostMapping("/{recycleBinId}/restore-to")
    public ResponseEntity<ApiResponse<RecycleBinService.RestoreResult>> restoreToLocation(
            @PathVariable Long recycleBinId,
            @RequestBody Map<String, Long> body,
            HttpServletRequest request) {
        try {
            User currentUser = userService.getCurrentUser(request);
            Long targetFolderId = body == null ? null : body.get("targetFolderId");
            if (targetFolderId == null) {
                return ResponseEntity.badRequest().body(ApiResponse.error("targetFolderId 不能为空"));
            }
            RecycleBinService.RestoreResult result = recycleBinService.restoreToLocation(recycleBinId, targetFolderId, currentUser);
            return ResponseEntity.ok(ApiResponse.success(result));
        } catch (Exception e) {
            log.error("恢复到指定位置失败: id={}, err={}", recycleBinId, e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * 永久删除回收站项目
     */
    @DeleteMapping("/{recycleBinId}")
    public ResponseEntity<ApiResponse<String>> permanentlyDelete(
            @PathVariable Long recycleBinId,
            HttpServletRequest request) {
        try {
            User currentUser = userService.getCurrentUser(request);
            recycleBinService.permanentlyDelete(recycleBinId, currentUser);
            return ResponseEntity.ok(ApiResponse.success("已永久删除"));
        } catch (Exception e) {
            log.error("永久删除失败: id={}, err={}", recycleBinId, e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * 清空回收站
     */
    @DeleteMapping("/empty")
    public ResponseEntity<ApiResponse<String>> emptyRecycleBin(HttpServletRequest request) {
        try {
            User currentUser = userService.getCurrentUser(request);
            recycleBinService.emptyRecycleBin(currentUser);
            return ResponseEntity.ok(ApiResponse.success("回收站已清空"));
        } catch (Exception e) {
            log.error("清空回收站失败: {}", e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * 回收站统计
     */
    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<RecycleBinService.RecycleBinStats>> getRecycleBinStats(HttpServletRequest request) {
        try {
            User currentUser = userService.getCurrentUser(request);
            RecycleBinService.RecycleBinStats stats = recycleBinService.getRecycleBinStats(currentUser);
            return ResponseEntity.ok(ApiResponse.success(stats));
        } catch (Exception e) {
            log.error("获取回收站统计失败: {}", e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * 获取即将过期项目
     */
    @GetMapping("/expiring")
    public ResponseEntity<ApiResponse<List<RecycleBinService.ExpiringItem>>> getExpiringItems(
            @RequestParam(defaultValue = "24") int hours,
            HttpServletRequest request) {
        try {
            User currentUser = userService.getCurrentUser(request);
            List<RecycleBinService.ExpiringItem> items = recycleBinService.getExpiringItemsReminder(currentUser, Math.max(hours, 1));
            return ResponseEntity.ok(ApiResponse.success(items));
        } catch (Exception e) {
            log.error("获取即将过期项目失败: {}", e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * 批量恢复
     */
    @PostMapping("/batch/restore")
    public ResponseEntity<ApiResponse<RecycleBinService.BatchOperationResult>> batchRestore(
            @RequestBody(required = false) Map<String, List<Long>> body,
            HttpServletRequest request) {
        try {
            User currentUser = userService.getCurrentUser(request);
            List<Long> recycleBinIds = extractIds(body);
            RecycleBinService.BatchOperationResult result = recycleBinService.batchRestore(recycleBinIds, currentUser);
            return ResponseEntity.ok(ApiResponse.success(result));
        } catch (Exception e) {
            log.error("批量恢复失败: {}", e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * 批量永久删除
     */
    @PostMapping("/batch/delete")
    public ResponseEntity<ApiResponse<RecycleBinService.BatchOperationResult>> batchDelete(
            @RequestBody(required = false) Map<String, List<Long>> body,
            HttpServletRequest request) {
        try {
            User currentUser = userService.getCurrentUser(request);
            List<Long> recycleBinIds = extractIds(body);
            RecycleBinService.BatchOperationResult result = recycleBinService.batchPermanentlyDelete(recycleBinIds, currentUser);
            return ResponseEntity.ok(ApiResponse.success(result));
        } catch (Exception e) {
            log.error("批量永久删除失败: {}", e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * 清理已过期项目
     */
    @PostMapping("/cleanup-expired")
    public ResponseEntity<ApiResponse<String>> cleanupExpiredItems() {
        try {
            recycleBinService.cleanupExpiredItems();
            return ResponseEntity.ok(ApiResponse.success("过期项目清理完成"));
        } catch (Exception e) {
            log.error("清理过期项目失败: {}", e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    private Map<String, Object> buildPageResult(Page<RecycleBinService.RecycleBinItem> pageData) {
        Map<String, Object> result = new HashMap<>();
        result.put("content", pageData.getContent());
        result.put("totalPages", pageData.getTotalPages());
        result.put("totalElements", pageData.getTotalElements());
        result.put("number", pageData.getNumber());
        result.put("size", pageData.getSize());
        result.put("first", pageData.isFirst());
        result.put("last", pageData.isLast());
        return result;
    }

    private List<Long> extractIds(Map<String, List<Long>> body) {
        if (body == null) {
            return List.of();
        }
        List<Long> ids = body.get("recycleBinIds");
        if (ids != null) {
            return ids;
        }
        List<Long> fallback = body.get("ids");
        return fallback == null ? List.of() : fallback;
    }
}
