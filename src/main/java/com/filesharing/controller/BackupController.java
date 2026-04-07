package com.filesharing.controller;

import com.filesharing.backup.DataBackupService;
import com.filesharing.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CompletionException;

/**
 * 数据备份控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/backup")
@RequiredArgsConstructor
@Tag(name = "数据备份", description = "数据备份和恢复管理API")
public class BackupController {
    
    private final DataBackupService backupService;
    @Value("${backup.base-path:./backups}")
    private String backupBasePath;
    @Value("${backup.max-size:10737418240}")
    private long maxBackupSize;
    @Value("${backup.compression-level:6}")
    private int compressionLevel;
    private final Map<String, CompletableFuture<DataBackupService.BackupResult>> asyncRequestFutures = new ConcurrentHashMap<>();
    private final Map<String, AsyncRequestMeta> asyncRequestMeta = new ConcurrentHashMap<>();
    
    /**
     * 创建完整备份
     */
    @PostMapping("/full")
    @Operation(summary = "创建完整备份", description = "创建包含所有数据的完整备份")
    public ResponseEntity<ApiResponse<DataBackupService.BackupResult>> createFullBackup(
            @Parameter(description = "备份名称") @RequestParam String backupName,
            @Parameter(description = "是否包含文件") @RequestParam(defaultValue = "true") boolean includeFiles) {
        try {
            DataBackupService.BackupResult result = backupService.createFullBackup(backupName, includeFiles);
            return ResponseEntity.ok(ApiResponse.success("备份创建成功", result));
        } catch (Exception e) {
            log.error("创建完整备份失败: 备份名称={}", backupName, e);
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("备份创建失败: " + e.getMessage()));
        }
    }
    
    /**
     * 创建增量备份
     */
    @PostMapping("/incremental")
    @Operation(summary = "创建增量备份", description = "创建自指定时间以来的增量备份")
    public ResponseEntity<ApiResponse<DataBackupService.BackupResult>> createIncrementalBackup(
            @Parameter(description = "备份名称") @RequestParam String backupName,
            @Parameter(description = "起始时间") @RequestParam String sinceTimeString) {
        try {
            LocalDateTime sinceTime = LocalDateTime.parse(sinceTimeString);
            DataBackupService.BackupResult result = backupService.createIncrementalBackup(backupName, sinceTime);
            return ResponseEntity.ok(ApiResponse.success("增量备份创建成功", result));
        } catch (Exception e) {
            log.error("创建增量备份失败: 备份名称={}", backupName, e);
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("增量备份创建失败: " + e.getMessage()));
        }
    }
    
    /**
     * 异步创建备份
     */
    @PostMapping("/async")
    @Operation(summary = "异步创建备份", description = "异步执行备份任务")
    public ResponseEntity<ApiResponse<Map<String, String>>> createBackupAsync(
            @RequestBody Map<String, Object> request) {
        try {
            String backupName = request.get("backupName") == null ? null : String.valueOf(request.get("backupName")).trim();
            String backupType = request.get("backupType") == null ? "FULL" : String.valueOf(request.get("backupType")).trim().toUpperCase();
            boolean includeFiles = parseBoolean(request.get("includeFiles"), true);
            String sinceTimeString = (String) request.get("sinceTime");

            if (!StringUtils.hasText(backupName)) {
                return ResponseEntity.badRequest()
                    .body(ApiResponse.error("启动异步备份失败: backupName不能为空"));
            }

            if (!"FULL".equals(backupType) && !"INCREMENTAL".equals(backupType)) {
                return ResponseEntity.badRequest()
                    .body(ApiResponse.error("启动异步备份失败: 不支持的备份类型 " + backupType));
            }
            
            LocalDateTime sinceTime = sinceTimeString != null ? LocalDateTime.parse(sinceTimeString) : null;
            if ("INCREMENTAL".equals(backupType) && sinceTime == null) {
                return ResponseEntity.badRequest()
                    .body(ApiResponse.error("启动异步备份失败: 增量备份必须提供sinceTime"));
            }
            
            CompletableFuture<DataBackupService.BackupResult> future =
                backupService.createBackupAsync(backupName, backupType, includeFiles, sinceTime);

            String requestId = "async_" + System.currentTimeMillis();
            asyncRequestFutures.put(requestId, future);
            asyncRequestMeta.put(requestId, new AsyncRequestMeta(backupName, backupType, LocalDateTime.now()));

            future.whenComplete((result, throwable) -> {
                if (throwable != null) {
                    log.error("异步备份任务执行失败: backupName={}, backupType={}", backupName, backupType, throwable);
                    return;
                }
                if (result != null) {
                    log.info("异步备份任务完成: taskId={}, backupName={}, success={}", result.getTaskId(), backupName, result.getSuccess());
                }
            });
            
            Map<String, String> response = new HashMap<>();
            response.put("taskId", requestId);
            response.put("requestId", requestId);
            response.put("backupName", backupName);
            response.put("backupType", backupType);
            response.put("message", "备份任务已启动");
            response.put("status", "RUNNING");
            
            return ResponseEntity.ok(ApiResponse.success("异步备份任务启动成功", response));
            
        } catch (Exception e) {
            log.error("启动异步备份失败", e);
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("启动异步备份失败: " + e.getMessage()));
        }
    }

    /**
     * 删除备份（查询参数版本，支持完整路径）
     */
    @DeleteMapping("/delete")
    @Operation(summary = "删除备份（Query模式）", description = "通过backupPath参数删除指定备份，支持包含目录分隔符的路径")
    public ResponseEntity<ApiResponse<String>> deleteBackupByQuery(
            @Parameter(description = "备份路径") @RequestParam String backupPath) {
        try {
            backupService.deleteBackup(backupPath);
            return ResponseEntity.ok(ApiResponse.success("备份删除成功"));
        } catch (Exception e) {
            log.error("删除备份失败(Query): 备份路径={}", backupPath, e);
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("删除备份失败: " + e.getMessage()));
        }
    }
    
    /**
     * 恢复数据
     */
    @PostMapping("/restore")
    @Operation(summary = "恢复数据", description = "从指定备份恢复数据")
    public ResponseEntity<ApiResponse<DataBackupService.RestoreResult>> restoreFromBackup(
            @Parameter(description = "备份路径") @RequestParam String backupPath,
            @Parameter(description = "是否恢复文件") @RequestParam(defaultValue = "true") boolean restoreFiles) {
        try {
            DataBackupService.RestoreResult result = backupService.restoreFromBackup(backupPath, restoreFiles);
            return ResponseEntity.ok(ApiResponse.success("数据恢复成功", result));
        } catch (Exception e) {
            log.error("数据恢复失败: 备份路径={}", backupPath, e);
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("数据恢复失败: " + e.getMessage()));
        }
    }
    
    /**
     * 获取备份列表
     */
    @GetMapping("/list")
    @Operation(summary = "获取备份列表", description = "获取所有可用的备份信息")
    public ResponseEntity<ApiResponse<List<DataBackupService.BackupInfo>>> listBackups() {
        try {
            List<DataBackupService.BackupInfo> backups = backupService.listBackups();
            return ResponseEntity.ok(ApiResponse.success("获取备份列表成功", backups));
        } catch (Exception e) {
            log.error("获取备份列表失败", e);
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("获取备份列表失败: " + e.getMessage()));
        }
    }
    
    /**
     * 删除备份
     */
    @DeleteMapping("/{backupPath}")
    @Operation(summary = "删除备份", description = "删除指定的备份")
    public ResponseEntity<ApiResponse<String>> deleteBackup(
            @Parameter(description = "备份路径") @PathVariable String backupPath) {
        try {
            backupService.deleteBackup(backupPath);
            return ResponseEntity.ok(ApiResponse.success("备份删除成功"));
        } catch (Exception e) {
            log.error("删除备份失败: 备份路径={}", backupPath, e);
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("删除备份失败: " + e.getMessage()));
        }
    }
    
    /**
     * 获取备份任务状态
     */
    @GetMapping("/task/{taskId}")
    @Operation(summary = "获取备份任务状态", description = "获取指定备份任务的执行状态")
    public ResponseEntity<ApiResponse<DataBackupService.BackupTask>> getBackupTaskStatus(
            @Parameter(description = "任务ID") @PathVariable String taskId) {
        try {
            DataBackupService.BackupTask task = backupService.getBackupTask(taskId);
            if (task == null) {
                task = buildAsyncRequestTask(taskId);
                if (task == null) {
                    return ResponseEntity.badRequest()
                        .body(ApiResponse.error("任务不存在: " + taskId));
                }
            }
            return ResponseEntity.ok(ApiResponse.success("获取任务状态成功", task));
        } catch (Exception e) {
            log.error("获取备份任务状态失败: 任务ID={}", taskId, e);
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("获取任务状态失败: " + e.getMessage()));
        }
    }
    
    /**
     * 清理过期备份
     */
    @DeleteMapping("/cleanup")
    @Operation(summary = "清理过期备份", description = "删除指定天数之前的备份")
    public ResponseEntity<ApiResponse<DataBackupService.CleanupResult>> cleanupExpiredBackups(
            @Parameter(description = "保留天数") @RequestParam(defaultValue = "30") int daysToKeep) {
        try {
            DataBackupService.CleanupResult result = backupService.cleanupExpiredBackups(daysToKeep);
            return ResponseEntity.ok(ApiResponse.success("清理完成", result));
        } catch (Exception e) {
            log.error("清理过期备份失败", e);
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("清理失败: " + e.getMessage()));
        }
    }
    
    /**
     * 获取备份统计信息
     */
    @GetMapping("/statistics")
    @Operation(summary = "获取备份统计", description = "获取备份系统的统计信息")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getBackupStatistics() {
        try {
            Map<String, Object> stats = new HashMap<>();
            
            List<DataBackupService.BackupInfo> backups = backupService.listBackups();
            stats.put("totalBackups", backups.size());
            
            long totalFileSize = backups.stream()
                .mapToLong(backup -> backup.getTotalFileSize() != null ? backup.getTotalFileSize() : 0)
                .sum();
            stats.put("totalBackupSize", totalFileSize);
            
            long fullBackups = backups.stream()
                .filter(backup -> "FULL".equals(backup.getBackupType()))
                .count();
            stats.put("fullBackupCount", fullBackups);
            
            long incrementalBackups = backups.stream()
                .filter(backup -> "INCREMENTAL".equals(backup.getBackupType()))
                .count();
            stats.put("incrementalBackupCount", incrementalBackups);
            
            // 计算最近备份时间
            LocalDateTime latestBackup = backups.stream()
                .map(DataBackupService.BackupInfo::getCreateTime)
                .filter(Objects::nonNull)
                .max(LocalDateTime::compareTo)
                .orElse(null);
            stats.put("latestBackupTime", latestBackup);
            
            stats.put("backupBasePath", backupBasePath);
            stats.put("maxBackupSize", maxBackupSize);
            stats.put("compressionLevel", compressionLevel);
            
            return ResponseEntity.ok(ApiResponse.success("获取统计信息成功", stats));
        } catch (Exception e) {
            log.error("获取备份统计失败", e);
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("获取统计失败: " + e.getMessage()));
        }
    }
    
    /**
     * 验证备份完整性
     */
    @PostMapping("/validate")
    @Operation(summary = "验证备份完整性", description = "验证指定备份的完整性")
    public ResponseEntity<ApiResponse<Map<String, Object>>> validateBackup(
            @Parameter(description = "备份路径") @RequestParam String backupPath) {
        try {
            var matched = backupService.listBackups().stream()
                .filter(backup -> backupPath.equals(backup.getBackupPath()))
                .findFirst();

            boolean isValid = matched.map(DataBackupService.BackupInfo::getValid).orElse(false);
            String message = matched
                .map(backup -> Boolean.TRUE.equals(backup.getValid()) ? "备份完整性验证通过" : "备份完整性验证未通过")
                .orElse("未找到对应备份路径");

            Map<String, Object> validation = new HashMap<>();
            validation.put("backupPath", backupPath);
            validation.put("isValid", isValid);
            validation.put("validationTime", System.currentTimeMillis());
            validation.put("message", message);
            
            return ResponseEntity.ok(ApiResponse.success("验证完成", validation));
        } catch (Exception e) {
            log.error("验证备份完整性失败: 备份路径={}", backupPath, e);
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("验证失败: " + e.getMessage()));
        }
    }

    private boolean parseBoolean(Object value, boolean defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        return Boolean.parseBoolean(String.valueOf(value));
    }

    private DataBackupService.BackupTask buildAsyncRequestTask(String requestId) {
        CompletableFuture<DataBackupService.BackupResult> future = asyncRequestFutures.get(requestId);
        if (future == null) {
            return null;
        }

        AsyncRequestMeta meta = asyncRequestMeta.get(requestId);
        DataBackupService.BackupTask task = new DataBackupService.BackupTask();
        task.setTaskId(requestId);
        if (meta != null) {
            task.setBackupName(meta.backupName);
            task.setBackupType(meta.backupType);
            task.setStartTime(meta.startTime);
        }

        if (!future.isDone()) {
            task.setStatus("RUNNING");
            task.setSuccess(null);
            return task;
        }

        try {
            DataBackupService.BackupResult result = future.join();
            task.setStatus(Boolean.TRUE.equals(result.getSuccess()) ? "COMPLETED" : "FAILED");
            task.setSuccess(result.getSuccess());
            task.setErrorMessage(Boolean.TRUE.equals(result.getSuccess()) ? null : result.getMessage());
            task.setEndTime(LocalDateTime.now());
            return task;
        } catch (CompletionException ex) {
            task.setStatus("FAILED");
            task.setSuccess(false);
            task.setErrorMessage(ex.getCause() == null ? ex.getMessage() : ex.getCause().getMessage());
            task.setEndTime(LocalDateTime.now());
            return task;
        }
    }

    private static class AsyncRequestMeta {
        private final String backupName;
        private final String backupType;
        private final LocalDateTime startTime;

        private AsyncRequestMeta(String backupName, String backupType, LocalDateTime startTime) {
            this.backupName = backupName;
            this.backupType = backupType;
            this.startTime = startTime;
        }
    }
    
    /**
     * 导出备份配置
     */
    @GetMapping("/config/export")
    @Operation(summary = "导出备份配置", description = "导出当前的备份配置")
    public ResponseEntity<ApiResponse<Map<String, Object>>> exportBackupConfig() {
        try {
            Map<String, Object> config = new HashMap<>();
            config.put("backupBasePath", backupBasePath);
            config.put("maxBackupSize", maxBackupSize);
            config.put("compressionLevel", compressionLevel);
            config.put("retentionDays", 30);
            config.put("autoBackupEnabled", false);
            config.put("exportTime", System.currentTimeMillis());
            
            return ResponseEntity.ok(ApiResponse.success("导出配置成功", config));
        } catch (Exception e) {
            log.error("导出备份配置失败", e);
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("导出配置失败: " + e.getMessage()));
        }
    }
    
    /**
     * 导入备份配置
     */
    @PostMapping("/config/import")
    @Operation(summary = "导入备份配置", description = "导入备份配置")
    public ResponseEntity<ApiResponse<String>> importBackupConfig(
            @RequestBody Map<String, Object> config) {
        try {
            // 这里应该实现配置导入逻辑
            log.info("导入备份配置: {}", config);
            return ResponseEntity.ok(ApiResponse.success("配置导入成功"));
        } catch (Exception e) {
            log.error("导入备份配置失败", e);
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("配置导入失败: " + e.getMessage()));
        }
    }
}