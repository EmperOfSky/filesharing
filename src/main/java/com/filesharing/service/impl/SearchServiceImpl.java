package com.filesharing.service.impl;

import com.filesharing.entity.FileEntity;
import com.filesharing.entity.Folder;
import com.filesharing.entity.SearchRecord;
import com.filesharing.entity.User;
import com.filesharing.exception.BusinessException;
import com.filesharing.repository.FileRepository;
import com.filesharing.repository.FolderRepository;
import com.filesharing.repository.SearchRecordRepository;
import com.filesharing.service.SearchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 搜索服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class SearchServiceImpl implements SearchService {
    
    private final FileRepository fileRepository;
    private final FolderRepository folderRepository;
    private final SearchRecordRepository searchRecordRepository;
    
    @Override
    public SearchResult search(SearchRequest request, User currentUser) {
        long startTime = System.currentTimeMillis();
        
        List<SearchResultItem> results = new ArrayList<>();
        int totalResults = 0;
        
        try {
            // 根据搜索类型执行不同搜索
            switch (request.getSearchType()) {
                case FILENAME:
                    results = searchByFilename(request.getKeyword(), currentUser, request.getFileTypes());
                    break;
                case CONTENT:
                    results = searchByContent(request.getKeyword(), currentUser);
                    break;
                case TAG:
                    results = searchByTag(request.getKeyword(), currentUser);
                    break;
                case ALL:
                default:
                    results = searchAll(request.getKeyword(), currentUser, request.getFileTypes());
                    break;
            }
            
            totalResults = results.size();
            
        } finally {
            long duration = System.currentTimeMillis() - startTime;
            
            // 记录搜索历史
            recordSearch(request, currentUser, totalResults, duration);
            
            log.info("搜索完成: 关键词={}, 类型={}, 结果数={}, 耗时={}ms", 
                    request.getKeyword(), request.getSearchType(), totalResults, duration);
        }
        
        // 分页处理
        Pageable pageable = request.getPageable();
        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), results.size());
        
        List<SearchResultItem> pagedResults = results.subList(start, end);
        Page<SearchResultItem> resultPage = new PageImpl<>(pagedResults, pageable, results.size());
        
        SearchResult searchResult = new SearchResult();
        searchResult.setResults(resultPage);
        searchResult.setTotalResults(totalResults);
        searchResult.setSearchTime(System.currentTimeMillis() - startTime);
        searchResult.setHasMore(results.size() > end);
        
        return searchResult;
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<SearchSuggestion> getSuggestions(String keyword, User currentUser) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return new ArrayList<>();
        }
        
        List<SearchSuggestion> suggestions = new ArrayList<>();
        
        // 文件名建议
        List<FileEntity> files = fileRepository.findByOriginalNameContainingIgnoreCaseAndUploader(
                keyword, currentUser);
        files.stream()
                .limit(5)
                .forEach(file -> {
                    SearchSuggestion suggestion = new SearchSuggestion();
                    suggestion.setText(file.getOriginalName());
                    suggestion.setType("file");
                    suggestion.setRelevance(calculateRelevance(keyword, file.getOriginalName()));
                    suggestions.add(suggestion);
                });
        
        // 文件夹名建议
        List<Folder> folders = folderRepository.findByOwner(currentUser);
        folders.stream()
                .filter(folder -> folder.getName().toLowerCase().contains(keyword.toLowerCase()))
                .limit(3)
                .forEach(folder -> {
                    SearchSuggestion suggestion = new SearchSuggestion();
                    suggestion.setText(folder.getName());
                    suggestion.setType("folder");
                    suggestion.setRelevance(calculateRelevance(keyword, folder.getName()));
                    suggestions.add(suggestion);
                });
        
        // 按相关性排序并返回前10个
        return suggestions.stream()
                .sorted((s1, s2) -> Double.compare(s2.getRelevance(), s1.getRelevance()))
                .limit(10)
                .collect(Collectors.toList());
    }
    
    @Override
    @Transactional(readOnly = true)
    public Page<SearchHistory> getSearchHistory(User user, Pageable pageable) {
        return searchRecordRepository.findByUserOrderBySearchTimeDesc(user, pageable)
                .map(record -> {
                    SearchHistory history = new SearchHistory();
                    history.setId(record.getId());
                    history.setKeyword(record.getSearchKeyword());
                    history.setSearchType(record.getSearchType().name());
                    history.setResultCount(record.getResultCount());
                    history.setSearchTime(record.getSearchTime());
                    history.setSearchDuration(record.getSearchDuration());
                    return history;
                });
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<PopularSearch> getPopularSearches(int limit) {
        return searchRecordRepository.findPopularSearches(limit)
                .stream()
                .map(obj -> {
                    PopularSearch popular = new PopularSearch();
                    popular.setKeyword((String) obj[0]);
                    popular.setSearchCount(((Number) obj[1]).intValue());
                    return popular;
                })
                .collect(Collectors.toList());
    }
    
    @Override
    public void clearSearchHistory(User user) {
        searchRecordRepository.deleteByUser(user);
        log.info("清除用户搜索历史: 用户ID={}", user.getId());
    }
    
    // ==================== 私有方法 ====================
    
    private List<SearchResultItem> searchByFilename(String keyword, User user, List<String> fileTypes) {
        List<SearchResultItem> results = new ArrayList<>();
        
        // 搜索文件
        List<FileEntity> files = fileRepository.findByOriginalNameContainingIgnoreCaseAndUploader(
                keyword, user);
        
        // 过滤文件类型
        if (fileTypes != null && !fileTypes.isEmpty()) {
            files = files.stream()
                    .filter(file -> fileTypes.contains(file.getFileType()))
                    .collect(Collectors.toList());
        }
        
        files.forEach(file -> {
            SearchResultItem item = new SearchResultItem();
            item.setId(file.getId());
            item.setName(file.getOriginalName());
            item.setType("file");
            item.setFileType(file.getFileType());
            item.setSize(file.getFileSize());
            item.setPath(getFilePath(file)); // 简化实现
            item.setModifiedAt(file.getUpdatedAt());
            item.setRelevance(calculateRelevance(keyword, file.getOriginalName()));
            results.add(item);
        });
        
        // 搜索文件夹
        List<Folder> folders = folderRepository.findByOwner(user);
        folders.stream()
                .filter(folder -> folder.getName().toLowerCase().contains(keyword.toLowerCase()))
                .forEach(folder -> {
                    SearchResultItem item = new SearchResultItem();
                    item.setId(folder.getId());
                    item.setName(folder.getName());
                    item.setType("folder");
                    item.setPath(getFolderPath(folder)); // 简化实现
                    item.setModifiedAt(folder.getUpdatedAt());
                    item.setRelevance(calculateRelevance(keyword, folder.getName()));
                    results.add(item);
                });
        
        return results.stream()
                .sorted((i1, i2) -> Double.compare(i2.getRelevance(), i1.getRelevance()))
                .collect(Collectors.toList());
    }
    
    private List<SearchResultItem> searchByContent(String keyword, User user) {
        // 简化实现：只搜索文件名
        return searchByFilename(keyword, user, null);
    }
    
    private List<SearchResultItem> searchByTag(String keyword, User user) {
        // 简化实现：只搜索文件名
        return searchByFilename(keyword, user, null);
    }
    
    private List<SearchResultItem> searchAll(String keyword, User user, List<String> fileTypes) {
        return searchByFilename(keyword, user, fileTypes);
    }
    
    private void recordSearch(SearchRequest request, User user, int resultCount, long duration) {
        SearchRecord record = new SearchRecord();
        record.setSearchKeyword(request.getKeyword());
        record.setUser(user);
        record.setSearchType(SearchRecord.SearchType.valueOf(request.getSearchType().name()));
        record.setResultCount(resultCount);
        record.setSearchDuration(duration);
        record.setHasFilters(request.getFileTypes() != null && !request.getFileTypes().isEmpty());
        record.setFilterConditions(request.getFileTypes() != null ? 
                String.join(",", request.getFileTypes()) : null);
        record.setSearchTime(LocalDateTime.now());
        record.setClientIp("127.0.0.1"); // 简化实现
        record.setUserAgent("API Client"); // 简化实现
        
        searchRecordRepository.save(record);
    }
    
    private double calculateRelevance(String keyword, String text) {
        if (text == null || keyword == null) return 0.0;
        
        String lowerText = text.toLowerCase();
        String lowerKeyword = keyword.toLowerCase();
        
        // 完全匹配得分最高
        if (lowerText.equals(lowerKeyword)) {
            return 1.0;
        }
        
        // 包含匹配
        if (lowerText.contains(lowerKeyword)) {
            return 0.8 + (0.2 * (double) lowerKeyword.length() / lowerText.length());
        }
        
        // 部分匹配
        int matches = 0;
        for (char c : lowerKeyword.toCharArray()) {
            if (lowerText.indexOf(c) >= 0) {
                matches++;
            }
        }
        
        return (double) matches / lowerKeyword.length() * 0.5;
    }
    
    private String getFilePath(FileEntity file) {
        // 简化实现
        return "/files/" + file.getId();
    }
    
    private String getFolderPath(Folder folder) {
        // 简化实现
        return "/folders/" + folder.getId();
    }
}