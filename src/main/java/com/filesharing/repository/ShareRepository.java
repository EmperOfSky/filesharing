package com.filesharing.repository;

import com.filesharing.entity.ShareRecord;
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
 * 分享记录Repository接口
 */
@Repository
public interface ShareRepository extends JpaRepository<ShareRecord, Long> {
    
    /**
     * 根据分享键查找分享记录
     */
    Optional<ShareRecord> findByShareKey(String shareKey);
    
    /**
     * 根据分享者查找分享记录
     */
    Page<ShareRecord> findBySharer(User sharer, Pageable pageable);
    
    /**
     * 根据分享者和分享状态查找
     */
    Page<ShareRecord> findBySharerAndStatus(User sharer, ShareRecord.ShareStatus status, Pageable pageable);
    
    /**
     * 根据分享类型查找
     */
    @Query("SELECT s FROM ShareRecord s WHERE s.shareType = :shareType")
    Page<ShareRecord> findByShareType(@Param("shareType") ShareRecord.ShareType shareType, Pageable pageable);
    
    /**
     * 查找有效的分享记录（未过期且未达到最大访问次数）
     */
    @Query("SELECT s FROM ShareRecord s WHERE s.status = 'ACTIVE' " +
           "AND (s.expireTime IS NULL OR s.expireTime > :currentTime) " +
           "AND (s.maxAccessCount = 0 OR s.currentAccessCount < s.maxAccessCount)")
    List<ShareRecord> findActiveShares(@Param("currentTime") LocalDateTime currentTime);
    
    /**
     * 查找已过期的分享记录
     */
    @Query("SELECT s FROM ShareRecord s WHERE s.expireTime < :currentTime AND s.status = 'ACTIVE'")
    List<ShareRecord> findExpiredShares(@Param("currentTime") LocalDateTime currentTime);
    
    /**
     * 根据关联的文件查找分享记录
     */
    @Query("SELECT s FROM ShareRecord s WHERE s.file.id = :fileId")
    List<ShareRecord> findByFileId(@Param("fileId") Long fileId);
    
    /**
     * 根据关联的文件夹查找分享记录
     */
    @Query("SELECT s FROM ShareRecord s WHERE s.folder.id = :folderId")
    List<ShareRecord> findByFolderId(@Param("folderId") Long folderId);
    
    /**
     * 统计用户的分享数量
     */
    @Query("SELECT COUNT(s) FROM ShareRecord s WHERE s.sharer = :sharer")
    Long countBySharer(@Param("sharer") User sharer);
    
    /**
     * 统计特定时间范围内的分享数量
     */
    @Query("SELECT COUNT(s) FROM ShareRecord s WHERE s.createdAt BETWEEN :startTime AND :endTime")
    Long countSharesBetween(@Param("startTime") LocalDateTime startTime, 
                           @Param("endTime") LocalDateTime endTime);
    
    /**
     * 查找访问次数最多的分享记录
     */
    @Query("SELECT s FROM ShareRecord s ORDER BY s.currentAccessCount DESC")
    Page<ShareRecord> findMostAccessedShares(Pageable pageable);
}