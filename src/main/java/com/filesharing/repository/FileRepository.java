package com.filesharing.repository;

import com.filesharing.entity.FileEntity;
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
 * 文件Repository接口
 */
@Repository
public interface FileRepository extends JpaRepository<FileEntity, Long> {
    
    /**
     * 根据存储名称查找文件
     */
    Optional<FileEntity> findByStorageName(String storageName);
    
    /**
     * 根据上传者查找文件列表
     */
    Page<FileEntity> findByUploader(User uploader, Pageable pageable);
    
    /**
     * 根据上传者和文件状态查找文件
     */
    Page<FileEntity> findByUploaderAndStatus(User uploader, FileEntity.FileStatus status, Pageable pageable);
    
    /**
     * 根据上传者和原始文件名模糊查找
     */
    @Query("SELECT f FROM FileEntity f WHERE f.uploader = :uploader AND f.originalName LIKE %:fileName%")
    Page<FileEntity> findByUploaderAndOriginalNameContaining(@Param("uploader") User uploader, 
                                                             @Param("fileName") String fileName, 
                                                             Pageable pageable);
    
    /**
     * 根据MD5值查找文件（用于去重）
     */
    List<FileEntity> findByMd5Hash(String md5Hash);
    
    /**
     * 查找公开文件
     */
    @Query("SELECT f FROM FileEntity f WHERE f.isPublic = true AND f.status = 'AVAILABLE'")
    Page<FileEntity> findPublicFiles(Pageable pageable);
    
    /**
     * 根据文件扩展名查找文件
     */
    @Query("SELECT f FROM FileEntity f WHERE f.extension = :extension AND f.status = 'AVAILABLE'")
    Page<FileEntity> findByExtension(@Param("extension") String extension, Pageable pageable);
    
    /**
     * 查找最近上传的文件
     */
    @Query("SELECT f FROM FileEntity f WHERE f.createdAt >= :since ORDER BY f.createdAt DESC")
    Page<FileEntity> findRecentFiles(@Param("since") LocalDateTime since, Pageable pageable);
    
    /**
     * 统计用户的文件总大小
     */
    @Query("SELECT SUM(f.fileSize) FROM FileEntity f WHERE f.uploader = :uploader AND f.status = 'AVAILABLE'")
    Long sumFileSizeByUploader(@Param("uploader") User uploader);
    
    /**
     * 统计特定时间内上传的文件数量
     */
    @Query("SELECT COUNT(f) FROM FileEntity f WHERE f.createdAt BETWEEN :startTime AND :endTime")
    Long countFilesBetween(@Param("startTime") LocalDateTime startTime, 
                          @Param("endTime") LocalDateTime endTime);
    
    /**
     * 查找需要清理的已删除文件
     */
    @Query("SELECT f FROM FileEntity f WHERE f.status = 'DELETED' AND f.deletedAt < :beforeTime")
    List<FileEntity> findDeletedFilesBefore(@Param("beforeTime") LocalDateTime beforeTime);
}