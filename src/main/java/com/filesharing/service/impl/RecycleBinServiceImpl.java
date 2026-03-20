package com.filesharing.service.impl;

import com.filesharing.entity.RecycleBin;
import com.filesharing.entity.FileEntity;
import com.filesharing.entity.Folder;
import com.filesharing.entity.User;
import com.filesharing.exception.BusinessException;
import com.filesharing.repository.FileRepository;
import com.filesharing.repository.FolderRepository;
import com.filesharing.repository.RecycleBinRepository;
import com.filesharing.service.RecycleBinService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * 回收站服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class RecycleBinServiceImpl implements RecycleBinService {
    
    private static final int DEFAULT_EXPIRE_DAYS = 30;

    private final RecycleBinRepository recycleBinRepository;
    private final FileRepository fileRepository;
    private final FolderRepository folderRepository;
    
    @Override
    public void moveToRecycleBin(Long fileId, User user, String deleteReason) {
        FileEntity file = fileRepository.findById(fileId)
                .orElseThrow(() -> new BusinessException("文件不存在"));

        if (file.getUploader() == null || !Objects.equals(file.getUploader().getId(), user.getId())) {
            throw new BusinessException("无权操作该文件");
        }

        LocalDateTime now = LocalDateTime.now();
        RecycleBin record = new RecycleBin();
        record.setItemId(file.getId());
        record.setItemType(RecycleBin.ItemType.FILE);
        record.setOriginalName(file.getOriginalName());
        record.setOriginalPath(file.getFilePath());
        record.setOriginalParentId(file.getFolder() != null ? file.getFolder().getId() : null);
        record.setFileSize(file.getFileSize());
        record.setFileType(file.getExtension());
        record.setDeletedBy(user);
        record.setDeletedAt(now);
        record.setExpireAt(now.plusDays(DEFAULT_EXPIRE_DAYS));
        record.setIsRecoverable(true);
        record.setDeleteReason(deleteReason);

        recycleBinRepository.save(record);

        file.setStatus(FileEntity.FileStatus.DELETED);
        file.setDeletedAt(now);
        fileRepository.save(file);

        log.info("文件移动到回收站：文件 ID={}, 删除者={}, 原因={}", fileId, user.getUsername(), deleteReason);
    }
        
    @Override
    public void moveFolderToRecycleBin(Long folderId, User user, String deleteReason) {
        Folder folder = folderRepository.findById(folderId)
                .orElseThrow(() -> new BusinessException("文件夹不存在"));

        if (folder.getOwner() == null || !Objects.equals(folder.getOwner().getId(), user.getId())) {
            throw new BusinessException("无权操作该文件夹");
        }

        LocalDateTime now = LocalDateTime.now();
        RecycleBin record = new RecycleBin();
        record.setItemId(folder.getId());
        record.setItemType(RecycleBin.ItemType.FOLDER);
        record.setOriginalName(folder.getName());
        record.setOriginalPath(folder.getFolderPath());
        record.setOriginalParentId(folder.getParent() != null ? folder.getParent().getId() : null);
        record.setFileSize(0L);
        record.setFileType("folder");
        record.setDeletedBy(user);
        record.setDeletedAt(now);
        record.setExpireAt(now.plusDays(DEFAULT_EXPIRE_DAYS));
        record.setIsRecoverable(true);
        record.setDeleteReason(deleteReason);

        recycleBinRepository.save(record);
        log.info("文件夹移动到回收站：文件夹 ID={}, 删除者={}, 原因={}", folderId, user.getUsername(), deleteReason);
    }
    
    @Override
    @Transactional(readOnly = true)
    public Page<RecycleBinItem> getUserRecycleBin(User user, String itemType, int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.max(size, 1);
        Pageable pageable = PageRequest.of(safePage, safeSize);

        Page<RecycleBin> records;
        if (itemType == null || itemType.isBlank()) {
            records = recycleBinRepository.findByDeletedByOrderByDeletedAtDesc(user, pageable);
        } else {
            RecycleBin.ItemType type = RecycleBin.ItemType.valueOf(itemType.trim().toUpperCase(Locale.ROOT));
            records = recycleBinRepository.findByDeletedByAndItemType(user, type, pageable);
        }

        List<RecycleBinItem> items = records.getContent().stream()
                .map(this::toRecycleBinItem)
                .toList();

        return new PageImpl<>(items, pageable, records.getTotalElements());
    }
    
    @Override
    @Transactional(readOnly = true)
    public Page<RecycleBinItem> searchRecycleBin(User user, String keyword, int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.max(size, 1);
        Pageable pageable = PageRequest.of(safePage, safeSize);

        Page<RecycleBin> records = recycleBinRepository.searchByKeyword(user,
            keyword == null ? "" : keyword.trim(), pageable);

        List<RecycleBinItem> items = records.getContent().stream()
            .map(this::toRecycleBinItem)
            .toList();

        return new PageImpl<>(items, pageable, records.getTotalElements());
    }
    

    
    @Override
    public RestoreResult restoreItem(Long recycleBinId, User user) {
        RecycleBin record = getOwnedRecycleRecord(recycleBinId, user);

        if (!Boolean.TRUE.equals(record.getIsRecoverable())) {
            return new RestoreResult(false, "该项目不可恢复", recycleBinId,
                    record.getItemType().name(), record.getOriginalPath());
        }

        if (record.getItemType() == RecycleBin.ItemType.FILE) {
            fileRepository.findById(record.getItemId()).ifPresent(file -> {
                file.setStatus(FileEntity.FileStatus.AVAILABLE);
                file.setDeletedAt(null);
                if (record.getOriginalParentId() != null) {
                    folderRepository.findById(record.getOriginalParentId()).ifPresent(file::setFolder);
                }
                fileRepository.save(file);
            });
        } else {
            folderRepository.findById(record.getItemId()).ifPresent(folder -> {
                if (record.getOriginalParentId() == null) {
                    folder.setParent(null);
                } else {
                    folderRepository.findById(record.getOriginalParentId()).ifPresent(folder::setParent);
                }
                folderRepository.save(folder);
            });
        }

        recycleBinRepository.delete(record);
        log.info("恢复回收站项目：ID={}, 用户={}", recycleBinId, user.getUsername());
        return new RestoreResult(true, "恢复成功", record.getItemId(), record.getItemType().name(), record.getOriginalPath());
    }
        
    @Override
    public RestoreResult restoreToLocation(Long recycleBinId, Long targetFolderId, User user) {
        RecycleBin record = getOwnedRecycleRecord(recycleBinId, user);
        Folder targetFolder = folderRepository.findById(targetFolderId)
                .orElseThrow(() -> new BusinessException("目标文件夹不存在"));

        if (targetFolder.getOwner() == null || !Objects.equals(targetFolder.getOwner().getId(), user.getId())) {
            throw new BusinessException("无权恢复到该目标文件夹");
        }

        if (record.getItemType() == RecycleBin.ItemType.FILE) {
            FileEntity file = fileRepository.findById(record.getItemId())
                    .orElseThrow(() -> new BusinessException("源文件不存在"));
            file.setStatus(FileEntity.FileStatus.AVAILABLE);
            file.setDeletedAt(null);
            file.setFolder(targetFolder);
            fileRepository.save(file);
        } else {
            Folder folder = folderRepository.findById(record.getItemId())
                    .orElseThrow(() -> new BusinessException("源文件夹不存在"));
            folder.setParent(targetFolder);
            folderRepository.save(folder);
        }

        recycleBinRepository.delete(record);
        String restorePath = targetFolder.getFolderPath() == null ? targetFolder.getName() : targetFolder.getFolderPath();
        log.info("恢复到指定位置：回收站 ID={}, 目标文件夹 ID={}, 用户={}", recycleBinId, targetFolderId, user.getUsername());
        return new RestoreResult(true, "恢复成功", record.getItemId(), record.getItemType().name(), restorePath);
    }
    
    @Override
    public void permanentlyDelete(Long recycleBinId, User user) {
        RecycleBin record = getOwnedRecycleRecord(recycleBinId, user);
        permanentlyDeleteRecord(record);
        log.info("永久删除回收站项目：ID={}, 用户={}", recycleBinId, user.getUsername());
    }
    
    @Override
    @Transactional(readOnly = true)
    public RecycleBinStats getRecycleBinStats(User user) {
        LocalDateTime now = LocalDateTime.now();
        Object[] row = normalizeStatsRow(recycleBinRepository.getRecycleBinStats(user, now));

        long totalItems = safeLong(row != null && row.length > 0 ? row[0] : 0);
        long fileCount = safeLong(row != null && row.length > 1 ? row[1] : 0);
        long folderCount = safeLong(row != null && row.length > 2 ? row[2] : 0);
        long expiredCount = safeLong(row != null && row.length > 3 ? row[3] : 0);

        List<RecycleBin> sample = recycleBinRepository
            .findByDeletedByOrderByDeletedAtDesc(user, PageRequest.of(0, 1000))
            .getContent();

        RecycleBinStats stats = new RecycleBinStats();
        stats.setTotalItems(totalItems);
        stats.setFileCount(fileCount);
        stats.setFolderCount(folderCount);
        stats.setExpiredCount(expiredCount);
        stats.setRecoverableCount(Math.max(totalItems - expiredCount, 0));

        sample.stream()
            .map(RecycleBin::getDeletedAt)
            .filter(Objects::nonNull)
            .min(LocalDateTime::compareTo)
            .ifPresent(t -> stats.setOldestItemDate(t.toString()));

        sample.stream()
            .map(RecycleBin::getDeletedAt)
            .filter(Objects::nonNull)
            .max(LocalDateTime::compareTo)
            .ifPresent(t -> stats.setNewestItemDate(t.toString()));

        return stats;
    }

    private Object[] normalizeStatsRow(Object[] row) {
        if (row == null) {
            return null;
        }
        if (row.length == 1 && row[0] instanceof Object[]) {
            return (Object[]) row[0];
        }
        return row;
    }
    
    @Override
    public List<ExpiringItem> getExpiringItemsReminder(User user, int hours) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime threshold = now.plusHours(hours);
        List<RecycleBin> soon = recycleBinRepository.findItemsExpiringSoon(now, threshold);

        List<ExpiringItem> result = new ArrayList<>();
        for (RecycleBin record : soon) {
            if (record.getDeletedBy() == null || !Objects.equals(record.getDeletedBy().getId(), user.getId())) {
                continue;
            }

            long remainHours = record.getExpireAt() == null ? 0 : Math.max(Duration.between(now, record.getExpireAt()).toHours(), 0);
            result.add(new ExpiringItem(
                    record.getId(),
                    record.getOriginalName(),
                    record.getItemType().name(),
                    record.getExpireAt() == null ? null : record.getExpireAt().toString(),
                    remainHours
            ));
        }

        log.info("获取即将过期项目：用户={}, 时间范围={}小时, 命中={}项", user.getUsername(), hours, result.size());
        return result;
    }
    
    @Override
    public BatchOperationResult batchRestore(List<Long> recycleBinIds, User user) {
        if (recycleBinIds == null) {
            return new BatchOperationResult(0, 0, 0);
        }

        int success = 0;
        int failure = 0;
        BatchOperationResult result = new BatchOperationResult(recycleBinIds.size(), 0, 0);

        for (Long id : recycleBinIds) {
            try {
                RestoreResult restoreResult = restoreItem(id, user);
                if (Boolean.TRUE.equals(restoreResult.getSuccess())) {
                    success++;
                } else {
                    failure++;
                    result.addErrorMessage("ID " + id + ": " + restoreResult.getMessage());
                }
            } catch (Exception e) {
                failure++;
                result.addErrorMessage("ID " + id + ": " + e.getMessage());
            }
        }

        result.setSuccessCount(success);
        result.setFailureCount(failure);
        log.info("批量恢复：数量={}, 用户={}", recycleBinIds.size(), user.getUsername());
        return result;
    }
    
    @Override
    public BatchOperationResult batchPermanentlyDelete(List<Long> recycleBinIds, User user) {
        if (recycleBinIds == null) {
            return new BatchOperationResult(0, 0, 0);
        }

        int success = 0;
        int failure = 0;
        BatchOperationResult result = new BatchOperationResult(recycleBinIds.size(), 0, 0);

        for (Long id : recycleBinIds) {
            try {
                permanentlyDelete(id, user);
                success++;
            } catch (Exception e) {
                failure++;
                result.addErrorMessage("ID " + id + ": " + e.getMessage());
            }
        }

        result.setSuccessCount(success);
        result.setFailureCount(failure);
        log.info("批量永久删除：数量={}, 用户={}", recycleBinIds.size(), user.getUsername());
        return result;
    }
    
    @Override
    public void cleanupExpiredItems() {
        LocalDateTime now = LocalDateTime.now();
        List<RecycleBin> expiredItems = recycleBinRepository.findExpiredItems(now);
        for (RecycleBin record : expiredItems) {
            permanentlyDeleteRecord(record);
        }
        log.info("清理过期回收站项目：当前时间={}", now);
    }
    
    @Override
    public void emptyRecycleBin(User user) {
        List<RecycleBin> records = recycleBinRepository
                .findByDeletedByOrderByDeletedAtDesc(user, PageRequest.of(0, 1000))
                .getContent();
        for (RecycleBin record : records) {
            permanentlyDeleteRecord(record);
        }
        log.info("清空回收站：用户={}", user.getUsername());
    }
    

    private RecycleBin getOwnedRecycleRecord(Long recycleBinId, User user) {
        RecycleBin record = recycleBinRepository.findById(recycleBinId)
                .orElseThrow(() -> new BusinessException("回收站记录不存在"));
        if (record.getDeletedBy() == null || !Objects.equals(record.getDeletedBy().getId(), user.getId())) {
            throw new BusinessException("无权操作该回收站记录");
        }
        return record;
    }

    private void permanentlyDeleteRecord(RecycleBin record) {
        if (record.getItemType() == RecycleBin.ItemType.FILE) {
            fileRepository.findById(record.getItemId()).ifPresent(fileRepository::delete);
        } else if (record.getItemType() == RecycleBin.ItemType.FOLDER) {
            folderRepository.findById(record.getItemId()).ifPresent(folderRepository::delete);
        }
        recycleBinRepository.delete(record);
    }

    private RecycleBinItem toRecycleBinItem(RecycleBin record) {
        LocalDateTime now = LocalDateTime.now();
        boolean recoverable = Boolean.TRUE.equals(record.getIsRecoverable())
                && (record.getExpireAt() == null || record.getExpireAt().isAfter(now));

        return new RecycleBinItem(
                record.getId(),
                record.getItemId(),
                record.getItemType() == null ? null : record.getItemType().name(),
                record.getOriginalName(),
                record.getOriginalPath(),
                record.getFileSize(),
                record.getFileType(),
                record.getDeletedBy() == null ? null : record.getDeletedBy().getUsername(),
                record.getDeletedAt() == null ? null : record.getDeletedAt().toString(),
                record.getExpireAt() == null ? null : record.getExpireAt().toString(),
                recoverable,
                record.getDeleteReason()
        );
    }

    private long safeLong(Object value) {
        if (value == null) {
            return 0L;
        }
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        return Long.parseLong(value.toString());
    }

}