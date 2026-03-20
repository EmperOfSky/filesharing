package com.filesharing.controller;

import com.filesharing.dto.ApiResponse;
import com.filesharing.entity.SmartRecommendation;
import com.filesharing.entity.User;
import com.filesharing.service.SmartRecommendationService;
import com.filesharing.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
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
 * 智能推荐控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/recommendations")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Tag(name = "智能推荐", description = "智能推荐相关API")
public class RecommendationController {
    
    private final SmartRecommendationService recommendationService;
    private final UserService userService;
    
    /**
     * 生成个性化推荐
     */
    @PostMapping("/generate")
    @Operation(summary = "生成个性化推荐", description = "为当前用户生成个性化文件和内容推荐")
    public ResponseEntity<ApiResponse<List<SmartRecommendation>>> generateRecommendations(
            HttpServletRequest request) {
        try {
            User currentUser = userService.getCurrentUser(request);
            List<SmartRecommendation> recommendations = recommendationService.generatePersonalizedRecommendations(currentUser);
            
            return ResponseEntity.ok(ApiResponse.success("推荐生成成功", recommendations));
            
        } catch (Exception e) {
            log.error("生成推荐失败", e);
            return ResponseEntity.badRequest().body(ApiResponse.error("推荐生成失败: " + e.getMessage()));
        }
    }
    
    /**
     * 获取用户推荐列表
     */
    @GetMapping
    @Operation(summary = "获取推荐列表", description = "获取当前用户的推荐列表")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getUserRecommendations(
            @Parameter(description = "页码") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "10") int size,
            HttpServletRequest request) {
        try {
            User currentUser = userService.getCurrentUser(request);
            int safePage = Math.max(page, 0);
            int safeSize = Math.max(size, 1);
            Page<SmartRecommendation> recommendations = recommendationService.getUserRecommendations(currentUser, safePage, safeSize);
            
            Map<String, Object> result = new HashMap<>();
            result.put("content", recommendations.getContent());
            result.put("recommendations", recommendations.getContent());
            result.put("number", recommendations.getNumber());
            result.put("currentPage", recommendations.getNumber());
            result.put("totalPages", recommendations.getTotalPages());
            result.put("totalElements", recommendations.getTotalElements());
            result.put("numberOfElements", recommendations.getNumberOfElements());
            result.put("size", recommendations.getSize());
            result.put("first", recommendations.isFirst());
            result.put("last", recommendations.isLast());
            
            return ResponseEntity.ok(ApiResponse.success("获取推荐列表成功", result));
            
        } catch (Exception e) {
            log.error("获取推荐列表失败", e);
            return ResponseEntity.badRequest().body(ApiResponse.error("获取推荐列表失败: " + e.getMessage()));
        }
    }
    
    /**
     * 标记推荐为已查看
     */
    @PutMapping("/{id}/view")
    @Operation(summary = "标记推荐为已查看", description = "将指定推荐标记为已查看状态")
    public ResponseEntity<ApiResponse<Void>> markAsViewed(
            @Parameter(description = "推荐ID") @PathVariable Long id,
            HttpServletRequest request) {
        try {
            User currentUser = userService.getCurrentUser(request);
            recommendationService.markRecommendationViewed(id, currentUser);
            
            return ResponseEntity.ok(ApiResponse.<Void>success(null));
            
        } catch (Exception e) {
            log.error("标记推荐为已查看失败: ID={}", id, e);
            return ResponseEntity.badRequest().body(ApiResponse.error("标记失败: " + e.getMessage()));
        }
    }
    
    /**
     * 标记推荐为已采纳
     */
    @PutMapping("/{id}/adopt")
    @Operation(summary = "标记推荐为已采纳", description = "将指定推荐标记为已采纳状态")
    public ResponseEntity<ApiResponse<Void>> markAsAdopted(
            @Parameter(description = "推荐ID") @PathVariable Long id,
            HttpServletRequest request) {
        try {
            User currentUser = userService.getCurrentUser(request);
            recommendationService.markRecommendationAdopted(id, currentUser);
            
            return ResponseEntity.ok(ApiResponse.<Void>success(null));
            
        } catch (Exception e) {
            log.error("标记推荐为已采纳失败: ID={}", id, e);
            return ResponseEntity.badRequest().body(ApiResponse.error("采纳标记失败: " + e.getMessage()));
        }
    }
    
    /**
     * 获取推荐分析数据
     */
    @GetMapping("/analytics")
    @Operation(summary = "获取推荐分析", description = "获取用户的推荐效果分析数据")
    public ResponseEntity<ApiResponse<SmartRecommendationService.RecommendationAnalytics>> getRecommendationAnalytics(
            HttpServletRequest request) {
        try {
            User currentUser = userService.getCurrentUser(request);
            SmartRecommendationService.RecommendationAnalytics analytics = 
                recommendationService.getRecommendationAnalytics(currentUser);
            
            return ResponseEntity.ok(ApiResponse.success("获取分析数据成功", analytics));
            
        } catch (Exception e) {
            log.error("获取推荐分析失败", e);
            return ResponseEntity.badRequest().body(ApiResponse.error("获取分析数据失败: " + e.getMessage()));
        }
    }
    
    /**
     * 获取相似项推荐
     */
    @GetMapping("/similar/{itemId}")
    @Operation(summary = "获取相似项推荐", description = "根据指定项获取相似内容推荐")
    public ResponseEntity<ApiResponse<List<SmartRecommendation>>> getSimilarRecommendations(
            @Parameter(description = "项目ID") @PathVariable Long itemId,
            @Parameter(description = "推荐类型") @RequestParam SmartRecommendation.RecommendationType type,
            HttpServletRequest request) {
        try {
            User currentUser = userService.getCurrentUser(request);
            List<SmartRecommendation> recommendations = 
                recommendationService.getSimilarItemsRecommendation(itemId, type, currentUser);
            
            return ResponseEntity.ok(ApiResponse.success("获取相似推荐成功", recommendations));
            
        } catch (Exception e) {
            log.error("获取相似推荐失败: 项目ID={}, 类型={}", itemId, type, e);
            return ResponseEntity.badRequest().body(ApiResponse.error("获取相似推荐失败: " + e.getMessage()));
        }
    }
    
    /**
     * 清理过期推荐（管理员功能）
     */
    @DeleteMapping("/cleanup")
    @Operation(summary = "清理过期推荐", description = "清理系统中所有过期的推荐记录")
    public ResponseEntity<ApiResponse<Map<String, Object>>> cleanupExpiredRecommendations() {
        try {
            recommendationService.cleanupExpiredRecommendations();
            
            Map<String, Object> result = new HashMap<>();
            result.put("message", "过期推荐清理完成");
            result.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.ok(ApiResponse.success("清理完成", result));
            
        } catch (Exception e) {
            log.error("清理过期推荐失败", e);
            return ResponseEntity.badRequest().body(ApiResponse.error("清理失败: " + e.getMessage()));
        }
    }
}