package com.filesharing.service.impl;

import com.filesharing.entity.FileEntity;
import com.filesharing.entity.User;
import com.filesharing.exception.BusinessException;
import com.filesharing.repository.FileRepository;
import com.filesharing.repository.FolderRepository;
import com.filesharing.repository.ShareRepository;
import com.filesharing.repository.UserRepository;
import com.filesharing.service.StatisticsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 统计服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StatisticsServiceImpl implements StatisticsService {
    
    private final FileRepository fileRepository;
    private final FolderRepository folderRepository;
    private final ShareRepository shareRepository;
    private final UserRepository userRepository;
    
    @Override
    public void updateFileStatistics(FileEntity file, StatisticAction action) {
        try {
            switch (action) {
                case DOWNLOAD:
                    file.setDownloadCount(file.getDownloadCount() + 1);
                    file.setLastDownloadAt(LocalDateTime.now());
                    break;
                case PREVIEW:
                    file.setPreviewCount(file.getPreviewCount() + 1);
                    file.setLastPreviewAt(LocalDateTime.now());
                    break;
                case SHARE:
                    file.setShareCount(file.getShareCount() + 1);
                    break;
                case RATE:
                    // 评分逻辑可以根据需要实现
                    break;
            }
            fileRepository.save(file);
            log.debug("更新文件统计: 文件ID={}, 动作={}", file.getId(), action);
        } catch (Exception e) {
            log.error("更新文件统计失败: 文件ID={}, 动作={}", file.getId(), action, e);
        }
    }
    
    @Override
    public FileStatsDetail getFileStatistics(Long fileId) {
        try {
            FileEntity file = fileRepository.findById(fileId)
                .orElseThrow(() -> new BusinessException("文件不存在"));
            
            FileStatsDetail detail = new FileStatsDetail();
            detail.setFileId(file.getId());
            detail.setFileName(file.getOriginalName());
            detail.setFileType(file.getExtension());
            detail.setTotalDownloads(file.getDownloadCount().longValue());
            detail.setTotalPreviews(file.getPreviewCount().longValue());
            detail.setTotalShares(file.getShareCount().longValue());
            detail.setLastDownloadTime(file.getLastDownloadAt() != null ? 
                file.getLastDownloadAt().toString() : null);
            detail.setLastPreviewTime(file.getLastPreviewAt() != null ? 
                file.getLastPreviewAt().toString() : null);
            
            // 计算流行度分数（简化算法）
            double popularityScore = (file.getDownloadCount() * 0.4 + 
                                    file.getPreviewCount() * 0.3 + 
                                    file.getShareCount() * 0.3);
            detail.setPopularityScore(popularityScore);
            
            return detail;
        } catch (Exception e) {
            log.error("获取文件统计详情失败: 文件ID={}", fileId, e);
            throw new BusinessException("获取文件统计失败: " + e.getMessage());
        }
    }
    
    @Override
    public Page<FileRanking> getPopularFiles(int page, int size) {
        try {
            // 按下载次数排序获取热门文件
            Page<FileEntity> files = fileRepository.findAll(
                PageRequest.of(page, size));
            
            List<FileRanking> rankings = files.getContent().stream()
                .sorted(Comparator.comparing(FileEntity::getDownloadCount).reversed())
                .map(file -> {
                    FileRanking ranking = new FileRanking();
                    ranking.setFileId(file.getId());
                    ranking.setFileName(file.getOriginalName());
                    ranking.setDownloadCount(file.getDownloadCount().longValue());
                    ranking.setPreviewCount(file.getPreviewCount().longValue());
                    ranking.setShareCount(file.getShareCount().longValue());
                    return ranking;
                })
                .collect(Collectors.toList());
            
            return new PageImpl<>(rankings, PageRequest.of(page, size), files.getTotalElements());
        } catch (Exception e) {
            log.error("获取热门文件排行失败", e);
            throw new BusinessException("获取热门文件排行失败: " + e.getMessage());
        }
    }
    
    @Override
    public Page<FileBasicInfo> getLatestFiles(int page, int size) {
        try {
            Page<FileEntity> files = fileRepository.findAll(
                PageRequest.of(page, size));
            
            List<FileBasicInfo> fileInfos = files.getContent().stream()
                .sorted(Comparator.comparing(FileEntity::getCreatedAt).reversed())
                .map(file -> {
                    FileBasicInfo info = new FileBasicInfo();
                    info.setFileId(file.getId());
                    info.setFileName(file.getOriginalName());
                    info.setFileSize(file.getFileSize());
                    info.setFileType(file.getExtension());
                    info.setCreatedAt(file.getCreatedAt().toString());
                    return info;
                })
                .collect(Collectors.toList());
            
            return new PageImpl<>(fileInfos, PageRequest.of(page, size), files.getTotalElements());
        } catch (Exception e) {
            log.error("获取最新文件失败", e);
            throw new BusinessException("获取最新文件失败: " + e.getMessage());
        }
    }
    
    @Override
    public Page<FileRanking> getMostActiveFiles(int page, int size) {
        try {
            Page<FileEntity> files = fileRepository.findAll(
                PageRequest.of(page, size));
            
            List<FileRanking> rankings = files.getContent().stream()
                .sorted((f1, f2) -> {
                    long score1 = f1.getDownloadCount() + f1.getPreviewCount() + f1.getShareCount();
                    long score2 = f2.getDownloadCount() + f2.getPreviewCount() + f2.getShareCount();
                    return Long.compare(score2, score1);
                })
                .map(file -> {
                    FileRanking ranking = new FileRanking();
                    ranking.setFileId(file.getId());
                    ranking.setFileName(file.getOriginalName());
                    ranking.setDownloadCount(file.getDownloadCount().longValue());
                    ranking.setPreviewCount(file.getPreviewCount().longValue());
                    ranking.setShareCount(file.getShareCount().longValue());
                    return ranking;
                })
                .collect(Collectors.toList());
            
            return new PageImpl<>(rankings, PageRequest.of(page, size), files.getTotalElements());
        } catch (Exception e) {
            log.error("获取最活跃文件失败", e);
            throw new BusinessException("获取最活跃文件失败: " + e.getMessage());
        }
    }
    
    @Override
    public List<FileTypeDistribution> getFileTypeDistribution() {
        try {
            List<FileEntity> allFiles = fileRepository.findAll();
            
            Map<String, Long> typeCount = allFiles.stream()
                .collect(Collectors.groupingBy(
                    FileEntity::getExtension,
                    Collectors.counting()));
            
            Map<String, Long> typeSize = allFiles.stream()
                .collect(Collectors.groupingBy(
                    FileEntity::getExtension,
                    Collectors.summingLong(FileEntity::getFileSize)));
            
            return typeCount.entrySet().stream()
                .map(entry -> {
                    FileTypeDistribution distribution = new FileTypeDistribution();
                    distribution.setFileType(entry.getKey());
                    distribution.setFileCount(entry.getValue());
                    distribution.setTotalSize(typeSize.get(entry.getKey()));
                    return distribution;
                })
                .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("获取文件类型分布失败", e);
            throw new BusinessException("获取文件类型分布失败: " + e.getMessage());
        }
    }
    
    @Override
    public List<FileSizeDistribution> getFileSizeDistribution() {
        try {
            List<FileEntity> allFiles = fileRepository.findAll();
            
            Map<String, Long> sizeRanges = new LinkedHashMap<>();
            sizeRanges.put("0-1MB", 0L);
            sizeRanges.put("1-10MB", 0L);
            sizeRanges.put("10-100MB", 0L);
            sizeRanges.put("100MB+", 0L);
            
            for (FileEntity file : allFiles) {
                long size = file.getFileSize();
                if (size <= 1024 * 1024) {
                    sizeRanges.put("0-1MB", sizeRanges.get("0-1MB") + 1);
                } else if (size <= 10 * 1024 * 1024) {
                    sizeRanges.put("1-10MB", sizeRanges.get("1-10MB") + 1);
                } else if (size <= 100 * 1024 * 1024) {
                    sizeRanges.put("10-100MB", sizeRanges.get("10-100MB") + 1);
                } else {
                    sizeRanges.put("100MB+", sizeRanges.get("100MB+") + 1);
                }
            }
            
            return sizeRanges.entrySet().stream()
                .map(entry -> {
                    FileSizeDistribution distribution = new FileSizeDistribution();
                    distribution.setSizeRange(entry.getKey());
                    distribution.setFileCount(entry.getValue());
                    return distribution;
                })
                .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("获取文件大小分布失败", e);
            throw new BusinessException("获取文件大小分布失败: " + e.getMessage());
        }
    }
    
    @Override
    public Page<FileBasicInfo> getZeroDownloadFiles(int page, int size) {
        try {
            List<FileEntity> zeroDownloadFiles = fileRepository.findAll().stream()
                .filter(file -> file.getDownloadCount() == 0)
                .collect(Collectors.toList());
            
            List<FileBasicInfo> fileInfos = zeroDownloadFiles.stream()
                .skip(page * size)
                .limit(size)
                .map(file -> {
                    FileBasicInfo info = new FileBasicInfo();
                    info.setFileId(file.getId());
                    info.setFileName(file.getOriginalName());
                    info.setFileSize(file.getFileSize());
                    info.setFileType(file.getExtension());
                    info.setCreatedAt(file.getCreatedAt().toString());
                    return info;
                })
                .collect(Collectors.toList());
            
            return new PageImpl<>(fileInfos, PageRequest.of(page, size), zeroDownloadFiles.size());
        } catch (Exception e) {
            log.error("获取零下载文件失败", e);
            throw new BusinessException("获取零下载文件失败: " + e.getMessage());
        }
    }
    
    @Override
    public void updateUserBehaviorStatistics(User user, BehaviorAction action, Object... params) {
        try {
            switch (action) {
                case UPLOAD:
                    user.setUsedStorage(user.getUsedStorage() + 
                        ((Long) params[0])); // params[0] 应该是文件大小
                    break;
                case DOWNLOAD:
                    // 可以记录下载行为
                    break;
                case PREVIEW:
                    // 可以记录预览行为
                    break;
                case CREATE_FOLDER:
                    // 可以记录创建文件夹行为
                    break;
                case SHARE:
                    // 可以记录分享行为
                    break;
                case LOGIN:
                    user.setLastLoginTime(LocalDateTime.now());
                    break;
                case STORAGE_CHANGE:
                    user.setUsedStorage((Long) params[0]);
                    break;
            }
            userRepository.save(user);
            log.debug("更新用户行为统计: 用户ID={}, 动作={}", user.getId(), action);
        } catch (Exception e) {
            log.error("更新用户行为统计失败: 用户ID={}, 动作={}", user.getId(), action, e);
        }
    }
    
    @Override
    public UserBehaviorDetail getUserBehaviorStatistics(Long userId) {
        try {
            User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("用户不存在"));
            
            UserBehaviorDetail detail = new UserBehaviorDetail();
            detail.setUserId(user.getId());
            detail.setUsername(user.getUsername());
            detail.setTotalLogins(1L); // 简化实现
            detail.setLastLoginTime(user.getLastLoginTime() != null ? 
                user.getLastLoginTime().toString() : null);
            
            return detail;
        } catch (Exception e) {
            log.error("获取用户行为统计失败: 用户ID={}", userId, e);
            throw new BusinessException("获取用户行为统计失败: " + e.getMessage());
        }
    }
    
    @Override
    public Page<UserRanking> getMostActiveUsers(int page, int size) {
        try {
            List<User> users = userRepository.findAll();
            
            List<UserRanking> rankings = users.stream()
                .map(user -> {
                    UserRanking ranking = new UserRanking();
                    ranking.setUserId(user.getId());
                    ranking.setUsername(user.getUsername());
                    ranking.setActivityScore(calculateUserActivityScore(user));
                    return ranking;
                })
                .sorted(Comparator.comparing(UserRanking::getActivityScore).reversed())
                .skip(page * size)
                .limit(size)
                .collect(Collectors.toList());
            
            return new PageImpl<>(rankings, PageRequest.of(page, size), users.size());
        } catch (Exception e) {
            log.error("获取最活跃用户排行失败", e);
            throw new BusinessException("获取最活跃用户排行失败: " + e.getMessage());
        }
    }
    
    @Override
    public Page<UserRanking> getTopContributors(int page, int size) {
        try {
            List<User> users = userRepository.findAll();
            
            List<UserRanking> rankings = users.stream()
                .map(user -> {
                    UserRanking ranking = new UserRanking();
                    ranking.setUserId(user.getId());
                    ranking.setUsername(user.getUsername());
                    ranking.setContributionScore(user.getUsedStorage()); // 使用存储使用量作为贡献度指标
                    return ranking;
                })
                .sorted(Comparator.comparing(UserRanking::getContributionScore).reversed())
                .skip(page * size)
                .limit(size)
                .collect(Collectors.toList());
            
            return new PageImpl<>(rankings, PageRequest.of(page, size), users.size());
        } catch (Exception e) {
            log.error("获取高贡献用户排行失败", e);
            throw new BusinessException("获取高贡献用户排行失败: " + e.getMessage());
        }
    }
    
    @Override
    public Page<UserRanking> getStorageHeavyUsers(int page, int size) {
        try {
            List<User> users = userRepository.findAll();
            
            List<UserRanking> rankings = users.stream()
                .filter(user -> user.getUsedStorage() > 0)
                .map(user -> {
                    UserRanking ranking = new UserRanking();
                    ranking.setUserId(user.getId());
                    ranking.setUsername(user.getUsername());
                    ranking.setStorageUsed(user.getUsedStorage());
                    ranking.setStorageQuota(user.getStorageQuota());
                    return ranking;
                })
                .sorted(Comparator.comparing(UserRanking::getStorageUsed).reversed())
                .skip(page * size)
                .limit(size)
                .collect(Collectors.toList());
            
            return new PageImpl<>(rankings, PageRequest.of(page, size), 
                users.stream().filter(u -> u.getUsedStorage() > 0).count());
        } catch (Exception e) {
            log.error("获取存储大户排行失败", e);
            throw new BusinessException("获取存储大户排行失败: " + e.getMessage());
        }
    }
    
    @Override
    public List<UserLevelDistribution> getUserLevelDistribution() {
        try {
            List<User> users = userRepository.findAll();
            
            Map<String, Long> levelDistribution = users.stream()
                .collect(Collectors.groupingBy(
                    user -> getUserLevel(user),
                    Collectors.counting()));
            
            return levelDistribution.entrySet().stream()
                .map(entry -> {
                    UserLevelDistribution distribution = new UserLevelDistribution();
                    distribution.setLevel(entry.getKey());
                    distribution.setUserCount(entry.getValue());
                    return distribution;
                })
                .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("获取用户等级分布失败", e);
            throw new BusinessException("获取用户等级分布失败: " + e.getMessage());
        }
    }
    
    @Override
    public List<DailyTrend> getUserGrowthTrend(int days) {
        try {
            LocalDateTime sinceTime = LocalDateTime.now().minusDays(days);
            List<User> recentUsers = userRepository.findAll().stream()
                .filter(user -> user.getCreatedAt().isAfter(sinceTime))
                .collect(Collectors.toList());
            
            Map<LocalDate, Long> dailyCounts = recentUsers.stream()
                .collect(Collectors.groupingBy(
                    user -> user.getCreatedAt().toLocalDate(),
                    Collectors.counting()));
            
            return dailyCounts.entrySet().stream()
                .map(entry -> {
                    DailyTrend trend = new DailyTrend();
                    trend.setDate(entry.getKey().toString());
                    trend.setValue(entry.getValue());
                    return trend;
                })
                .sorted(Comparator.comparing(t -> LocalDate.parse(t.getDate())))
                .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("获取用户增长趋势失败", e);
            throw new BusinessException("获取用户增长趋势失败: " + e.getMessage());
        }
    }
    
    @Override
    public List<DailyTrend> getUserActivityTrend(int days) {
        try {
            // 简化实现，返回用户登录趋势
            return getUserGrowthTrend(days);
        } catch (Exception e) {
            log.error("获取用户活跃度趋势失败", e);
            throw new BusinessException("获取用户活跃度趋势失败: " + e.getMessage());
        }
    }
    
    @Override
    public void collectSystemStatistics() {
        try {
            log.info("开始收集系统统计信息");
            // 这里可以实现定期收集系统统计的逻辑
        } catch (Exception e) {
            log.error("收集系统统计失败", e);
        }
    }
    
    @Override
    public SystemOverview getSystemOverview() {
        try {
            SystemOverview overview = new SystemOverview();
            overview.setTotalUsers((long) userRepository.findAll().size());
            overview.setTotalFiles((long) fileRepository.findAll().size());
            overview.setTotalFolders((long) folderRepository.findAll().size());
            overview.setTotalShares((long) shareRepository.findAll().size());
            overview.setTotalStorageUsed(fileRepository.findAll().stream()
                .mapToLong(FileEntity::getFileSize)
                .sum());
            
            return overview;
        } catch (Exception e) {
            log.error("获取系统概览统计失败", e);
            throw new BusinessException("获取系统概览统计失败: " + e.getMessage());
        }
    }
    
    @Override
    public List<SystemLoadTrend> getSystemLoadTrend(int days) {
        try {
            // 简化实现，返回存储使用趋势
            List<DailyTrend> storageTrends = getUserGrowthTrend(days);
            
            return storageTrends.stream()
                .map(trend -> {
                    SystemLoadTrend loadTrend = new SystemLoadTrend();
                    loadTrend.setDate(trend.getDate());
                    loadTrend.setCpuUsage(0.0); // 简化实现
                    loadTrend.setMemoryUsage(0.0); // 简化实现
                    loadTrend.setStorageUsage(trend.getValue().doubleValue());
                    return loadTrend;
                })
                .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("获取系统负载趋势失败", e);
            throw new BusinessException("获取系统负载趋势失败: " + e.getMessage());
        }
    }
    
    @Override
    public SystemHealthReport getSystemHealthReport(int days) {
        try {
            SystemHealthReport report = new SystemHealthReport();
            report.setReportDate(LocalDate.now().toString());
            report.setSystemStatus("HEALTHY"); // 简化实现
            report.setTotalUsers((long) userRepository.findAll().size());
            report.setTotalFiles((long) fileRepository.findAll().size());
            
            return report;
        } catch (Exception e) {
            log.error("获取系统健康报告失败", e);
            throw new BusinessException("获取系统健康报告失败: " + e.getMessage());
        }
    }
    
    @Override
    public List<PerformanceTrend> getPerformanceTrend(int days) {
        try {
            // 简化实现，返回响应时间趋势
            List<DailyTrend> trends = getUserGrowthTrend(days);
            
            return trends.stream()
                .map(trend -> {
                    PerformanceTrend perfTrend = new PerformanceTrend();
                    perfTrend.setDate(trend.getDate());
                    perfTrend.setAverageResponseTime(100.0); // 简化实现
                    perfTrend.setErrorRate(0.0); // 简化实现
                    return perfTrend;
                })
                .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("获取性能趋势失败", e);
            throw new BusinessException("获取性能趋势失败: " + e.getMessage());
        }
    }
    
    @Override
    public Page<SystemAlert> getAbnormalStatistics(int page, int size) {
        try {
            // 简化实现，返回一些示例告警
            List<SystemAlert> alerts = Arrays.asList(
                createSystemAlert("STORAGE_WARNING", "存储空间不足警告", "当前存储使用率达到85%"),
                createSystemAlert("PERFORMANCE_DEGRADATION", "性能下降", "响应时间超过正常水平")
            );
            
            return new PageImpl<>(alerts, PageRequest.of(page, size), alerts.size());
        } catch (Exception e) {
            log.error("获取异常统计失败", e);
            throw new BusinessException("获取异常统计失败: " + e.getMessage());
        }
    }
    
    // ==================== 私有辅助方法 ====================
    
    private long calculateUserActivityScore(User user) {
        // 简化的活跃度计算
        return user.getUsedStorage() / (1024 * 1024); // 以MB为单位计算
    }
    
    private String getUserLevel(User user) {
        long storageMB = user.getUsedStorage() / (1024 * 1024);
        if (storageMB < 100) return "初级用户";
        if (storageMB < 1000) return "中级用户";
        if (storageMB < 10000) return "高级用户";
        return "VIP用户";
    }
    
    private SystemAlert createSystemAlert(String type, String title, String message) {
        SystemAlert alert = new SystemAlert();
        alert.setAlertType(type);
        alert.setAlertTitle(title);
        alert.setAlertMessage(message);
        alert.setAlertTime(LocalDateTime.now().toString());
        alert.setSeverity("WARNING");
        return alert;
    }
}