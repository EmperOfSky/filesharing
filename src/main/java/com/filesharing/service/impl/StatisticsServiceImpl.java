package com.filesharing.service.impl;

import com.filesharing.entity.User;
import com.filesharing.exception.BusinessException;
import com.filesharing.repository.*;
import com.filesharing.service.StatisticsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 统计服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StatisticsServiceImpl implements StatisticsService {
    
    private final UserRepository userRepository;
    private final FileRepository fileRepository;
    private final FolderRepository folderRepository;
    private final ShareRepository shareRepository;
    private final UserBehaviorStatisticsRepository userBehaviorStatisticsRepository;
    
    @Override
    public UserStatistics getUserStatistics(User user) {
        UserStatistics stats = new UserStatistics();
        stats.setUserId(user.getId());
        stats.setTotalFiles(fileRepository.countByUploader(user));
        stats.setTotalFolders(folderRepository.countByOwner(user));
        stats.setTotalShares(shareRepository.countBySharer(user));
        stats.setUsedStorage(user.getUsedStorage());
        stats.setStorageQuota(user.getStorageQuota());
        stats.setStorageUsagePercentage(
            user.getStorageQuota() > 0 ? 
            (double) user.getUsedStorage() / user.getStorageQuota() * 100 : 0.0
        );
        return stats;
    }
    
    @Override
    public SystemStatistics getSystemStatistics() {
        SystemStatistics stats = new SystemStatistics();
        stats.setTotalUsers(userRepository.countActiveUsers());
        stats.setTotalFiles(fileRepository.count());
        stats.setTotalFolders(folderRepository.count());
        stats.setTotalShares(shareRepository.count());
        stats.setTotalStorageUsed(fileRepository.sumFileSize());
        return stats;
    }
    
    @Override
    public Map<String, Object> getStorageStatistics(User user) {
        Map<String, Object> stats = new HashMap<>();
        stats.put("usedStorage", user.getUsedStorage());
        stats.put("storageQuota", user.getStorageQuota());
        stats.put("availableStorage", Math.max(0, user.getStorageQuota() - user.getUsedStorage()));
        stats.put("usagePercentage", 
            user.getStorageQuota() > 0 ? 
            (double) user.getUsedStorage() / user.getStorageQuota() * 100 : 0.0);
        return stats;
    }
    
    @Override
    public List<FileTypeStatistic> getFileTypeStatistics(User user) {
        return fileRepository.getFileTypeStatistics(user);
    }
    
    @Override
    public Map<String, Object> getRecentActivity(User user, int days) {
        LocalDateTime since = LocalDateTime.now().minusDays(days);
        Map<String, Object> activity = new HashMap<>();
        
        // 简化实现
        activity.put("recentUploads", fileRepository.countByUploaderAndCreatedAtAfter(user, since));
        activity.put("recentDownloads", 0L); // 简化实现
        activity.put("recentShares", shareRepository.countBySharerAndCreatedAtAfter(user, since));
        
        return activity;
    }
}