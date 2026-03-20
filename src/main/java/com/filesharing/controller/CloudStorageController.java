package com.filesharing.controller;

import com.filesharing.dto.ApiResponse;
import com.filesharing.dto.request.CloudStorageAsyncProbeRequest;
import com.filesharing.dto.request.CloudStorageValidationRequest;
import com.filesharing.entity.CloudStorageConfig;
import com.filesharing.service.CloudStorageCapabilityService;
import com.filesharing.service.CloudStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;
import java.util.Map;

/**
 * 云存储管理控制器
 * 提供云存储配置管理和文件操作API
 */
@Slf4j
@RestController
@RequestMapping("/api/cloud-storage")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class CloudStorageController {
    
    private final CloudStorageService cloudStorageService;
    private final CloudStorageCapabilityService cloudStorageCapabilityService;

    /**
     * 返回云存储能力清单（校验/异步/对象存储支持）
     */
    @GetMapping("/capabilities")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getCapabilities() {
        try {
            return ResponseEntity.ok(ApiResponse.success(cloudStorageCapabilityService.getCapabilities()));
        } catch (Exception e) {
            log.error("获取能力清单失败", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("获取能力清单失败: " + e.getMessage()));
        }
    }

    /**
     * 强校验接口（类似 schema-first 校验）
     */
    @PostMapping("/validate-config")
    public ResponseEntity<ApiResponse<Map<String, Object>>> validateConfig(
            @Valid @RequestBody CloudStorageValidationRequest request) {
        try {
            return ResponseEntity.ok(ApiResponse.success("配置校验通过", cloudStorageCapabilityService.validateConfig(request)));
        } catch (Exception e) {
            log.error("配置校验失败", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("配置校验失败: " + e.getMessage()));
        }
    }

    /**
     * 异步探测接口（文件I/O + HTTP + 云存储连接）
     */
    @PostMapping("/async-probe")
    public ResponseEntity<ApiResponse<Map<String, Object>>> asyncProbe(
            @Valid @RequestBody CloudStorageAsyncProbeRequest request) {
        try {
            return ResponseEntity.ok(ApiResponse.success("异步探测完成", cloudStorageCapabilityService.runAsyncProbe(request)));
        } catch (Exception e) {
            log.error("异步探测失败", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("异步探测失败: " + e.getMessage()));
        }
    }
    
    /**
     * 创建云存储配置
     */
    @PostMapping("/configs")
    public ResponseEntity<ApiResponse<CloudStorageConfig>> createStorageConfig(
            @Valid @RequestBody CloudStorageConfig config) {
        try {
            CloudStorageConfig createdConfig = cloudStorageService.createStorageConfig(config);
            return ResponseEntity.ok(ApiResponse.success("配置创建成功", createdConfig));
        } catch (Exception e) {
            log.error("创建云存储配置失败", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("配置创建失败: " + e.getMessage()));
        }
    }
    
    /**
     * 获取云存储配置详情
     */
    @GetMapping("/configs/{configId}")
    public ResponseEntity<ApiResponse<CloudStorageConfig>> getStorageConfig(
            @PathVariable Long configId) {
        try {
            CloudStorageConfig config = cloudStorageService.getStorageConfigById(configId);
            return ResponseEntity.ok(ApiResponse.success(config));
        } catch (Exception e) {
            log.error("获取云存储配置失败: ID={}", configId, e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("获取配置失败: " + e.getMessage()));
        }
    }
    
    /**
     * 获取所有云存储配置
     */
    @GetMapping("/configs")
    public ResponseEntity<ApiResponse<List<CloudStorageService.StorageConfigInfo>>> getAllStorageConfigs() {
        try {
            List<CloudStorageService.StorageConfigInfo> configs = cloudStorageService.getAllStorageConfigs();
            return ResponseEntity.ok(ApiResponse.success(configs));
        } catch (Exception e) {
            log.error("获取云存储配置列表失败", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("获取配置列表失败：" + e.getMessage()));
        }
    }
    
    /**
     * 获取启用的云存储配置
     */
    @GetMapping("/configs/enabled")
    public ResponseEntity<ApiResponse<List<CloudStorageService.StorageConfigInfo>>> getEnabledStorageConfigs() {
        try {
            List<CloudStorageService.StorageConfigInfo> configs = cloudStorageService.getEnabledStorageConfigs();
            return ResponseEntity.ok(ApiResponse.success(configs));
        } catch (Exception e) {
            log.error("获取启用的云存储配置失败", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("获取启用配置失败：" + e.getMessage()));
        }
    }
    
    /**
     * 获取默认云存储配置
     */
    @GetMapping("/configs/default")
    public ResponseEntity<ApiResponse<CloudStorageConfig>> getDefaultStorageConfig() {
        try {
            CloudStorageConfig config = cloudStorageService.getDefaultStorageConfig();
            return ResponseEntity.ok(ApiResponse.success(config));
        } catch (Exception e) {
            log.error("获取默认云存储配置失败", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("获取默认配置失败: " + e.getMessage()));
        }
    }
    
    /**
     * 更新云存储配置
     */
    @PutMapping("/configs/{configId}")
    public ResponseEntity<ApiResponse<CloudStorageConfig>> updateStorageConfig(
            @PathVariable Long configId,
            @Valid @RequestBody CloudStorageConfig configUpdates) {
        try {
            CloudStorageConfig updatedConfig = cloudStorageService.updateStorageConfig(configId, configUpdates);
            return ResponseEntity.ok(ApiResponse.success("配置更新成功", updatedConfig));
        } catch (Exception e) {
            log.error("更新云存储配置失败: ID={}", configId, e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("配置更新失败: " + e.getMessage()));
        }
    }
    
    /**
     * 删除云存储配置
     */
    @DeleteMapping("/configs/{configId}")
    public ResponseEntity<ApiResponse<String>> deleteStorageConfig(
            @PathVariable Long configId) {
        try {
            cloudStorageService.deleteStorageConfig(configId);
            return ResponseEntity.ok(ApiResponse.success("配置删除成功"));
        } catch (Exception e) {
            log.error("删除云存储配置失败: ID={}", configId, e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("配置删除失败: " + e.getMessage()));
        }
    }
    
    /**
     * 测试云存储连接
     */
    @PostMapping("/configs/{configId}/test-connection")
    public ResponseEntity<ApiResponse<Map<String, Object>>> testConnection(
            @PathVariable Long configId) {
        try {
            var testResult = cloudStorageService.testConnection(configId);
            Map<String, Object> result = Map.of(
                "success", testResult.getSuccess(),
                "message", testResult.getMessage(),
                "endpoint", testResult.getEndpoint(),
                "responseTime", testResult.getResponseTime()
            );
            return ResponseEntity.ok(ApiResponse.success("连接测试完成", result));
        } catch (Exception e) {
            log.error("云存储连接测试失败: ID={}", configId, e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("连接测试失败: " + e.getMessage()));
        }
    }
    
    /**
     * 获取存储使用情况
     */
    @GetMapping("/configs/{configId}/usage")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getStorageUsage(
            @PathVariable Long configId) {
        try {
            var usage = cloudStorageService.getStorageUsage(configId);
            Map<String, Object> result = Map.of(
                "configId", usage.getConfigId(),
                "totalSpace", usage.getTotalLimit(),
                "usedSpace", usage.getUsedStorage(),
                "availableSpace", usage.getAvailableStorage(),
                "usagePercentage", usage.getUsagePercentage()
            );
            return ResponseEntity.ok(ApiResponse.success("获取存储使用情况成功", result));
        } catch (Exception e) {
            log.error("获取存储使用情况失败: ID={}", configId, e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("获取存储使用情况失败: " + e.getMessage()));
        }
    }
    
    /**
     * 获取所有存储配置信息概览
     */
    @GetMapping("/configs/info")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getStorageConfigInfos() {
        try {
            var infos = cloudStorageService.getAllStorageConfigs();
            List<Map<String, Object>> result = infos.stream()
                .map(info -> {
                    Map<String, Object> map = new java.util.HashMap<>();
                    map.put("id", info.getId());
                    map.put("configName", info.getConfigName());
                    map.put("providerType", info.getProviderType());
                    map.put("bucketName", info.getBucketName());
                    map.put("region", info.getRegion());
                    map.put("isEnabled", info.getIsEnabled());
                    map.put("isDefault", info.getIsDefault());
                    map.put("connectionStatus", info.getConnectionStatus());
                    map.put("description", info.getDescription());
                    return map;
                })
                .collect(java.util.stream.Collectors.toList());
            return ResponseEntity.ok(ApiResponse.success(result));
        } catch (Exception e) {
            log.error("获取存储配置信息失败", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("获取配置信息失败：" + e.getMessage()));
        }
    }
}