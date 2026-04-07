package com.filesharing.controller;

import com.filesharing.dto.ApiResponse;
import com.filesharing.dto.SearchRequest;
import com.filesharing.dto.SearchResponse;
import com.filesharing.entity.User;
import com.filesharing.service.SearchService;
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
 * 搜索控制器
 */
@Slf4j
@RestController
@RequestMapping({"/api/search", "/api/files/search"})
@RequiredArgsConstructor
public class SearchController {

    private final SearchService searchService;
    private final UserService userService;

    /**
     * 执行搜索（GET 形式，方便直接调试）
     */
    @GetMapping
    public ResponseEntity<ApiResponse<SearchResponse>> searchByQuery(
            @RequestParam(defaultValue = "") String keyword,
            @RequestParam(defaultValue = "ALL") String searchType,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            HttpServletRequest request) {
        try {
            SearchRequest searchRequest = new SearchRequest();
            searchRequest.setKeyword(keyword == null ? "" : keyword.trim());
            searchRequest.setSearchType(parseSearchType(searchType));
            searchRequest.setPage(Math.max(page, 0));
            searchRequest.setSize(Math.max(size, 1));
            return doSearch(searchRequest, request);
        } catch (Exception e) {
            log.error("执行搜索失败: {}", e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * 执行高级搜索（POST JSON）
     */
    @PostMapping
    public ResponseEntity<ApiResponse<SearchResponse>> searchByBody(
            @RequestBody(required = false) SearchRequest searchRequest,
            HttpServletRequest request) {
        try {
            SearchRequest safeRequest = searchRequest == null ? new SearchRequest() : searchRequest;
            if (safeRequest.getKeyword() == null) {
                safeRequest.setKeyword("");
            }
            if (safeRequest.getSearchType() == null) {
                safeRequest.setSearchType(SearchRequest.SearchType.ALL);
            }
            if (safeRequest.getPage() == null || safeRequest.getPage() < 0) {
                safeRequest.setPage(0);
            }
            if (safeRequest.getSize() == null || safeRequest.getSize() <= 0) {
                safeRequest.setSize(10);
            }
            return doSearch(safeRequest, request);
        } catch (Exception e) {
            log.error("执行高级搜索失败: {}", e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * 获取搜索建议
     */
    @GetMapping("/suggestions")
    public ResponseEntity<ApiResponse<List<String>>> getSuggestions(
            @RequestParam(defaultValue = "") String keyword,
            HttpServletRequest request) {
        try {
            User currentUser = userService.getCurrentUser(request);
            List<String> suggestions = searchService.getSearchSuggestions(keyword, currentUser);
            return ResponseEntity.ok(ApiResponse.success(suggestions));
        } catch (Exception e) {
            log.error("获取搜索建议失败: {}", e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * 获取热门关键词
     */
    @GetMapping("/hot-keywords")
    public ResponseEntity<ApiResponse<List<SearchService.HotKeyword>>> getHotKeywords(
            @RequestParam(defaultValue = "10") int limit) {
        try {
            List<SearchService.HotKeyword> hotKeywords = searchService.getHotKeywords(limit);
            return ResponseEntity.ok(ApiResponse.success(hotKeywords));
        } catch (Exception e) {
            log.error("获取热门关键词失败: {}", e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * 获取搜索历史
     */
    @GetMapping("/history")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getHistory(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            HttpServletRequest request) {
        try {
            User currentUser = userService.getCurrentUser(request);
            Page<SearchService.SearchHistory> historyPage = searchService.getSearchHistory(
                    currentUser,
                    Math.max(page, 0),
                    Math.max(size, 1)
            );

            Map<String, Object> result = new HashMap<>();
            result.put("content", historyPage.getContent());
            result.put("totalPages", historyPage.getTotalPages());
            result.put("totalElements", historyPage.getTotalElements());
            result.put("number", historyPage.getNumber());
            result.put("size", historyPage.getSize());
            result.put("first", historyPage.isFirst());
            result.put("last", historyPage.isLast());

            return ResponseEntity.ok(ApiResponse.success(result));
        } catch (Exception e) {
            log.error("获取搜索历史失败: {}", e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * 获取搜索统计
     */
    @GetMapping("/statistics")
    public ResponseEntity<ApiResponse<SearchService.SearchStatistics>> getStatistics() {
        try {
            SearchService.SearchStatistics statistics = searchService.getSearchStatistics();
            return ResponseEntity.ok(ApiResponse.success(statistics));
        } catch (Exception e) {
            log.error("获取搜索统计失败: {}", e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * 清理过期搜索记录
     */
    @PostMapping("/cleanup")
    public ResponseEntity<ApiResponse<String>> cleanupExpiredRecords() {
        try {
            searchService.cleanupExpiredSearchRecords();
            return ResponseEntity.ok(ApiResponse.success("过期搜索记录清理完成"));
        } catch (Exception e) {
            log.error("清理过期搜索记录失败: {}", e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    private ResponseEntity<ApiResponse<SearchResponse>> doSearch(SearchRequest searchRequest, HttpServletRequest request) {
        User currentUser = userService.getCurrentUser(request);
        String clientIp = resolveClientIp(request);
        String userAgent = request.getHeader("User-Agent");
        SearchResponse response = searchService.searchFiles(searchRequest, currentUser, clientIp, userAgent);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    private SearchRequest.SearchType parseSearchType(String searchType) {
        if (searchType == null || searchType.isBlank()) {
            return SearchRequest.SearchType.ALL;
        }
        try {
            return SearchRequest.SearchType.valueOf(searchType.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            return SearchRequest.SearchType.ALL;
        }
    }

    private String resolveClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            return xForwardedFor.split(",")[0].trim();
        }
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isBlank()) {
            return xRealIp;
        }
        return request.getRemoteAddr();
    }
}
