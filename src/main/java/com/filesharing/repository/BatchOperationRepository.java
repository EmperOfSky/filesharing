package com.filesharing.repository;

import com.filesharing.entity.BatchOperation;
import com.filesharing.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 批量操作Repository接口
 */
@Repository
public interface BatchOperationRepository extends JpaRepository<BatchOperation, Long> {
    
    /**
     * 根据用户查找批量操作（按创建时间倒序）
     */
    Page<BatchOperation> findByUserOrderByCreatedAtDesc(User user, Pageable pageable);
    
    /**
     * 根据用户和操作类型查找
     */
    @Query("SELECT bo FROM BatchOperation bo WHERE bo.user = :user AND bo.operationType = :operationType " +
           "ORDER BY bo.createdAt DESC")
    Page<BatchOperation> findByUserAndOperationType(@Param("user") User user, 
                                                   @Param("operationType") BatchOperation.OperationType operationType, 
                                                   Pageable pageable);
    
    /**
     * 根据用户和状态查找
     */
    Page<BatchOperation> findByUserAndStatus(User user, BatchOperation.OperationStatus status, Pageable pageable);
    
    /**
     * 查找正在进行的操作
     */
    @Query("SELECT bo FROM BatchOperation bo WHERE bo.status = 'PROCESSING' ORDER BY bo.startedAt ASC")
    List<BatchOperation> findProcessingOperations();
    
    /**
     * 查找可取消的操作
     */
    @Query("SELECT bo FROM BatchOperation bo WHERE bo.status IN ('PENDING', 'PROCESSING') " +
           "AND bo.isCancellable = true ORDER BY bo.createdAt DESC")
    Page<BatchOperation> findCancellableOperations(Pageable pageable);
    
    /**
     * 统计用户各类操作数量
     */
    @Query("SELECT bo.operationType, COUNT(bo) FROM BatchOperation bo WHERE bo.user = :user " +
           "GROUP BY bo.operationType")
    List<Object[]> countOperationsByType(@Param("user") User user);
    
    /**
     * 获取操作成功率统计
     */
    @Query("SELECT bo.operationType, " +
           "COUNT(bo) as total, " +
           "SUM(CASE WHEN bo.status = 'COMPLETED' THEN 1 ELSE 0 END) as success, " +
           "AVG(bo.progressPercentage) as avgProgress " +
           "FROM BatchOperation bo WHERE bo.user = :user GROUP BY bo.operationType")
    List<Object[]> getOperationSuccessStats(@Param("user") User user);
    
    /**
     * 查找指定时间范围内的操作
     */
    @Query("SELECT bo FROM BatchOperation bo WHERE bo.user = :user " +
           "AND bo.createdAt BETWEEN :startTime AND :endTime ORDER BY bo.createdAt DESC")
    Page<BatchOperation> findByUserAndCreatedAtBetween(@Param("user") User user,
                                                      @Param("startTime") LocalDateTime startTime,
                                                      @Param("endTime") LocalDateTime endTime,
                                                      Pageable pageable);
    
    /**
     * 获取用户的操作统计
     */
    @Query("SELECT " +
           "COUNT(bo) as totalOperations, " +
           "SUM(CASE WHEN bo.status = 'COMPLETED' THEN 1 ELSE 0 END) as completed, " +
           "SUM(CASE WHEN bo.status = 'FAILED' THEN 1 ELSE 0 END) as failed " +
           "FROM BatchOperation bo WHERE bo.user = :user")
    Object[] getUserOperationStats(@Param("user") User user);
    
    /**
     * 查找长时间运行的操作
     */
    @Query("SELECT bo FROM BatchOperation bo WHERE bo.status = 'PROCESSING' " +
           "AND bo.startedAt < :beforeTime ORDER BY bo.startedAt ASC")
    List<BatchOperation> findLongRunningOperations(@Param("beforeTime") LocalDateTime beforeTime);
}