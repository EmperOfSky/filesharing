package com.filesharing.service.impl;

import com.filesharing.entity.BatchOperation;
import com.filesharing.entity.User;
import com.filesharing.exception.BusinessException;
import com.filesharing.repository.BatchOperationRepository;
import com.filesharing.service.BatchOperationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * 批量操作服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class BatchOperationServiceImpl implements BatchOperationService {
    
    private final BatchOperationRepository batchOperationRepository;
    
    @Override
    public BatchOperation createBatchOperation(BatchOperation operation) {
        operation.setStartedAt(LocalDateTime.now());
        operation.setStatus(BatchOperation.OperationStatus.PENDING);
        operation.setProgressPercentage(0.0);
        
        BatchOperation savedOperation = batchOperationRepository.save(operation);
        log.info("创建批量操作: ID={}, 类型={}", savedOperation.getId(), savedOperation.getOperationType());
        
        return savedOperation;
    }
    
    @Override
    @Transactional(readOnly = true)
    public BatchOperation getBatchOperationById(Long operationId) {
        return batchOperationRepository.findById(operationId)
                .orElseThrow(() -> new BusinessException("批量操作不存在"));
    }
    
    @Override
    @Transactional(readOnly = true)
    public Page<BatchOperation> getUserBatchOperations(User user, Pageable pageable) {
        return batchOperationRepository.findByUserOrderByCreatedAtDesc(user, pageable);
    }
    
    @Override
    @Transactional(readOnly = true)
    public Page<BatchOperation> getUserBatchOperationsByType(User user, 
                                                           BatchOperation.OperationType operationType, 
                                                           Pageable pageable) {
        return batchOperationRepository.findByUserAndOperationType(user, operationType, pageable);
    }
    
    @Override
    @Transactional(readOnly = true)
    public Page<BatchOperation> getUserBatchOperationsByStatus(User user, 
                                                              BatchOperation.OperationStatus status, 
                                                              Pageable pageable) {
        return batchOperationRepository.findByUserAndStatus(user, status, pageable);
    }
    
    @Override
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
            operation.setErrorMessage(e.getMessage());
            batchOperationRepository.save(operation);
            
            log.error("批量操作执行失败: ID={}, 错误={}", operationId, e.getMessage());
        }
        
        return CompletableFuture.completedFuture(null);
    }
    
    @Override
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
    
    @Override
    @Transactional(readOnly = true)
    public List<BatchOperation> getProcessingOperations() {
        return batchOperationRepository.findProcessingOperations();
    }
    
    @Override
    @Transactional(readOnly = true)
    public Page<BatchOperation> getCancellableOperations(Pageable pageable) {
        return batchOperationRepository.findCancellableOperations(pageable);
    }
    
    @Override
    public void updateProgress(Long operationId, Double progress) {
        BatchOperation operation = getBatchOperationById(operationId);
        operation.setProgressPercentage(progress);
        batchOperationRepository.save(operation);
    }
    
    @Override
    public void updateEstimatedCompletion(Long operationId, LocalDateTime estimatedTime) {
        BatchOperation operation = getBatchOperationById(operationId);
        operation.setEstimatedCompletion(estimatedTime);
        batchOperationRepository.save(operation);
    }
    
    // ==================== 私有方法 ====================
    
    private void executeBatchUpload(BatchOperation operation) {
        // 批量上传逻辑
        log.info("执行批量上传操作: ID={}", operation.getId());
        // 这里应该实现具体的批量上传逻辑
        simulateProcessing(5000); // 模拟处理时间
    }
    
    private void executeBatchDelete(BatchOperation operation) {
        // 批量删除逻辑
        log.info("执行批量删除操作: ID={}", operation.getId());
        // 这里应该实现具体的批量删除逻辑
        simulateProcessing(3000); // 模拟处理时间
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
}