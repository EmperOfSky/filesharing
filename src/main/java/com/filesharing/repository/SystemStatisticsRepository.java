package com.filesharing.repository;

import com.filesharing.entity.SystemStatistics;
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
 * 系统统计Repository接口
 */
@Repository
public interface SystemStatisticsRepository extends JpaRepository<SystemStatistics, Long> {
    
    /**
     * 根据日期查找系统统计
     */
    Optional<SystemStatistics> findByStatDate(LocalDateTime statDate);
    
    /**
     * 查找最新的系统统计
     */
    @Query("SELECT ss FROM SystemStatistics ss ORDER BY ss.statDate DESC")
    Page<SystemStatistics> findLatestStats(Pageable pageable);
    
    /**
     * 查找指定时间范围内的统计
     */
    @Query("SELECT ss FROM SystemStatistics ss WHERE ss.statDate BETWEEN :startTime AND :endTime " +
           "ORDER BY ss.statDate DESC")
    Page<SystemStatistics> findByStatDateBetween(@Param("startTime") LocalDateTime startTime, 
                                                @Param("endTime") LocalDateTime endTime, 
                                                Pageable pageable);
    
    /**
     * 获取系统负载趋势
     */
    @Query("SELECT ss.statDate, ss.systemLoad, ss.cpuUsage, ss.memoryUsage, ss.diskUsage " +
           "FROM SystemStatistics ss WHERE ss.statDate >= :since ORDER BY ss.statDate DESC")
    List<Object[]> getSystemLoadTrend(@Param("since") LocalDateTime since);
    
    /**
     * 获取用户增长趋势
     */
    @Query("SELECT ss.statDate, ss.totalUsers, ss.newUsers, ss.activeUsers " +
           "FROM SystemStatistics ss WHERE ss.statDate >= :since ORDER BY ss.statDate DESC")
    List<Object[]> getUserGrowthTrend(@Param("since") LocalDateTime since);
    
    /**
     * 获取文件增长趋势
     */
    @Query("SELECT ss.statDate, ss.totalFiles, ss.newFiles, ss.totalFolders " +
           "FROM SystemStatistics ss WHERE ss.statDate >= :since ORDER BY ss.statDate DESC")
    List<Object[]> getFileGrowthTrend(@Param("since") LocalDateTime since);
    
    /**
     * 获取存储使用趋势
     */
    @Query("SELECT ss.statDate, ss.totalStorageUsed, ss.totalStorageQuota, " +
           "ss.averageFileSize FROM SystemStatistics ss WHERE ss.statDate >= :since " +
           "ORDER BY ss.statDate DESC")
    List<Object[]> getStorageTrend(@Param("since") LocalDateTime since);
    
    /**
     * 获取性能指标趋势
     */
    @Query("SELECT ss.statDate, ss.avgResponseTime, ss.successRequests, ss.errorRequests " +
           "FROM SystemStatistics ss WHERE ss.statDate >= :since ORDER BY ss.statDate DESC")
    List<Object[]> getPerformanceTrend(@Param("since") LocalDateTime since);
    
    /**
     * 统计最受欢迎的文件类型
     */
    @Query("SELECT SUBSTRING(ss.popularFileTypes, 1, 200) as fileTypes, " +
           "COUNT(ss) as frequency FROM SystemStatistics ss " +
           "WHERE ss.popularFileTypes IS NOT NULL GROUP BY fileTypes")
    List<Object[]> getPopularFileTypes();
    
    /**
    * 获取系统健康度报告
    */
    @Query("SELECT " +
           "AVG(ss.systemLoad) as avgLoad, " +
           "AVG(ss.cpuUsage) as avgCpu, " +
           "AVG(ss.memoryUsage) as avgMemory, " +
           "AVG(ss.diskUsage) as avgDisk, " +
           "AVG(ss.avgResponseTime) as avgResponse " +
           "FROM SystemStatistics ss WHERE ss.statDate >= :since")
    Object[] getSystemHealthReport(@Param("since") LocalDateTime since);
    
    /**
     * 查找异常统计数据
     */
    @Query("SELECT ss FROM SystemStatistics ss WHERE " +
           "ss.systemLoad > 0.8 OR ss.cpuUsage > 0.8 OR ss.memoryUsage > 0.8 OR " +
           "ss.errorRequests > ss.successRequests * 0.1 ORDER BY ss.statDate DESC")
    Page<SystemStatistics> findAbnormalStats(Pageable pageable);
    
    /**
     * 获取最近24小时的详细统计
     */
    @Query("SELECT ss FROM SystemStatistics ss WHERE ss.statDate >= :yesterday " +
           "ORDER BY ss.statDate DESC")
    List<SystemStatistics> getLast24HoursStats(@Param("yesterday") LocalDateTime yesterday);
    
    /**
     * 统计系统峰值使用情况
     */
    @Query("SELECT " +
           "MAX(ss.systemLoad) as peakLoad, " +
           "MAX(ss.cpuUsage) as peakCpu, " +
           "MAX(ss.memoryUsage) as peakMemory, " +
           "MAX(ss.diskUsage) as peakDisk " +
           "FROM SystemStatistics ss")
    Object[] getPeakUsageStats();
}