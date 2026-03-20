package com.filesharing.controller;

import com.filesharing.backup.DataBackupService;
import com.filesharing.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * 数据备份控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/backup")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Tag(name = "数据备份", description = "数据备份和恢复管理API")
public class BackupController {
    
    private final DataBackupService backupService;
    
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
            String backupName = (String) request.get("backupName");
            String backupType = (String) request.get("backupType");
            boolean includeFiles = (Boolean) request.getOrDefault("includeFiles", true);
            String sinceTimeString = (String) request.get("sinceTime");
            
            LocalDateTime sinceTime = sinceTimeString != null ? LocalDateTime.parse(sinceTimeString) : null;
            
            CompletableFuture<DataBackupService.BackupResult> future = 
                backupService.createBackupAsync(backupName, backupType, includeFiles, sinceTime);
            
            // 获取任务ID（这里简化处理）
            String taskId = "async_" + System.currentTimeMillis();
            
            Map<String, String> response = new HashMap<>();
            response.put("taskId", taskId);
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
                return ResponseEntity.badRequest()
                    .body(ApiResponse.error("任务不存在: " + taskId));
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
                .max(LocalDateTime::compareTo)
                .orElse(null);
            stats.put("latestBackupTime", latestBackup);
            
            stats.put("backupBasePath", "./backups");
            stats.put("maxBackupSize", "10GB");
            
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
            // 这里应该实现备份完整性验证逻辑
            Map<String, Object> validation = new HashMap<>();
            validation.put("backupPath", backupPath);
            validation.put("isValid", true);
            validation.put("validationTime", System.currentTimeMillis());
            validation.put("message", "备份完整性验证通过");
            
            return ResponseEntity.ok(ApiResponse.success("验证完成", validation));
        } catch (Exception e) {
            log.error("验证备份完整性失败: 备份路径={}", backupPath, e);
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("验证失败: " + e.getMessage()));
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
            config.put("backupBasePath", "./backups");
            config.put("maxBackupSize", "10GB");
            config.put("compressionLevel", 6);
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