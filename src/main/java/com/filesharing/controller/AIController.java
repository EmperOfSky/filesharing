package com.filesharing.controller;

import com.filesharing.dto.ApiResponse;
import com.filesharing.entity.User;
import com.filesharing.service.AIService;
import com.filesharing.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;

/**
 * AI功能控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class AIController {
    
    private final AIService aiService;
    private final UserService userService;
    
    /**
     * 文本内容智能分类
     */
    @PostMapping("/classify-text")
    public ResponseEntity<ApiResponse<Map<String, Object>>> classifyText(
            @RequestBody Map<String, String> request,
            HttpServletRequest httpRequest) {
        try {
            String content = request.get("content");
            if (content == null || content.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(ApiResponse.error("内容不能为空"));
            }
            
            User currentUser = getCurrentUser(httpRequest);
            Map<String, Object> result = aiService.classifyTextContent(content, currentUser);
            
            if ((Boolean) result.getOrDefault("success", false)) {
                return ResponseEntity.ok(ApiResponse.success("分类成功", result));
            } else {
                return ResponseEntity.badRequest()
                    .body(ApiResponse.error((String) result.get("error")));
            }
        } catch (Exception e) {
            log.error("文本分类失败: {}", e.getMessage());
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("分类失败: " + e.getMessage()));
        }
    }
    
    /**
     * 文件内容智能分析
     */
    @PostMapping("/analyze-file")
    public ResponseEntity<ApiResponse<Map<String, Object>>> analyzeFile(
            @RequestParam("file") MultipartFile file,
            HttpServletRequest httpRequest) {
        try {
            if (file.isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(ApiResponse.error("文件不能为空"));
            }
            
            User currentUser = getCurrentUser(httpRequest);
            Map<String, Object> result = aiService.analyzeFileContent(file, currentUser);
            
            if ((Boolean) result.getOrDefault("success", false)) {
                return ResponseEntity.ok(ApiResponse.success("分析成功", result));
            } else {
                return ResponseEntity.badRequest()
                    .body(ApiResponse.error((String) result.get("error")));
            }
        } catch (Exception e) {
            log.error("文件分析失败: {}", e.getMessage());
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("分析失败: " + e.getMessage()));
        }
    }
    
    /**
     * 图像内容识别
     */
    @PostMapping("/recognize-image")
    public ResponseEntity<ApiResponse<Map<String, Object>>> recognizeImage(
            @RequestParam("image") MultipartFile image,
            HttpServletRequest httpRequest) {
        try {
            if (image.isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(ApiResponse.error("图片不能为空"));
            }
            
            User currentUser = getCurrentUser(httpRequest);
            Map<String, Object> result = aiService.recognizeImage(image, currentUser);
            
            if ((Boolean) result.getOrDefault("success", false)) {
                return ResponseEntity.ok(ApiResponse.success("识别成功", result));
            } else {
                return ResponseEntity.badRequest()
                    .body(ApiResponse.error((String) result.get("error")));
            }
        } catch (Exception e) {
            log.error("图像识别失败: {}", e.getMessage());
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("识别失败: " + e.getMessage()));
        }
    }
    
    /**
     * 智能标签推荐
     */
    @PostMapping("/recommend-tags")
    public ResponseEntity<ApiResponse<List<String>>> recommendTags(
            @RequestBody Map<String, String> request,
            HttpServletRequest httpRequest) {
        try {
            String content = request.get("content");
            String fileType = request.get("fileType");
            
            if (content == null || content.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(ApiResponse.error("内容不能为空"));
            }
            
            User currentUser = getCurrentUser(httpRequest);
            List<String> tags = aiService.recommendTags(content, fileType, currentUser);
            
            return ResponseEntity.ok(ApiResponse.success("推荐成功", tags));
        } catch (Exception e) {
            log.error("标签推荐失败: {}", e.getMessage());
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("推荐失败: " + e.getMessage()));
        }
    }
    
    /**
     * 智能搜索
     */
    @GetMapping("/smart-search")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> smartSearch(
            @RequestParam String query,
            HttpServletRequest httpRequest) {
        try {
            if (query == null || query.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(ApiResponse.error("查询内容不能为空"));
            }
            
            User currentUser = getCurrentUser(httpRequest);
            List<Map<String, Object>> results = aiService.smartSearch(query, currentUser);
            
            return ResponseEntity.ok(ApiResponse.success("搜索成功", results));
        } catch (Exception e) {
            log.error("智能搜索失败: {}", e.getMessage());
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("搜索失败: " + e.getMessage()));
        }
    }
    
    /**
     * 用户行为分析
     */
    @GetMapping("/analyze-behavior")
    public ResponseEntity<ApiResponse<Map<String, Object>>> analyzeUserBehavior(
            HttpServletRequest httpRequest) {
        try {
            User currentUser = getCurrentUser(httpRequest);
            Map<String, Object> analysis = aiService.analyzeUserBehavior(currentUser);
            
            return ResponseEntity.ok(ApiResponse.success("分析成功", analysis));
        } catch (Exception e) {
            log.error("用户行为分析失败: {}", e.getMessage());
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("分析失败: " + e.getMessage()));
        }
    }
    
    /**
     * 内容相似度分析
     */
    @PostMapping("/similarity")
    public ResponseEntity<ApiResponse<Map<String, Object>>> calculateSimilarity(
            @RequestBody Map<String, String> request) {
        try {
            String content1 = request.get("content1");
            String content2 = request.get("content2");
            
            if (content1 == null || content2 == null) {
                return ResponseEntity.badRequest()
                    .body(ApiResponse.error("两个内容都不能为空"));
            }
            
            Double similarity = aiService.calculateSimilarity(content1, content2);
            Map<String, Object> result = Map.of(
                "similarity", similarity,
                "percentage", String.format("%.2f%%", similarity * 100)
            );
            
            return ResponseEntity.ok(ApiResponse.success("计算成功", result));
        } catch (Exception e) {
            log.error("相似度计算失败: {}", e.getMessage());
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("计算失败: " + e.getMessage()));
        }
    }
    
    /**
     * 自动摘要生成
     */
    @PostMapping("/summarize")
    public ResponseEntity<ApiResponse<String>> generateSummary(
            @RequestBody Map<String, Object> request) {
        try {
            String content = (String) request.get("content");
            Integer maxLength = (Integer) request.getOrDefault("maxLength", 200);
            
            if (content == null || content.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(ApiResponse.error("内容不能为空"));
            }
            
            String summary = aiService.generateSummary(content, maxLength);
            
            return ResponseEntity.ok(ApiResponse.success("摘要生成成功", summary));
        } catch (Exception e) {
            log.error("摘要生成失败: {}", e.getMessage());
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("生成失败: " + e.getMessage()));
        }
    }
    
    /**
     * 情感分析
     */
    @PostMapping("/sentiment")
    public ResponseEntity<ApiResponse<Map<String, Object>>> sentimentAnalysis(
            @RequestBody Map<String, String> request) {
        try {
            String content = request.get("content");
            
            if (content == null || content.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(ApiResponse.error("内容不能为空"));
            }
            
            Map<String, Object> result = aiService.sentimentAnalysis(content);
            
            return ResponseEntity.ok(ApiResponse.success("分析成功", result));
        } catch (Exception e) {
            log.error("情感分析失败: {}", e.getMessage());
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("分析失败: " + e.getMessage()));
        }
    }
    
    /**
     * 关键词提取
     */
    @PostMapping("/keywords")
    public ResponseEntity<ApiResponse<List<String>>> extractKeywords(
            @RequestBody Map<String, Object> request) {
        try {
            String content = (String) request.get("content");
            Integer maxKeywords = (Integer) request.getOrDefault("maxKeywords", 10);
            
            if (content == null || content.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(ApiResponse.error("内容不能为空"));
            }
            
            List<String> keywords = aiService.extractKeywords(content, maxKeywords);
            
            return ResponseEntity.ok(ApiResponse.success("提取成功", keywords));
        } catch (Exception e) {
            log.error("关键词提取失败: {}", e.getMessage());
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("提取失败: " + e.getMessage()));
        }
    }
    
    /**
     * 获取可用AI模型列表
     */
    @GetMapping("/models")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getAvailableModels() {
        try {
            var models = aiService.getAvailableModels();
            List<Map<String, Object>> modelList = models.stream()
                .map(model -> {
                    Map<String, Object> map = new java.util.HashMap<>();
                    map.put("id", model.getId());
                    map.put("modelName", model.getModelName());
                    map.put("provider", model.getProvider().name());
                    map.put("modelType", model.getModelType().name());
                    map.put("isEnabled", model.getIsEnabled());
                    return map;
                })
                .collect(java.util.stream.Collectors.toList());
            
            return ResponseEntity.ok(ApiResponse.success("获取成功", modelList));
        } catch (Exception e) {
            log.error("获取模型列表失败: {}", e.getMessage());
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("获取失败: " + e.getMessage()));
        }
    }
    
    /**
     * 测试AI模型连接
     */
    @PostMapping("/test-model/{modelId}")
    public ResponseEntity<ApiResponse<String>> testModelConnection(
            @PathVariable Long modelId) {
        try {
            boolean isConnected = aiService.testModelConnection(modelId);
            
            if (isConnected) {
                return ResponseEntity.ok(ApiResponse.success("连接测试成功"));
            } else {
                return ResponseEntity.badRequest()
                    .body(ApiResponse.error("连接测试失败"));
            }
        } catch (Exception e) {
            log.error("模型连接测试失败: {}", e.getMessage());
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("测试失败: " + e.getMessage()));
        }
    }
    
    // 辅助方法：从请求中获取当前用户
    private User getCurrentUser(HttpServletRequest request) {
        // 实际应用中应该从JWT token解析用户信息
        return userService.findUserById(1L); // 示例用户ID
    }
}