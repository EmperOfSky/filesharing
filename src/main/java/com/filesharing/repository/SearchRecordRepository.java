package com.filesharing.repository;

import com.filesharing.entity.SearchRecord;
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
 * 搜索记录Repository接口
 */
@Repository
public interface SearchRecordRepository extends JpaRepository<SearchRecord, Long> {
    
    /**
     * 根据用户查找搜索记录
     */
    Page<SearchRecord> findByUser(User user, Pageable pageable);
    
    /**
     * 根据搜索类型查找记录
     */
    @Query("SELECT s FROM SearchRecord s WHERE s.searchType = :searchType")
    Page<SearchRecord> findBySearchType(@Param("searchType") SearchRecord.SearchType searchType, Pageable pageable);
    
    /**
     * 查找指定时间范围内的搜索记录
     */
    @Query("SELECT s FROM SearchRecord s WHERE s.searchTime BETWEEN :startTime AND :endTime")
    Page<SearchRecord> findBySearchTimeBetween(@Param("startTime") LocalDateTime startTime, 
                                              @Param("endTime") LocalDateTime endTime, 
                                              Pageable pageable);
    
    /**
     * 统计用户的搜索次数
     */
    @Query("SELECT COUNT(s) FROM SearchRecord s WHERE s.user = :user")
    Long countByUser(@Param("user") User user);
    
    /**
     * 获取热门搜索关键词
     */
    @Query("SELECT s.searchKeyword, COUNT(s) as searchCount FROM SearchRecord s " +
           "WHERE s.searchTime >= :since GROUP BY s.searchKeyword " +
           "ORDER BY searchCount DESC")
    List<Object[]> findPopularKeywords(@Param("since") LocalDateTime since, Pageable pageable);
    
    /**
     * 获取搜索趋势（按天统计）
     */
    /*
    @Query("SELECT DATE(s.searchTime) as searchDate, COUNT(s) as searchCount " +
           "FROM SearchRecord s WHERE s.searchTime >= :since " +
           "GROUP BY DATE(s.searchTime) ORDER BY searchDate DESC")
    List<Object[]> getSearchTrend(@Param("since") LocalDateTime since);
    */
    
    /**
     * 获取用户搜索习惯统计
     */
    @Query("SELECT s.searchType, COUNT(s) FROM SearchRecord s WHERE s.user = :user " +
           "GROUP BY s.searchType")
    List<Object[]> getUserSearchHabits(@Param("user") User user);
    
    /**
     * 查找最近的搜索记录
     */
    @Query("SELECT s FROM SearchRecord s ORDER BY s.searchTime DESC")
    Page<SearchRecord> findRecentSearches(Pageable pageable);
    
    /**
     * 查找无结果的搜索记录
     */
    @Query("SELECT s FROM SearchRecord s WHERE s.resultCount = 0 ORDER BY s.searchTime DESC")
    Page<SearchRecord> findZeroResultSearches(Pageable pageable);
    
    /**
     * 统计搜索成功率
     */
    @Query("SELECT " +
           "COUNT(s) as totalCount, " +
           "SUM(CASE WHEN s.resultCount > 0 THEN 1 ELSE 0 END) as successCount, " +
           "AVG(s.searchDuration) as avgDuration " +
           "FROM SearchRecord s WHERE s.searchTime >= :since")
    Object[] getSearchStatistics(@Param("since") LocalDateTime since);
}