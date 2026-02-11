package com.filesharing.repository;

import com.filesharing.entity.FileStatistics;
import com.filesharing.entity.FileEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 文件统计Repository接口
 */
@Repository
public interface FileStatisticsRepository extends JpaRepository<FileStatistics, Long> {
    
    /**
     * 根据文件查找统计信息
     */
    Optional<FileStatistics> findByFile(FileEntity file);
    
    /**
     * 查找热门文件（按下载次数排序）
     */
    @Query("SELECT fs FROM FileStatistics fs ORDER BY fs.totalDownloads DESC")
    Page<FileStatistics> findPopularFiles(Pageable pageable);
    
    /**
     * 查找最新文件（按创建时间排序）
     */
    @Query("SELECT fs FROM FileStatistics fs ORDER BY fs.file.createdAt DESC")
    Page<FileStatistics> findLatestFiles(Pageable pageable);
    
    /**
     * 查找最活跃文件（按总交互次数排序）
     */
    @Query("SELECT fs FROM FileStatistics fs ORDER BY (fs.totalDownloads + fs.totalPreviews + fs.totalShares) DESC")
    Page<FileStatistics> findMostActiveFiles(Pageable pageable);
    
    /**
     * 查找指定时间范围内的文件统计
     */
    @Query("SELECT fs FROM FileStatistics fs WHERE fs.updatedAt BETWEEN :startTime AND :endTime")
    Page<FileStatistics> findByUpdatedAtBetween(@Param("startTime") LocalDateTime startTime, 
                                               @Param("endTime") LocalDateTime endTime, 
                                               Pageable pageable);
    
    /**
     * 统计各类文件的平均下载次数
     */
    @Query("SELECT fs.file.extension, AVG(fs.totalDownloads) FROM FileStatistics fs " +
           "WHERE fs.file.extension IS NOT NULL GROUP BY fs.file.extension")
    List<Object[]> getAverageDownloadsByFileType();
    
    /**
     * 获取文件大小分布统计
     */
    @Query("SELECT " +
           "CASE " +
           "  WHEN f.fileSize < 1048576 THEN '< 1MB' " +
           "  WHEN f.fileSize < 10485760 THEN '1MB-10MB' " +
           "  WHEN f.fileSize < 104857600 THEN '10MB-100MB' " +
           "  ELSE '> 100MB' " +
           "END as sizeRange, " +
           "COUNT(f) as fileCount " +
           "FROM FileEntity f WHERE f.status = 'AVAILABLE' GROUP BY sizeRange")
    List<Object[]> getFileSizeDistribution();
    
    /**
     * 查找零下载的文件
     */
    @Query("SELECT fs FROM FileStatistics fs WHERE fs.totalDownloads = 0")
    Page<FileStatistics> findZeroDownloadFiles(Pageable pageable);
    
    /**
     * 获取文件活跃度排名
     */
    @Query("SELECT fs.file.id, fs.file.originalName, " +
           "(fs.totalDownloads * 2 + fs.totalPreviews + fs.totalShares) as activityScore " +
           "FROM FileStatistics fs ORDER BY activityScore DESC")
    Page<Object[]> getFileActivityRanking(Pageable pageable);
    
    /**
     * 统计最近N天的文件活动
     */
    /*
    @Query("SELECT DATE(fs.updatedAt) as statDate, " +
           "SUM(fs.totalDownloads) as totalDownloads, " +
           "SUM(fs.totalPreviews) as totalPreviews, " +
           "COUNT(fs.file) as activeFiles " +
           "FROM FileStatistics fs WHERE fs.updatedAt >= :since " +
           "GROUP BY DATE(fs.updatedAt) ORDER BY statDate DESC")
    List<Object[]> getRecentFileActivity(@Param("since") LocalDateTime since);
    */
}