package com.filesharing.repository;

import com.filesharing.entity.Thumbnail;
import com.filesharing.entity.FileEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 缩略图Repository接口
 */
@Repository
public interface ThumbnailRepository extends JpaRepository<Thumbnail, Long> {
    
    /**
     * 根据文件和尺寸规格查找缩略图
     */
    Optional<Thumbnail> findByFileAndSizeSpec(FileEntity file, Thumbnail.SizeSpec sizeSpec);
    
    /**
     * 根据文件查找所有缩略图
     */
    List<Thumbnail> findByFile(FileEntity file);
    
    /**
     * 根据文件和缩略图类型查找
     */
    @Query("SELECT t FROM Thumbnail t WHERE t.file = :file AND t.thumbnailType = :thumbnailType")
    List<Thumbnail> findByFileAndThumbnailType(@Param("file") FileEntity file, 
                                              @Param("thumbnailType") Thumbnail.ThumbnailType thumbnailType);
    
    /**
     * 查找指定状态的缩略图
     */
    @Query("SELECT t FROM Thumbnail t WHERE t.status = :status")
    List<Thumbnail> findByStatus(@Param("status") Thumbnail.GenerationStatus status);
    
    /**
     * 查找待处理的缩略图
     */
    @Query("SELECT t FROM Thumbnail t WHERE t.status = 'PENDING' ORDER BY t.createdAt ASC")
    List<Thumbnail> findPendingThumbnails();
    
    /**
     * 查找失败且需要重试的缩略图
     */
    @Query("SELECT t FROM Thumbnail t WHERE t.status = 'FAILED' AND t.retryCount < t.maxRetryCount")
    List<Thumbnail> findFailedRetriableThumbnails();
    
    /**
     * 统计文件的缩略图数量
     */
    @Query("SELECT COUNT(t) FROM Thumbnail t WHERE t.file = :file")
    Long countByFile(@Param("file") FileEntity file);
    
    /**
     * 检查文件是否已有指定尺寸的缩略图
     */
    @Query("SELECT CASE WHEN COUNT(t) > 0 THEN true ELSE false END FROM Thumbnail t " +
           "WHERE t.file = :file AND t.sizeSpec = :sizeSpec AND t.status = 'SUCCESS'")
    boolean existsSuccessfulThumbnail(@Param("file") FileEntity file, @Param("sizeSpec") Thumbnail.SizeSpec sizeSpec);
    
    /**
     * 查找需要清理的失败缩略图
     */
    @Query("SELECT t FROM Thumbnail t WHERE t.status = 'FAILED' AND t.retryCount >= t.maxRetryCount")
    List<Thumbnail> findUnrecoverableThumbnails();
    
    /**
     * 获取缩略图生成统计
     */
    @Query("SELECT t.status, COUNT(t) FROM Thumbnail t GROUP BY t.status")
    List<Object[]> getGenerationStatistics();
    
    /**
     * 获取各类型缩略图的数量统计
     */
    @Query("SELECT t.thumbnailType, COUNT(t) FROM Thumbnail t GROUP BY t.thumbnailType")
    List<Object[]> getTypeStatistics();
}