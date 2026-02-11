package com.filesharing.repository;

import com.filesharing.entity.BatchOperationDetail;
import com.filesharing.entity.BatchOperation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 批量操作详情Repository接口
 */
@Repository
public interface BatchOperationDetailRepository extends JpaRepository<BatchOperationDetail, Long> {
    
    /**
     * 根据批量操作查找详情（按创建时间排序）
     */
    Page<BatchOperationDetail> findByBatchOperationOrderByCreatedAtAsc(BatchOperation batchOperation, Pageable pageable);
    
    /**
     * 根据批量操作和状态查找详情
     */
    @Query("SELECT bod FROM BatchOperationDetail bod WHERE bod.batchOperation = :batchOperation " +
           "AND bod.itemStatus = :itemStatus ORDER BY bod.createdAt ASC")
    Page<BatchOperationDetail> findByBatchOperationAndItemStatus(@Param("batchOperation") BatchOperation batchOperation,
                                                                @Param("itemStatus") BatchOperationDetail.ItemStatus itemStatus,
                                                                Pageable pageable);
    
    /**
     * 根据项目ID查找操作详情
     */
    @Query("SELECT bod FROM BatchOperationDetail bod WHERE bod.itemId = :itemId AND bod.itemType = :itemType")
    List<BatchOperationDetail> findByItemIdAndItemType(@Param("itemId") Long itemId,
                                                      @Param("itemType") BatchOperationDetail.ItemType itemType);
    
    /**
     * 统计批量操作的详细结果
     */
    @Query("SELECT bod.itemStatus, COUNT(bod) FROM BatchOperationDetail bod " +
           "WHERE bod.batchOperation = :batchOperation GROUP BY bod.itemStatus")
    List<Object[]> countDetailsByStatus(@Param("batchOperation") BatchOperation batchOperation);
    
    /**
     * 获取失败的操作详情
     */
    @Query("SELECT bod FROM BatchOperationDetail bod WHERE bod.batchOperation = :batchOperation " +
           "AND bod.itemStatus = 'FAILED' ORDER BY bod.createdAt DESC")
    Page<BatchOperationDetail> findFailedDetails(@Param("batchOperation") BatchOperation batchOperation, Pageable pageable);
    
    /**
     * 获取成功的操作详情
     */
    @Query("SELECT bod FROM BatchOperationDetail bod WHERE bod.batchOperation = :batchOperation " +
           "AND bod.itemStatus = 'SUCCESS' ORDER BY bod.completedAt DESC")
    Page<BatchOperationDetail> findSuccessDetails(@Param("batchOperation") BatchOperation batchOperation, Pageable pageable);
    
    /**
     * 统计各类型项目的操作结果
     */
    @Query("SELECT bod.itemType, bod.itemStatus, COUNT(bod) FROM BatchOperationDetail bod " +
           "WHERE bod.batchOperation = :batchOperation GROUP BY bod.itemType, bod.itemStatus")
    List<Object[]> getItemTypeStats(@Param("batchOperation") BatchOperation batchOperation);
}