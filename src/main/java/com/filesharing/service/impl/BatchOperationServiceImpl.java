package com.filesharing.service.impl;

import com.filesharing.entity.BatchOperation;
import com.filesharing.entity.FileEntity;
import com.filesharing.entity.Folder;
import com.filesharing.entity.User;
import com.filesharing.exception.BusinessException;
import com.filesharing.repository.BatchOperationRepository;
import com.filesharing.service.BatchOperationService;
import com.filesharing.service.FileService;
import com.filesharing.service.FolderService;
import com.filesharing.service.UserService;
import com.filesharing.util.FileStorageUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * 批量操作服务实现类
 */
@Service
@RequiredArgsConstructor
@Transactional
public class BatchOperationServiceImpl implements BatchOperationService {
    
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(BatchOperationServiceImpl.class);
    
    private final BatchOperationRepository batchOperationRepository;
    private final FileService fileService;
    private final FolderService folderService;
    private final UserService userService;
    private final FileStorageUtil fileStorageUtil;
    
    public BatchOperation createBatchOperation(BatchOperation operation) {
        operation.setStartedAt(LocalDateTime.now());
        operation.setStatus(BatchOperation.OperationStatus.PENDING);
        operation.setProgressPercentage(0.0);
        
        BatchOperation savedOperation = batchOperationRepository.save(operation);
        log.info("创建批量操作: ID={}, 类型={}", savedOperation.getId(), savedOperation.getOperationType());
        
        return savedOperation;
    }
    
    @Transactional(readOnly = true)
    public BatchOperation getBatchOperationById(Long operationId) {
        return batchOperationRepository.findById(operationId)
                .orElseThrow(() -> new BusinessException("批量操作不存在"));
    }
    
    @Transactional(readOnly = true)
    public Page<BatchOperation> getUserBatchOperations(User user, Pageable pageable) {
        return batchOperationRepository.findByUserOrderByCreatedAtDesc(user, pageable);
    }
    
    @Transactional(readOnly = true)
    public Page<BatchOperation> getUserBatchOperationsByType(User user, 
                                                           BatchOperation.OperationType operationType, 
                                                           Pageable pageable) {
        return batchOperationRepository.findByUserAndOperationType(user, operationType, pageable);
    }
    
    @Transactional(readOnly = true)
    public Page<BatchOperation> getUserBatchOperationsByStatus(User user, 
                                                              BatchOperation.OperationStatus status, 
                                                              Pageable pageable) {
        return batchOperationRepository.findByUserAndStatus(user, status, pageable);
    }
    
    @Async
    public CompletableFuture<Void> executeBatchOperation(Long operationId) {
        try {
            BatchOperation operation = getBatchOperationById(operationId);
            
            if (operation.getStatus() != BatchOperation.OperationStatus.PENDING) {
                throw new BusinessException("操作已在执行中或已完成");
            }
            
            // 更新状态为处理中
            operation.setStatus(BatchOperation.OperationStatus.PROCESSING);
            operation.setStartedAt(LocalDateTime.now());
            batchOperationRepository.save(operation);
            
            // 根据操作类型执行不同的批量操作
            switch (operation.getOperationType()) {
                case UPLOAD:
                    executeBatchUpload(operation);
                    break;
                case DELETE:
                    executeBatchDelete(operation);
                    break;
                case MOVE:
                    executeBatchMove(operation);
                    break;
                case COPY:
                    executeBatchCopy(operation);
                    break;
                case RENAME:
                    executeBatchRename(operation);
                    break;
                default:
                    throw new BusinessException("不支持的操作类型");
            }
            
            // 更新完成状态
            operation.setStatus(BatchOperation.OperationStatus.COMPLETED);
            operation.setCompletedAt(LocalDateTime.now());
            operation.setProgressPercentage(100.0);
            batchOperationRepository.save(operation);
            
            log.info("批量操作执行完成: ID={}", operationId);
            
        } catch (Exception e) {
            // 更新失败状态
            BatchOperation operation = getBatchOperationById(operationId);
            operation.setStatus(BatchOperation.OperationStatus.FAILED);
            operation.setCompletedAt(LocalDateTime.now());
            operation.setErrorDetails(e.getMessage());
            batchOperationRepository.save(operation);
            
            log.error("批量操作执行失败: ID={}, 错误={}", operationId, e.getMessage());
        }
        
        return CompletableFuture.completedFuture(null);
    }
    
    public void cancelBatchOperation(Long operationId, User currentUser) {
        BatchOperation operation = getBatchOperationById(operationId);
        
        if (!operation.getUser().getId().equals(currentUser.getId())) {
            throw new BusinessException("无权限取消此操作");
        }
        
        if (!operation.getIsCancellable() || 
            operation.getStatus() == BatchOperation.OperationStatus.COMPLETED ||
            operation.getStatus() == BatchOperation.OperationStatus.FAILED) {
            throw new BusinessException("操作无法取消");
        }
        
        operation.setStatus(BatchOperation.OperationStatus.CANCELLED);
        operation.setCompletedAt(LocalDateTime.now());
        batchOperationRepository.save(operation);
        
        log.info("批量操作已取消: ID={}", operationId);
    }
    
    @Transactional(readOnly = true)
    public List<BatchOperation> getProcessingOperations() {
        return batchOperationRepository.findProcessingOperations();
    }
    
    @Transactional(readOnly = true)
    public Page<BatchOperation> getCancellableOperations(Pageable pageable) {
        return batchOperationRepository.findCancellableOperations(pageable);
    }
    
    public void updateProgress(Long operationId, Double progress) {
        BatchOperation operation = getBatchOperationById(operationId);
        operation.setProgressPercentage(progress);
        batchOperationRepository.save(operation);
    }
    
    public void updateEstimatedCompletion(Long operationId, LocalDateTime estimatedTime) {
        BatchOperation operation = getBatchOperationById(operationId);
        operation.setEstimatedCompletion(estimatedTime);
        batchOperationRepository.save(operation);
    }
    
    @Override
    public void retryFailedOperation(Long operationId, User user) {
        try {
            BatchOperation operation = getBatchOperationById(operationId);
            
            // 验证用户权限
            if (!operation.getUser().getId().equals(user.getId())) {
                throw new BusinessException("无权限重试此操作");
            }
            
            // 检查操作状态
            if (operation.getStatus() != BatchOperation.OperationStatus.FAILED) {
                throw new BusinessException("只有失败的操作才能重试");
            }
            
            // 重置操作状态
            operation.setStatus(BatchOperation.OperationStatus.PENDING);
            operation.setStartedAt(null);
            operation.setCompletedAt(null);
            operation.setProgressPercentage(0.0);
            operation.setProcessedItems(0);
            operation.setSuccessItems(0);
            operation.setFailedItems(0);
            operation.setErrorDetails(null);
            
            batchOperationRepository.save(operation);
            
            // 重新执行操作
            executeBatchOperation(operationId);
            
            log.info("操作重试已启动: 操作ID={}, 用户ID={}", operationId, user.getId());
            
        } catch (Exception e) {
            log.error("重试操作失败: 操作ID={}, 用户ID={}", operationId, user.getId(), e);
            throw new BusinessException("重试操作失败: " + e.getMessage());
        }
    }
    
    @Override
    public OperationProgress getOperationProgress(Long operationId, User user) {
        try {
            BatchOperation operation = getBatchOperationById(operationId);
            
            // 验证用户权限
            if (!operation.getUser().getId().equals(user.getId())) {
                throw new BusinessException("无权限查看此操作进度");
            }
            
            OperationProgress progress = new OperationProgress();
            progress.setOperationId(operation.getId());
            progress.setStatus(operation.getStatus().name());
            progress.setTotalItems(operation.getTotalItems());
            progress.setProcessedItems(operation.getProcessedItems());
            progress.setSuccessItems(operation.getSuccessItems());
            progress.setFailedItems(operation.getFailedItems());
            progress.setProgressPercentage(operation.getProgressPercentage());
            
            // 计算预计完成时间
            if (operation.getEstimatedCompletion() != null) {
                progress.setEstimatedCompletion(operation.getEstimatedCompletion().toString());
            }
            
            progress.setIsCancellable(operation.getIsCancellable());
            
            return progress;
            
        } catch (Exception e) {
            log.error("获取操作进度失败: 操作ID={}, 用户ID={}", operationId, user.getId(), e);
            throw new BusinessException("获取操作进度失败: " + e.getMessage());
        }
    }
    
    @Override
    public UserOperationStats getUserOperationStats(User user) {
        try {
            // 查询用户操作统计
            Object[] stats = batchOperationRepository.getUserOperationStats(user);
            
            Long totalOperations = (Long) (stats[0] != null ? stats[0] : 0L);
            Long completedOperations = (Long) (stats[1] != null ? stats[1] : 0L);
            Long failedOperations = (Long) (stats[2] != null ? stats[2] : 0L);
            
            // 计算成功率
            Double successRate = totalOperations > 0 ? 
                (double) completedOperations / totalOperations * 100 : 0.0;
            
            // 计算平均处理时间（简化实现）
            Double averageProcessingTime = 5.0; // 默认5秒
            
            // 获取各类型操作统计
            List<Object[]> typeStats = batchOperationRepository.getOperationSuccessStats(user);
            List<UserOperationStats.OperationTypeStat> typeStatList = typeStats.stream()
                .map(stat -> {
                    String operationType = (String) stat[0];
                    Long totalCount = (Long) stat[1];
                    Long successCount = (Long) stat[2];
                    Double avgProgress = (Double) stat[3];
                    
                    Double typeSuccessRate = totalCount > 0 ? 
                        (double) successCount / totalCount * 100 : 0.0;
                    
                    return new UserOperationStats.OperationTypeStat(
                        operationType, totalCount, typeSuccessRate, avgProgress
                    );
                })
                .collect(Collectors.toList());
            
            UserOperationStats result = new UserOperationStats(
                totalOperations, completedOperations, failedOperations, averageProcessingTime
            );
            result.setTypeStats(typeStatList);
            
            return result;
            
        } catch (Exception e) {
            log.error("获取用户操作统计失败: 用户ID={}", user.getId(), e);
            throw new BusinessException("获取操作统计失败: " + e.getMessage());
        }
    }
    
    @Override
    public void cleanupCompletedOperations(User user, int daysOld) {
        try {
            LocalDateTime cutoffTime = LocalDateTime.now().minusDays(daysOld);
            List<BatchOperation> operationsToCleanup = batchOperationRepository
                .findByUserAndStatus(user, BatchOperation.OperationStatus.COMPLETED, 
                    org.springframework.data.domain.PageRequest.of(0, 1000))
                .getContent()
                .stream()
                .filter(op -> op.getCompletedAt() != null && op.getCompletedAt().isBefore(cutoffTime))
                .collect(Collectors.toList());
            
            int cleanupCount = 0;
            for (BatchOperation operation : operationsToCleanup) {
                try {
                    batchOperationRepository.delete(operation);
                    cleanupCount++;
                    log.debug("清理完成的操作记录: ID={}, 类型={}, 完成时间={}", 
                        operation.getId(), operation.getOperationType(), operation.getCompletedAt());
                } catch (Exception e) {
                    log.warn("清理操作记录失败: ID={}", operation.getId(), e);
                }
            }
            
            log.info("批量操作记录清理完成: 用户ID={}, 清理天数={}, 清理数量={}", 
                user.getId(), daysOld, cleanupCount);
                
        } catch (Exception e) {
            log.error("清理完成的操作记录失败: 用户ID={}", user.getId(), e);
            throw new BusinessException("清理操作记录失败: " + e.getMessage());
        }
    }
    
    @Override
    public BatchOperationResult batchUploadAndExtract(List<MultipartFile> files, Long targetFolderId, User user) {
        try {
            // 验证目标文件夹权限
            Folder targetFolder = null;
            if (targetFolderId != null) {
                targetFolder = folderService.getFolderEntityById(targetFolderId);
                if (!targetFolder.getOwner().getId().equals(user.getId())) {
                    throw new BusinessException("无权限访问目标文件夹");
                }
            }
            
            // 检查存储空间
            long totalSize = files.stream().mapToLong(MultipartFile::getSize).sum();
            if (!userService.hasEnoughStorage(user.getId(), totalSize)) {
                throw new BusinessException("存储空间不足");
            }
            
            // 创建批量操作记录
            BatchOperation operation = new BatchOperation();
            operation.setOperationType(BatchOperation.OperationType.UPLOAD);
            operation.setUser(user);
            operation.setDescription("批量上传并解压 " + files.size() + " 个文件");
            operation.setTotalItems(files.size());
            operation.setIsCancellable(true);
            
            BatchOperation savedOperation = createBatchOperation(operation);
            
            // 异步执行批量上传
            executeBatchOperation(savedOperation.getId());
            
            // 返回操作结果
            BatchOperationResult result = new BatchOperationResult();
            result.setOperationId(savedOperation.getId());
            result.setOperationType("UPLOAD");
            result.setTotalItems(files.size());
            result.setStatus("PENDING");
            result.setMessage("批量上传并解压任务已提交");
            
            return result;
            
        } catch (Exception e) {
            log.error("批量上传并解压初始化失败", e);
            throw new BusinessException("批量上传并解压失败: " + e.getMessage());
        }
    }
    
    @Override
    public BatchOperationResult batchUpload(List<MultipartFile> files, Long targetFolderId, User user) {
        try {
            // 验证目标文件夹权限
            Folder targetFolder = null;
            if (targetFolderId != null) {
                targetFolder = folderService.getFolderEntityById(targetFolderId);
                if (!targetFolder.getOwner().getId().equals(user.getId())) {
                    throw new BusinessException("无权限访问目标文件夹");
                }
            }
            
            // 检查存储空间
            long totalSize = files.stream().mapToLong(MultipartFile::getSize).sum();
            if (!userService.hasEnoughStorage(user.getId(), totalSize)) {
                throw new BusinessException("存储空间不足");
            }
            
            // 创建批量操作记录
            BatchOperation operation = new BatchOperation();
            operation.setOperationType(BatchOperation.OperationType.UPLOAD);
            operation.setUser(user);
            operation.setDescription("批量上传 " + files.size() + " 个文件");
            operation.setTotalItems(files.size());
            operation.setIsCancellable(true);
            
            BatchOperation savedOperation = createBatchOperation(operation);
            
            // 异步执行批量上传
            executeBatchOperation(savedOperation.getId());
            
            // 返回操作结果
            BatchOperationResult result = new BatchOperationResult();
            result.setOperationId(savedOperation.getId());
            result.setOperationType("UPLOAD");
            result.setTotalItems(files.size());
            result.setStatus("PENDING");
            result.setMessage("批量上传任务已提交");
            
            return result;
            
        } catch (Exception e) {
            log.error("批量上传初始化失败", e);
            throw new BusinessException("批量上传失败: " + e.getMessage());
        }
    }
    
    // ==================== 私有方法 ====================
    
    private void executeBatchUpload(BatchOperation operation) {
        try {
            log.info("开始执行批量上传操作: ID={}", operation.getId());
            
            // 解析操作参数（这里简化处理）
            List<MultipartFile> files = new ArrayList<>(); // 实际应该从操作参数中解析
            Long targetFolderId = null; // 实际应该从操作参数中解析
            User user = operation.getUser();
            
            int successCount = 0;
            int failureCount = 0;
            List<String> errorMessages = new ArrayList<>();
            
            // 逐个处理文件上传
            for (int i = 0; i < files.size(); i++) {
                MultipartFile file = files.get(i);
                try {
                    // 执行单个文件上传
                    fileService.uploadFile(file, targetFolderId, user);
                    successCount++;
                    
                    // 更新进度
                    double progress = ((double) (i + 1) / files.size()) * 100;
                    updateProgress(operation.getId(), progress);
                    
                } catch (Exception e) {
                    failureCount++;
                    errorMessages.add("文件 '" + file.getOriginalFilename() + "' 上传失败: " + e.getMessage());
                    log.error("批量上传单个文件失败: {}", file.getOriginalFilename(), e);
                }
                
                // 模拟处理延迟
                Thread.sleep(100);
            }
            
            // 更新最终状态
            operation.setSuccessItems(successCount);
            operation.setFailedItems(failureCount);
            operation.setErrorDetails(errorMessages.toString());
            operation.setProcessedItems(files.size());
            
            log.info("批量上传操作完成: ID={}, 成功={}, 失败={}", 
                    operation.getId(), successCount, failureCount);
            
        } catch (Exception e) {
            log.error("批量上传执行过程中发生错误: ID={}", operation.getId(), e);
            throw new BusinessException("批量上传执行失败: " + e.getMessage());
        }
    }
    
    @Override
    public BatchOperationResult batchDelete(List<Long> fileIds, User user) {
        try {
            // 验证文件权限
            for (Long fileId : fileIds) {
                FileEntity file = fileService.getFileEntityById(fileId);
                if (!file.getUploader().getId().equals(user.getId())) {
                    throw new BusinessException("无权限删除文件: " + fileId);
                }
            }
            
            // 创建批量操作记录
            BatchOperation operation = new BatchOperation();
            operation.setOperationType(BatchOperation.OperationType.DELETE);
            operation.setUser(user);
            operation.setDescription("批量删除 " + fileIds.size() + " 个文件");
            operation.setTotalItems(fileIds.size());
            operation.setIsCancellable(true);
            
            BatchOperation savedOperation = createBatchOperation(operation);
            
            // 异步执行批量删除
            executeBatchOperation(savedOperation.getId());
            
            // 返回操作结果
            BatchOperationResult result = new BatchOperationResult();
            result.setOperationId(savedOperation.getId());
            result.setOperationType("DELETE");
            result.setTotalItems(fileIds.size());
            result.setStatus("PENDING");
            result.setMessage("批量删除任务已提交");
            
            return result;
            
        } catch (Exception e) {
            log.error("批量删除初始化失败", e);
            throw new BusinessException("批量删除失败: " + e.getMessage());
        }
    }
    
    private void executeBatchDelete(BatchOperation operation) {
        try {
            log.info("开始执行批量删除操作: ID={}", operation.getId());
            
            // 解析操作参数（这里简化处理）
            List<Long> fileIds = new ArrayList<>(); // 实际应该从操作参数中解析
            User user = operation.getUser();
            
            int successCount = 0;
            int failureCount = 0;
            List<String> errorMessages = new ArrayList<>();
            
            // 逐个处理文件删除
            for (int i = 0; i < fileIds.size(); i++) {
                Long fileId = fileIds.get(i);
                try {
                    // 执行单个文件删除
                    fileService.deleteFile(fileId, user);
                    successCount++;
                    
                    // 更新进度
                    double progress = ((double) (i + 1) / fileIds.size()) * 100;
                    updateProgress(operation.getId(), progress);
                    
                } catch (Exception e) {
                    failureCount++;
                    errorMessages.add("文件ID '" + fileId + "' 删除失败: " + e.getMessage());
                    log.error("批量删除单个文件失败: {}", fileId, e);
                }
                
                // 模拟处理延迟
                Thread.sleep(50);
            }
            
            // 更新最终状态
            operation.setSuccessItems(successCount);
            operation.setFailedItems(failureCount);
            operation.setErrorDetails(errorMessages.toString());
            operation.setProcessedItems(fileIds.size());
            
            log.info("批量删除操作完成: ID={}, 成功={}, 失败={}", 
                    operation.getId(), successCount, failureCount);
            
        } catch (Exception e) {
            log.error("批量删除执行过程中发生错误: ID={}", operation.getId(), e);
            throw new BusinessException("批量删除执行失败: " + e.getMessage());
        }
    }
    
    private void executeBatchMove(BatchOperation operation) {
        // 批量移动逻辑
        log.info("执行批量移动操作: ID={}", operation.getId());
        // 这里应该实现具体的批量移动逻辑
        simulateProcessing(4000); // 模拟处理时间
    }
    
    private void executeBatchCopy(BatchOperation operation) {
        // 批量复制逻辑
        log.info("执行批量复制操作: ID={}", operation.getId());
        // 这里应该实现具体的批量复制逻辑
        simulateProcessing(6000); // 模拟处理时间
    }
    
    private void executeBatchRename(BatchOperation operation) {
        // 批量重命名逻辑
        log.info("执行批量重命名操作: ID={}", operation.getId());
        // 这里应该实现具体的批量重命名逻辑
        simulateProcessing(2000); // 模拟处理时间
    }
    
    private void simulateProcessing(long milliseconds) {
        try {
            Thread.sleep(milliseconds);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BusinessException("操作被中断");
        }
    }
    
    @Override
    public BatchOperationResult batchDeleteFolders(List<Long> folderIds, User user) {
        try {
            // 验证文件夹权限
            for (Long folderId : folderIds) {
                Folder folder = folderService.getFolderEntityById(folderId);
                if (!folder.getOwner().getId().equals(user.getId())) {
                    throw new BusinessException("无权限删除文件夹: " + folderId);
                }
            }
            
            // 创建批量操作记录
            BatchOperation operation = new BatchOperation();
            operation.setOperationType(BatchOperation.OperationType.DELETE);
            operation.setUser(user);
            operation.setDescription("批量删除 " + folderIds.size() + " 个文件夹");
            operation.setTotalItems(folderIds.size());
            operation.setIsCancellable(true);
            
            BatchOperation savedOperation = createBatchOperation(operation);
            
            // 异步执行批量删除
            executeBatchOperation(savedOperation.getId());
            
            // 返回操作结果
            BatchOperationResult result = new BatchOperationResult();
            result.setOperationId(savedOperation.getId());
            result.setOperationType("DELETE");
            result.setTotalItems(folderIds.size());
            result.setStatus("PENDING");
            result.setMessage("批量删除文件夹任务已提交");
            
            return result;
            
        } catch (Exception e) {
            log.error("批量删除文件夹初始化失败", e);
            throw new BusinessException("批量删除文件夹失败: " + e.getMessage());
        }
    }
    
    @Override
    public BatchOperationResult batchMoveFiles(List<Long> fileIds, Long targetFolderId, User user) {
        // 简化实现
        BatchOperationResult result = new BatchOperationResult();
        result.setOperationType("MOVE");
        result.setTotalItems(fileIds.size());
        result.setStatus("PENDING");
        result.setMessage("批量移动文件任务已提交");
        return result;
    }
    
    @Override
    public BatchOperationResult batchMoveFolders(List<Long> folderIds, Long targetFolderId, User user) {
        // 简化实现
        BatchOperationResult result = new BatchOperationResult();
        result.setOperationType("MOVE");
        result.setTotalItems(folderIds.size());
        result.setStatus("PENDING");
        result.setMessage("批量移动文件夹任务已提交");
        return result;
    }
    
    @Override
    public BatchOperationResult batchCopyFiles(List<Long> fileIds, Long targetFolderId, User user) {
        // 简化实现
        BatchOperationResult result = new BatchOperationResult();
        result.setOperationType("COPY");
        result.setTotalItems(fileIds.size());
        result.setStatus("PENDING");
        result.setMessage("批量复制文件任务已提交");
        return result;
    }
    
    @Override
    public BatchOperationResult batchCopyFolders(List<Long> folderIds, Long targetFolderId, User user) {
        // 简化实现
        BatchOperationResult result = new BatchOperationResult();
        result.setOperationType("COPY");
        result.setTotalItems(folderIds.size());
        result.setStatus("PENDING");
        result.setMessage("批量复制文件夹任务已提交");
        return result;
    }
    
    @Override
    public BatchOperationResult batchRenameFiles(List<FileRenameInfo> renameInfos, User user) {
        // 简化实现
        BatchOperationResult result = new BatchOperationResult();
        result.setOperationType("RENAME");
        result.setTotalItems(renameInfos.size());
        result.setStatus("PENDING");
        result.setMessage("批量重命名文件任务已提交");
        return result;
    }
    
    @Override
    public BatchOperationResult batchRenameFolders(List<FolderRenameInfo> renameInfos, User user) {
        // 简化实现
        BatchOperationResult result = new BatchOperationResult();
        result.setOperationType("RENAME");
        result.setTotalItems(renameInfos.size());
        result.setStatus("PENDING");
        result.setMessage("批量重命名文件夹任务已提交");
        return result;
    }
    
    @Override
    public BatchOperationResult batchCompress(List<Long> itemIds, String archiveName, User user) {
        // 简化实现
        BatchOperationResult result = new BatchOperationResult();
        result.setOperationType("COMPRESS");
        result.setTotalItems(itemIds.size());
        result.setStatus("PENDING");
        result.setMessage("批量压缩任务已提交");
        return result;
    }
    
    @Override
    public Page<BatchOperationInfo> getUserOperations(User user, String operationType, String status, int page, int size) {
        // 简化实现
        return Page.empty();
    }
    
    @Override
    public BatchOperationDetailInfo getOperationDetails(Long operationId, User user) {
        // 简化实现
        return new BatchOperationDetailInfo();
    }
    
    @Override
    public void cancelOperation(Long operationId, User user) {
        // 简化实现
        cancelBatchOperation(operationId, user);
    }
}