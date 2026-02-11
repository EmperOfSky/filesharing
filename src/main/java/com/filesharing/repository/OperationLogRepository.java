package com.filesharing.repository;

import com.filesharing.entity.OperationLog;
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
 * 操作日志Repository接口
 */
@Repository
public interface OperationLogRepository extends JpaRepository<OperationLog, Long> {
    
    /**
     * 根据用户查找操作日志
     */
    Page<OperationLog> findByUser(User user, Pageable pageable);
    
    /**
     * 根据操作类型查找日志
     */
    @Query("SELECT o FROM OperationLog o WHERE o.operationType = :operationType")
    Page<OperationLog> findByOperationType(@Param("operationType") OperationLog.OperationType operationType, Pageable pageable);
    
    /**
     * 根据操作结果查找日志
     */
    Page<OperationLog> findByResult(OperationLog.OperationResult result, Pageable pageable);
    
    /**
     * 查找指定时间范围内的操作日志
     */
    @Query("SELECT o FROM OperationLog o WHERE o.operationTime BETWEEN :startTime AND :endTime")
    Page<OperationLog> findByOperationTimeBetween(@Param("startTime") LocalDateTime startTime, 
                                                 @Param("endTime") LocalDateTime endTime, 
                                                 Pageable pageable);
    
    /**
     * 根据用户和操作类型查找日志
     */
    @Query("SELECT o FROM OperationLog o WHERE o.user = :user AND o.operationType = :operationType")
    Page<OperationLog> findByUserAndOperationType(@Param("user") User user, 
                                                 @Param("operationType") OperationLog.OperationType operationType, 
                                                 Pageable pageable);
    
    /**
     * 查找包含特定IP地址的日志
     */
    @Query("SELECT o FROM OperationLog o WHERE o.ipAddress = :ipAddress")
    Page<OperationLog> findByIpAddress(@Param("ipAddress") String ipAddress, Pageable pageable);
    
    /**
     * 统计用户操作次数
     */
    @Query("SELECT COUNT(o) FROM OperationLog o WHERE o.user = :user")
    Long countByUser(@Param("user") User user);
    
    /**
     * 统计特定操作类型的次数
     */
    @Query("SELECT COUNT(o) FROM OperationLog o WHERE o.operationType = :operationType")
    Long countByOperationType(@Param("operationType") OperationLog.OperationType operationType);
    
    /**
     * 查找最近的操作日志
     */
    @Query("SELECT o FROM OperationLog o ORDER BY o.operationTime DESC")
    Page<OperationLog> findRecentLogs(Pageable pageable);
    
    /**
     * 查找失败的操作日志
     */
    @Query("SELECT o FROM OperationLog o WHERE o.result = 'FAILED' ORDER BY o.operationTime DESC")
    Page<OperationLog> findFailedOperations(Pageable pageable);
    
    /**
     * 根据用户代理查找日志（用于识别客户端类型）
     */
    @Query("SELECT o FROM OperationLog o WHERE o.userAgent LIKE %:userAgent%")
    Page<OperationLog> findByUserAgentContaining(@Param("userAgent") String userAgent, Pageable pageable);
}