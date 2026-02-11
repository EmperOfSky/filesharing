package com.filesharing.repository;

import com.filesharing.entity.FileVersion;
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
 * 文件版本Repository接口
 */
@Repository
public interface FileVersionRepository extends JpaRepository<FileVersion, Long> {
    
    /**
     * 根据文件查找所有版本（按版本号降序）
     */
    @Query("SELECT fv FROM FileVersion fv WHERE fv.file = :file AND fv.isDeleted = false " +
           "ORDER BY fv.versionNumber DESC")
    Page<FileVersion> findByFile(@Param("file") FileEntity file, Pageable pageable);
    
    /**
     * 根据文件查找当前版本
     */
    @Query("SELECT fv FROM FileVersion fv WHERE fv.file = :file AND fv.isCurrent = true " +
           "AND fv.isDeleted = false")
    Optional<FileVersion> findCurrentVersion(@Param("file") FileEntity file);
    
    /**
     * 根据文件和版本号查找版本
     */
    @Query("SELECT fv FROM FileVersion fv WHERE fv.file = :file AND fv.versionNumber = :versionNumber " +
           "AND fv.isDeleted = false")
    Optional<FileVersion> findByFileAndVersionNumber(@Param("file") FileEntity file, 
                                                    @Param("versionNumber") Integer versionNumber);
    
    /**
     * 查找文件的最大版本号
     */
    @Query("SELECT MAX(fv.versionNumber) FROM FileVersion fv WHERE fv.file = :file " +
           "AND fv.isDeleted = false")
    Integer findMaxVersionNumber(@Param("file") FileEntity file);
    
    /**
     * 查找指定标签的版本
     */
    @Query("SELECT fv FROM FileVersion fv WHERE fv.file = :file AND fv.versionTag = :tag " +
           "AND fv.isDeleted = false ORDER BY fv.versionNumber DESC")
    Page<FileVersion> findByFileAndVersionTag(@Param("file") FileEntity file, 
                                             @Param("tag") String tag, 
                                             Pageable pageable);
    
    /**
     * 查找用户修改的版本
     */
    @Query("SELECT fv FROM FileVersion fv WHERE fv.modifiedBy = :user AND fv.isDeleted = false " +
           "ORDER BY fv.modifiedAt DESC")
    Page<FileVersion> findByModifiedBy(@Param("user") User user, Pageable pageable);
    
    /**
     * 统计文件的版本数量
     */
    @Query("SELECT COUNT(fv) FROM FileVersion fv WHERE fv.file = :file AND fv.isDeleted = false")
    Long countVersionsByFile(@Param("file") FileEntity file);
    
    /**
     * 查找需要清理的旧版本
     */
    @Query("SELECT fv FROM FileVersion fv WHERE fv.isDeleted = true " +
           "AND fv.deletedAt < :beforeTime")
    List<FileVersion> findDeletedVersionsBefore(@Param("beforeTime") LocalDateTime beforeTime);
    
    /**
     * 获取版本统计信息
     */
    @Query("SELECT fv.versionTag, COUNT(fv) FROM FileVersion fv WHERE fv.file = :file " +
           "AND fv.isDeleted = false GROUP BY fv.versionTag")
    List<Object[]> getVersionTagStats(@Param("file") FileEntity file);
    
    /**
     * 查找指定时间范围内的版本
     */
    @Query("SELECT fv FROM FileVersion fv WHERE fv.file = :file " +
           "AND fv.modifiedAt BETWEEN :startTime AND :endTime " +
           "AND fv.isDeleted = false ORDER BY fv.versionNumber DESC")
    Page<FileVersion> findByFileAndModifiedAtBetween(@Param("file") FileEntity file,
                                                    @Param("startTime") LocalDateTime startTime,
                                                    @Param("endTime") LocalDateTime endTime,
                                                    Pageable pageable);
    
    /**
     * 查找具有相同MD5的版本（用于去重）
     */
    @Query("SELECT fv FROM FileVersion fv WHERE fv.md5Hash = :md5Hash AND fv.isDeleted = false")
    List<FileVersion> findByMd5Hash(@Param("md5Hash") String md5Hash);
}