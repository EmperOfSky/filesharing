package com.filesharing.repository;

import com.filesharing.entity.AIAnalysisRecord;
import com.filesharing.entity.AIModel;
import com.filesharing.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AIAnalysisRecordRepository extends JpaRepository<AIAnalysisRecord, Long> {
    
    /**
     * 根据AI模型查找分析记录
     */
    Page<AIAnalysisRecord> findByAiModelOrderByCreatedAtDesc(AIModel aiModel, Pageable pageable);
    
    /**
     * 根据处理状态查找记录
     */
    Page<AIAnalysisRecord> findByProcessStatusOrderByCreatedAtDesc(AIAnalysisRecord.ProcessStatus processStatus, Pageable pageable);
    
    /**
     * 根据请求用户查找记录
     */
    Page<AIAnalysisRecord> findByRequestedByOrderByCreatedAtDesc(User requestedBy, Pageable pageable);
    
    /**
     * 根据目标类型和ID查找记录
     */
    @Query("SELECT r FROM AIAnalysisRecord r WHERE r.targetType = :targetType AND r.targetId = :targetId ORDER BY r.createdAt DESC")
    Page<AIAnalysisRecord> findByTargetTypeAndTargetId(@Param("targetType") AIAnalysisRecord.TargetType targetType, 
                                                      @Param("targetId") Long targetId, 
                                                      Pageable pageable);
    
    /**
     * 统计指定时间范围内的分析记录
     */
    @Query("SELECT r.processStatus, COUNT(r) FROM AIAnalysisRecord r WHERE r.createdAt BETWEEN :startTime AND :endTime GROUP BY r.processStatus")
    List<Object[]> getAnalysisStatsByTimeRange(@Param("startTime") LocalDateTime startTime, 
                                              @Param("endTime") LocalDateTime endTime);
    
    /**
     * 查找失败的分析记录
     */
    @Query("SELECT r FROM AIAnalysisRecord r WHERE r.processStatus = 'FAILED' AND r.createdAt >= :since ORDER BY r.createdAt DESC")
    Page<AIAnalysisRecord> findFailedRecordsSince(@Param("since") LocalDateTime since, Pageable pageable);
    
    /**
     * 统计各模型的使用情况
     */
    @Query("SELECT r.aiModel.modelName, COUNT(r), AVG(r.confidenceScore) FROM AIAnalysisRecord r " +
           "WHERE r.processStatus = 'SUCCESS' GROUP BY r.aiModel.id, r.aiModel.modelName")
    List<Object[]> getModelUsageStats();
    
    /**
     * 查找置信度低于阈值的记录
     */
    @Query("SELECT r FROM AIAnalysisRecord r WHERE r.confidenceScore < :threshold AND r.processStatus = 'SUCCESS' ORDER BY r.confidenceScore ASC")
    Page<AIAnalysisRecord> findLowConfidenceRecords(@Param("threshold") Double threshold, Pageable pageable);
}