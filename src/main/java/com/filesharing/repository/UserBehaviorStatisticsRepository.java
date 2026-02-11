package com.filesharing.repository;

import com.filesharing.entity.UserBehaviorStatistics;
import com.filesharing.entity.User;
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
 * 用户行为统计Repository接口
 */
@Repository
public interface UserBehaviorStatisticsRepository extends JpaRepository<UserBehaviorStatistics, Long> {
    
    /**
     * 根据用户查找行为统计
     */
    Optional<UserBehaviorStatistics> findByUser(User user);
    
    /**
     * 查找最活跃用户（按总操作数排序）
     */
    @Query("SELECT ubs FROM UserBehaviorStatistics ubs ORDER BY " +
           "(ubs.totalUploads + ubs.totalDownloads + ubs.totalPreviews + ubs.totalShares) DESC")
    Page<UserBehaviorStatistics> findMostActiveUsers(Pageable pageable);
    
    /**
     * 查找高贡献用户（按上传文件数排序）
     */
    @Query("SELECT ubs FROM UserBehaviorStatistics ubs ORDER BY ubs.totalUploads DESC")
    Page<UserBehaviorStatistics> findTopContributors(Pageable pageable);
    
    /**
     * 查找存储大户（按存储使用量排序）
     */
    @Query("SELECT ubs FROM UserBehaviorStatistics ubs ORDER BY ubs.totalStorageUsed DESC")
    Page<UserBehaviorStatistics> findStorageHeavyUsers(Pageable pageable);
    
    /**
     * 查找新手用户（按首次活跃时间排序）
     */
    @Query("SELECT ubs FROM UserBehaviorStatistics ubs WHERE ubs.firstActiveTime IS NOT NULL " +
           "ORDER BY ubs.firstActiveTime DESC")
    Page<UserBehaviorStatistics> findNewUsers(Pageable pageable);
    
    /**
     * 查找长期不活跃用户
     */
    @Query("SELECT ubs FROM UserBehaviorStatistics ubs WHERE ubs.lastActiveTime < :beforeTime")
    Page<UserBehaviorStatistics> findInactiveUsers(@Param("beforeTime") LocalDateTime beforeTime, Pageable pageable);
    
    /**
     * 统计用户等级分布
     */
    @Query("SELECT ubs.userLevel, COUNT(ubs) FROM UserBehaviorStatistics ubs GROUP BY ubs.userLevel")
    List<Object[]> getUserLevelDistribution();
    
    /**
     * 获取用户活跃度趋势
     */
    /*
    @Query("SELECT DATE(ubs.lastActiveTime) as activeDate, COUNT(ubs) as activeUserCount " +
           "FROM UserBehaviorStatistics ubs WHERE ubs.lastActiveTime >= :since " +
           "GROUP BY DATE(ubs.lastActiveTime) ORDER BY activeDate DESC")
    List<Object[]> getUserActivityTrend(@Param("since") LocalDateTime since);
    */
    
    /**
     * 统计各文件类型的用户偏好
     */
    @Query("SELECT ubs.favoriteFileType, COUNT(ubs) FROM UserBehaviorStatistics ubs " +
           "WHERE ubs.favoriteFileType IS NOT NULL GROUP BY ubs.favoriteFileType")
    List<Object[]> getUserFileTypePreferences();
    
    /**
     * 获取用户增长趋势
     */
    /*
    @Query("SELECT DATE(ubs.firstActiveTime) as joinDate, COUNT(ubs) as newUserCount " +
           "FROM UserBehaviorStatistics ubs WHERE ubs.firstActiveTime >= :since " +
           "GROUP BY DATE(ubs.firstActiveTime) ORDER BY joinDate DESC")
    List<Object[]> getUserGrowthTrend(@Param("since") LocalDateTime since);
    */
    
    /**
     * 查找VIP用户（高等级用户）
     */
    @Query("SELECT ubs FROM UserBehaviorStatistics ubs WHERE ubs.userLevel >= :minLevel")
    Page<UserBehaviorStatistics> findVipUsers(@Param("minLevel") Integer minLevel, Pageable pageable);
    
    /**
     * 统计用户平均指标
     */
    @Query("SELECT " +
           "AVG(ubs.totalUploads) as avgUploads, " +
           "AVG(ubs.totalDownloads) as avgDownloads, " +
           "AVG(ubs.totalPreviews) as avgPreviews, " +
           "AVG(ubs.totalStorageUsed) as avgStorage " +
           "FROM UserBehaviorStatistics ubs")
    Object[] getAverageUserMetrics();
}