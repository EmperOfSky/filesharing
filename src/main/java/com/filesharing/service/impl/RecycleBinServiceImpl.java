package com.filesharing.service.impl;

import com.filesharing.entity.RecycleBin;
import com.filesharing.entity.User;
import com.filesharing.exception.BusinessException;
import com.filesharing.repository.RecycleBinRepository;
import com.filesharing.service.RecycleBinService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 回收站服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class RecycleBinServiceImpl implements RecycleBinService {
    
    private final RecycleBinRepository recycleBinRepository;
    
    @Override
    public void moveToRecycleBin(Long itemId, ItemType itemType, String itemName, 
                               String itemPath, Long fileSize, String fileType, 
                               Long parentId, User deletedBy, String deleteReason) {
        RecycleBin recycleItem = new RecycleBin();
        recycleItem.setItemId(itemId);
        recycleItem.setItemType(itemType);
        recycleItem.setOriginalName(itemName);
        recycleItem.setOriginalPath(itemPath);
        recycleItem.setFileSize(fileSize);
        recycleItem.setFileType(fileType);
        recycleItem.setOriginalParentId(parentId);
        recycleItem.setDeletedBy(deletedBy);
        recycleItem.setDeletedAt(LocalDateTime.now());
        recycleItem.setExpireAt(LocalDateTime.now().plusDays(30)); // 30天后过期
        recycleItem.setDeleteReason(deleteReason != null ? deleteReason : "用户删除");
        
        recycleBinRepository.save(recycleItem);
        log.info("移动到回收站: 项目ID={}, 类型={}, 删除者={}", itemId, itemType, deletedBy.getUsername());
    }
    
    @Override
    @Transactional(readOnly = true)
    public Page<RecycleItemInfo> getUserRecycleBin(User user, Pageable pageable) {
        return recycleBinRepository.findByDeletedByOrderByDeletedAtDesc(user, pageable)
                .map(this::convertToRecycleItemInfo);
    }
    
    @Override
    @Transactional(readOnly = true)
    public RecycleItemInfo getRecycleItemById(Long recycleId, User user) {
        RecycleBin item = recycleBinRepository.findById(recycleId)
                .orElseThrow(() -> new BusinessException("回收站项目不存在"));
        
        if (!item.getDeletedBy().getId().equals(user.getId())) {
            throw new BusinessException("无权限访问此回收站项目");
        }
        
        return convertToRecycleItemInfo(item);
    }
    
    @Override
    public void restoreFromRecycleBin(Long recycleId, User user) {
        RecycleBin item = getRecycleItemById(recycleId, user);
        
        // 这里应该实现具体的恢复逻辑
        // 比如恢复文件或文件夹到原来的位置
        
        recycleBinRepository.deleteById(recycleId);
        log.info("从回收站恢复: 项目ID={}, 用户={}", recycleId, user.getUsername());
    }
    
    @Override
    public void permanentlyDelete(Long recycleId, User user) {
        RecycleBin item = getRecycleItemById(recycleId, user);
        
        // 这里应该实现永久删除逻辑
        // 比如删除实际的文件数据
        
        recycleBinRepository.deleteById(recycleId);
        log.info("永久删除回收站项目: 项目ID={}, 用户={}", recycleId, user.getUsername());
    }
    
    @Override
    @Transactional(readOnly = true)
    public RecycleBinStats getRecycleBinStats(User user) {
        LocalDateTime now = LocalDateTime.now();
        Object[] stats = recycleBinRepository.getRecycleBinStats(user, now);
        
        RecycleBinStats recycleStats = new RecycleBinStats();
        recycleStats.setTotalItems(((Number) stats[0]).longValue());
        recycleStats.setTotalSize(((Number) stats[1]).longValue());
        recycleStats.setExpiredItems(((Number) stats[2]).longValue());
        
        return recycleStats;
    }
    
    @Override
    public void clearExpiredItems() {
        LocalDateTime now = LocalDateTime.now();
        List<RecycleBin> expiredItems = recycleBinRepository.findExpiredItems(now);
        
        long count = expiredItems.size();
        recycleBinRepository.deleteAll(expiredItems);
        
        log.info("清理过期回收站项目: 数量={}", count);
    }
    
    @Override
    public void emptyRecycleBin(User user) {
        List<RecycleBin> userItems = recycleBinRepository.findByDeletedBy(user);
        recycleBinRepository.deleteAll(userItems);
        log.info("清空回收站: 用户={}, 项目数={}", user.getUsername(), userItems.size());
    }
    
    // ==================== 私有方法 ====================
    
    private RecycleBin getRecycleItemById(Long recycleId) {
        return recycleBinRepository.findById(recycleId)
                .orElseThrow(() -> new BusinessException("回收站项目不存在"));
    }
    
    private RecycleItemInfo convertToRecycleItemInfo(RecycleBin item) {
        RecycleItemInfo info = new RecycleItemInfo();
        info.setId(item.getId());
        info.setItemId(item.getItemId());
        info.setItemType(item.getItemType().name());
        info.setOriginalName(item.getOriginalName());
        info.setOriginalPath(item.getOriginalPath());
        info.setFileSize(item.getFileSize());
        info.setFileType(item.getFileType());
        info.setDeletedByName(item.getDeletedBy().getUsername());
        info.setDeletedAt(item.getDeletedAt());
        info.setExpireAt(item.getExpireAt());
        info.setIsRecoverable(item.getExpireAt().isAfter(LocalDateTime.now()));
        info.setDeleteReason(item.getDeleteReason());
        return info;
    }
}