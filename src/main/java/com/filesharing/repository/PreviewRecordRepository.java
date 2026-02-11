package com.filesharing.repository;

import com.filesharing.entity.PreviewRecord;
import com.filesharing.entity.User;
import com.filesharing.entity.FileEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 预览记录Repository接口
 */
@Repository
public interface PreviewRecordRepository extends JpaRepository<PreviewRecord, Long> {
    
    /**
     * 根据文件查找预览记录
     */
    Page<PreviewRecord> findByFile(FileEntity file, Pageable pageable);
    
    /**
     * 根据用户查找预览记录
     */
    Page<PreviewRecord> findByUser(User user, Pageable pageable);
    
    /**
     * 根据预览类型查找记录
     */
    @Query("SELECT p FROM PreviewRecord p WHERE p.previewType = :previewType")
    Page<PreviewRecord> findByPreviewType(@Param("previewType") PreviewRecord.PreviewType previewType, Pageable pageable);
    
    /**
     * 查找指定时间范围内的预览记录
     */
    @Query("SELECT p FROM PreviewRecord p WHERE p.previewTime BETWEEN :startTime AND :endTime")
    Page<PreviewRecord> findByPreviewTimeBetween(@Param("startTime") LocalDateTime startTime, 
                                                @Param("endTime") LocalDateTime endTime, 
                                                Pageable pageable);
    
    /**
     * 统计文件的预览次数
     */
    @Query("SELECT COUNT(p) FROM PreviewRecord p WHERE p.file = :file")
    Long countByFile(@Param("file") FileEntity file);
    
    /**
     * 统计用户的预览次数
     */
    @Query("SELECT COUNT(p) FROM PreviewRecord p WHERE p.user = :user")
    Long countByUser(@Param("user") User user);
    
    /**
     * 获取最受欢迎的文件（按预览次数排序）
     */
    @Query("SELECT p.file, COUNT(p) as previewCount FROM PreviewRecord p " +
           "GROUP BY p.file ORDER BY previewCount DESC")
    Page<Object[]> findMostPreviewedFiles(Pageable pageable);
    
    /**
     * 获取用户的预览统计
     */
    @Query("SELECT p.previewType, COUNT(p) FROM PreviewRecord p WHERE p.user = :user " +
           "GROUP BY p.previewType")
    List<Object[]> getUserPreviewStatistics(@Param("user") User user);
    
    /**
     * 获取文件的预览趋势（按天统计）
     */
    /*
    @Query("SELECT DATE(p.previewTime) as previewDate, COUNT(p) as previewCount " +
           "FROM PreviewRecord p WHERE p.file = :file " +
           "GROUP BY DATE(p.previewTime) ORDER BY previewDate DESC")
    List<Object[]> getFilePreviewTrend(@Param("file") FileEntity file);
    */
    
    /**
     * 查找最近的预览记录
     */
    @Query("SELECT p FROM PreviewRecord p ORDER BY p.previewTime DESC")
    Page<PreviewRecord> findRecentPreviews(Pageable pageable);
    
    /**
     * 查找失败的预览记录
     */
    @Query("SELECT p FROM PreviewRecord p WHERE p.isSuccess = false ORDER BY p.previewTime DESC")
    Page<PreviewRecord> findFailedPreviews(Pageable pageable);
}