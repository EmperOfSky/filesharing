package com.filesharing.service.impl;

import com.filesharing.dto.SearchRequest;
import com.filesharing.dto.SearchResponse;
import com.filesharing.entity.FileEntity;
import com.filesharing.entity.SearchRecord;
import com.filesharing.entity.User;
import com.filesharing.repository.FileRepository;
import com.filesharing.repository.FolderRepository;
import com.filesharing.repository.SearchRecordRepository;
import com.filesharing.service.SearchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 搜索服务实现类
 * @author Admin
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
    public SearchResponse searchFiles(SearchRequest request, User user, String clientIp, String userAgent) {
        long start = System.currentTimeMillis();

        int page = Math.max(request.getPage() == null ? 0 : request.getPage(), 0);
        int size = Math.max(request.getSize() == null ? 10 : request.getSize(), 1);

        Page<SearchResult> pageResult;
        SearchRequest.SearchType searchType = request.getSearchType() == null
            ? SearchRequest.SearchType.ALL
            : request.getSearchType();

        switch (searchType) {
            case FILENAME:
            pageResult = searchByFileName(request.getKeyword(), user, page, size);
            break;
            case CONTENT:
            pageResult = searchByContent(request.getKeyword(), user, page, size);
            break;
            case ALL:
            case TAG:
            default:
            pageResult = advancedSearch(request, user, page, size);
            break;
        }

        List<SearchResponse.SearchResult> responseResults = pageResult.getContent().stream()
            .map(this::toResponseResult)
            .collect(Collectors.toList());

        List<SearchResponse.Suggestion> suggestions = getSearchSuggestions(request.getKeyword(), user).stream()
            .limit(5)
            .map(s -> SearchResponse.Suggestion.builder()
                .keyword(s)
                .resultCount(0L)
                .type("keyword")
                .build())
            .collect(Collectors.toList());

        long duration = System.currentTimeMillis() - start;

        SearchRecord record = new SearchRecord();
        record.setSearchKeyword(request.getKeyword());
        record.setUser(user);
        record.setSearchType(toEntitySearchType(searchType));
        record.setResultCount((int) pageResult.getTotalElements());
        record.setSearchDuration(duration);
        record.setHasFilters(hasFilters(request));
        record.setFilterConditions(buildFilterSummary(request));
        record.setClientIp(clientIp);
        record.setUserAgent(userAgent);
        searchRecordRepository.save(record);

        SearchResponse response = new SearchResponse();
        response.setResults(responseResults);
        response.setTotalResults(pageResult.getTotalElements());
        response.setCurrentPage(pageResult.getNumber());
        response.setTotalPages(pageResult.getTotalPages());
        response.setSearchDuration(duration);
        response.setSearchKeyword(request.getKeyword());
        response.setSearchType(searchType.name());
        response.setSuggestions(suggestions);

        log.info("执行搜索：关键词={}, 用户={}, 结果={}, 耗时={}ms",
            request.getKeyword(), user.getUsername(), pageResult.getTotalElements(), duration);
        return response;
    }
    
    @Override
    public Page<SearchResult> searchByFileName(String keyword, User user, int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.max(size, 1);
        String safeKeyword = keyword == null ? "" : keyword.trim();

        Page<FileEntity> files = fileRepository.findByUploaderAndOriginalNameContaining(
            user,
            safeKeyword,
            PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.DESC, "updatedAt"))
        );

        List<SearchResult> results = files.getContent().stream()
            .filter(this::isAvailable)
            .map(file -> toSearchResult(file, safeKeyword, 0.95))
            .collect(Collectors.toList());

        return new PageImpl<>(results, files.getPageable(), files.getTotalElements());
    }
    
    @Override
    public Page<SearchResult> searchByContent(String keyword, User user, int page, int size) {
        // 当前项目未接入全文索引，这里使用文件名匹配作为兼容实现。
        Page<SearchResult> pageResult = searchByFileName(keyword, user, page, size);
        List<SearchResult> tuned = pageResult.getContent().stream().peek(item -> {
            item.setScore(Math.min(item.getScore() == null ? 0.8 : item.getScore(), 0.85));
            item.setHighlight("content~" + (keyword == null ? "" : keyword));
        }).collect(Collectors.toList());
        return new PageImpl<>(tuned, pageResult.getPageable(), pageResult.getTotalElements());
    }
    
    @Override
    public Page<SearchResult> searchAll(String keyword, User user, int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.max(size, 1);

        // 先拉取较多候选结果，再在内存中融合、去重、分页。
        List<SearchResult> nameResults = searchByFileName(keyword, user, 0, Math.max(safeSize * 3, 30)).getContent();
        List<SearchResult> contentResults = searchByContent(keyword, user, 0, Math.max(safeSize * 3, 30)).getContent();

        Map<Long, SearchResult> merged = new HashMap<>();
        for (SearchResult item : nameResults) {
            merged.put(item.getFileId(), item);
        }
        for (SearchResult item : contentResults) {
            merged.merge(item.getFileId(), item, (a, b) -> {
                double scoreA = a.getScore() == null ? 0 : a.getScore();
                double scoreB = b.getScore() == null ? 0 : b.getScore();
                return scoreA >= scoreB ? a : b;
            });
        }

        List<SearchResult> sorted = merged.values().stream()
                .sorted(Comparator.comparing((SearchResult r) -> r.getScore() == null ? 0 : r.getScore()).reversed())
                .collect(Collectors.toList());

        return slicePage(sorted, safePage, safeSize);
    }
    
    @Override
    public Page<SearchResult> advancedSearch(SearchRequest request, User user, int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.max(size, 1);

        List<SearchResult> all = searchAll(request.getKeyword(), user, 0, Math.max(safeSize * 5, 100)).getContent();

        List<SearchResult> filtered = all.stream().filter(result -> {
            if (request.getFileTypes() != null && !request.getFileTypes().isEmpty()) {
                Set<String> typeSet = request.getFileTypes().stream()
                        .filter(Objects::nonNull)
                        .map(t -> t.toLowerCase(Locale.ROOT))
                        .collect(Collectors.toSet());
                String fileType = result.getFileType() == null ? "" : result.getFileType().toLowerCase(Locale.ROOT);
                if (!typeSet.contains(fileType)) {
                    return false;
                }
            }

            if (request.getMinSize() != null && (result.getFileSize() == null || result.getFileSize() < request.getMinSize())) {
                return false;
            }

            if (request.getMaxSize() != null && (result.getFileSize() == null || result.getFileSize() > request.getMaxSize())) {
                return false;
            }

            if (request.getUploader() != null && !request.getUploader().isBlank()) {
                String expected = request.getUploader().toLowerCase(Locale.ROOT);
                String actual = result.getUploaderName() == null ? "" : result.getUploaderName().toLowerCase(Locale.ROOT);
                if (!actual.contains(expected)) {
                    return false;
                }
            }

            if (request.getFolderPath() != null && !request.getFolderPath().isBlank()) {
                String expected = request.getFolderPath().toLowerCase(Locale.ROOT);
                String actual = result.getFolderPath() == null ? "" : result.getFolderPath().toLowerCase(Locale.ROOT);
                if (!actual.contains(expected)) {
                    return false;
                }
            }

            return true;
        }).collect(Collectors.toList());

        return slicePage(filtered, safePage, safeSize);
    }
    
    @Override
    public List<String> getSearchSuggestions(String partialKeyword, User user) {
        if (partialKeyword == null || partialKeyword.trim().isEmpty()) {
            return new ArrayList<>();
        }

        String keyword = partialKeyword.trim().toLowerCase(Locale.ROOT);
        return fileRepository.findByUploaderAndOriginalNameContaining(
                        user,
                        partialKeyword.trim(),
                        PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "updatedAt")))
                .getContent()
                .stream()
                .map(FileEntity::getOriginalName)
                .filter(Objects::nonNull)
                .filter(name -> name.toLowerCase(Locale.ROOT).contains(keyword))
                .distinct()
                .limit(10)
                .collect(Collectors.toList());
    }
    
    @Override
    public List<HotKeyword> getHotKeywords(int limit) {
        int safeLimit = Math.max(limit, 1);
        List<Object[]> rows = searchRecordRepository.findPopularKeywords(
                LocalDateTime.now().minusDays(30),
                PageRequest.of(0, safeLimit)
        );

        List<HotKeyword> result = new ArrayList<>();
        for (Object[] row : rows) {
            String keyword = row[0] == null ? "" : row[0].toString();
            long count = row[1] instanceof Number ? ((Number) row[1]).longValue() : 0L;
            result.add(new HotKeyword(keyword, count));
        }
        return result;
    }
    
    @Override
    public Page<SearchHistory> getSearchHistory(User user, int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.max(size, 1);

        Page<SearchRecord> records = searchRecordRepository.findByUser(
            user,
            PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.DESC, "searchTime"))
        );

        List<SearchHistory> histories = records.getContent().stream()
            .map(record -> new SearchHistory(
                record.getId(),
                record.getSearchKeyword(),
                record.getSearchType() == null ? null : record.getSearchType().name(),
                record.getResultCount(),
                record.getSearchDuration(),
                record.getSearchTime() == null ? null : record.getSearchTime().toString()
            ))
            .collect(Collectors.toList());

        return new PageImpl<>(histories, records.getPageable(), records.getTotalElements());
    }
    
    @Override
    public void cleanupExpiredSearchRecords() {
        LocalDateTime threshold = LocalDateTime.now().minusDays(90);
        List<SearchRecord> expired = searchRecordRepository.findAll().stream()
                .filter(r -> r.getSearchTime() != null && r.getSearchTime().isBefore(threshold))
                .collect(Collectors.toList());
        if (!expired.isEmpty()) {
            searchRecordRepository.deleteAll(expired);
        }
        log.info("清理过期搜索记录: 删除数量={}", expired.size());
    }
    
    @Override
    public SearchStatistics getSearchStatistics() {
        SearchStatistics stats = new SearchStatistics();
        Object[] row = searchRecordRepository.getSearchStatistics(LocalDateTime.now().minusDays(30));

        long total = row != null && row.length > 0 && row[0] instanceof Number ? ((Number) row[0]).longValue() : 0L;
        long success = row != null && row.length > 1 && row[1] instanceof Number ? ((Number) row[1]).longValue() : 0L;
        double avg = row != null && row.length > 2 && row[2] instanceof Number ? ((Number) row[2]).doubleValue() : 0.0;

        stats.setTotalSearches(total);
        stats.setSuccessfulSearches(success);
        stats.setSuccessRate(total > 0 ? (double) success / total * 100 : 0.0);
        stats.setAverageDuration(avg);

        Map<SearchRecord.SearchType, Long> typeCounter = searchRecordRepository.findAll().stream()
                .collect(Collectors.groupingBy(SearchRecord::getSearchType, Collectors.counting()));

        List<SearchStatistics.SearchTypeStat> typeStats = new ArrayList<>();
        for (Map.Entry<SearchRecord.SearchType, Long> entry : typeCounter.entrySet()) {
            double percent = total > 0 ? (double) entry.getValue() / total * 100 : 0.0;
            typeStats.add(new SearchStatistics.SearchTypeStat(entry.getKey().name(), entry.getValue(), percent));
        }
        stats.setTypeStats(typeStats);

        List<SearchStatistics.DailySearchStat> daily = searchRecordRepository.findAll().stream()
                .filter(r -> r.getSearchTime() != null)
                .collect(Collectors.groupingBy(r -> r.getSearchTime().toLocalDate(), Collectors.toList()))
                .entrySet()
                .stream()
                .sorted(Map.Entry.<java.time.LocalDate, List<SearchRecord>>comparingByKey().reversed())
                .limit(7)
                .map(entry -> {
                    long searchCount = entry.getValue().size();
                    long successCount = entry.getValue().stream().filter(r -> r.getResultCount() != null && r.getResultCount() > 0).count();
                    double avgDuration = entry.getValue().stream()
                            .filter(r -> r.getSearchDuration() != null)
                            .mapToLong(SearchRecord::getSearchDuration)
                            .average().orElse(0.0);
                    return new SearchStatistics.DailySearchStat(entry.getKey().toString(), searchCount, successCount, avgDuration);
                })
                .collect(Collectors.toList());
        stats.setDailyStats(daily);

        return stats;
    }

    private SearchResult toSearchResult(FileEntity file, String keyword, double score) {
        SearchResult result = new SearchResult();
        result.setFileId(file.getId());
        result.setFileName(file.getOriginalName());
        result.setFileType(file.getExtension());
        result.setFileSize(file.getFileSize());
        result.setFilePath(file.getFilePath());
        result.setUploaderName(file.getUploader() == null ? null : file.getUploader().getUsername());
        result.setFolderPath(file.getFolder() == null ? "/" : file.getFolder().getFolderPath());
        result.setHighlight(keyword == null ? null : keyword);
        result.setScore(score);
        result.setCreatedAt(file.getCreatedAt() == null ? null : file.getCreatedAt().toString());
        return result;
    }

    private SearchResponse.SearchResult toResponseResult(SearchResult result) {
        return SearchResponse.SearchResult.builder()
                .fileId(result.getFileId())
                .fileName(result.getFileName())
                .fileType(result.getFileType())
                .fileSize(result.getFileSize())
                .fileIcon(result.getFileType() == null ? "file" : result.getFileType().toLowerCase(Locale.ROOT))
                .previewUrl(result.getFileId() == null ? null : "/api/preview/" + result.getFileId())
                .downloadUrl(result.getFileId() == null ? null : "/api/files/" + result.getFileId() + "/download")
                .uploaderName(result.getUploaderName())
                .folderPath(result.getFolderPath())
                .highlight(result.getHighlight())
                .relevanceScore(result.getScore())
                .createdAt(result.getCreatedAt())
                .updatedAt(result.getCreatedAt())
                .build();
    }

    private SearchRecord.SearchType toEntitySearchType(SearchRequest.SearchType type) {
        if (type == null) {
            return SearchRecord.SearchType.ALL;
        }
        return switch (type) {
            case FILENAME -> SearchRecord.SearchType.FILENAME;
            case CONTENT -> SearchRecord.SearchType.CONTENT;
            case TAG -> SearchRecord.SearchType.TAG;
            case ALL -> SearchRecord.SearchType.ALL;
        };
    }

    private boolean hasFilters(SearchRequest request) {
        return (request.getFileTypes() != null && !request.getFileTypes().isEmpty())
                || request.getMinSize() != null
                || request.getMaxSize() != null
                || (request.getUploader() != null && !request.getUploader().isBlank())
                || (request.getFolderPath() != null && !request.getFolderPath().isBlank())
                || (request.getDateFrom() != null && !request.getDateFrom().isBlank())
                || (request.getDateTo() != null && !request.getDateTo().isBlank());
    }

    private String buildFilterSummary(SearchRequest request) {
        Map<String, Object> summary = new HashMap<>();
        summary.put("fileTypes", request.getFileTypes());
        summary.put("minSize", request.getMinSize());
        summary.put("maxSize", request.getMaxSize());
        summary.put("uploader", request.getUploader());
        summary.put("folderPath", request.getFolderPath());
        summary.put("dateFrom", request.getDateFrom());
        summary.put("dateTo", request.getDateTo());
        return summary.toString();
    }

    private Page<SearchResult> slicePage(List<SearchResult> source, int page, int size) {
        int total = source.size();
        int fromIndex = Math.min(page * size, total);
        int toIndex = Math.min(fromIndex + size, total);
        List<SearchResult> content = source.subList(fromIndex, toIndex);
        return new PageImpl<>(content, PageRequest.of(page, size), total);
    }

    private boolean isAvailable(FileEntity file) {
        return file.getStatus() == null || file.getStatus() == FileEntity.FileStatus.AVAILABLE;
    }
}
