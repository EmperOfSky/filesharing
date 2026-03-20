package com.filesharing.repository;

import com.filesharing.entity.ChunkUploadRecord;
import com.filesharing.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 分片上传记录仓库接口
 */
@Repository
public interface ChunkUploadRecordRepository extends JpaRepository<ChunkUploadRecord, Long> {
    
    /**
     * 根据上传ID查找记录
     */
    Optional<ChunkUploadRecord> findByUploadId(String uploadId);
    
    /**
     * 根据用户查找上传记录
     */
    List<ChunkUploadRecord> findByUploaderOrderByCreatedAtDesc(User uploader);
    
    /**
     * 查找过期的上传记录
     */
    @Query("SELECT c FROM ChunkUploadRecord c WHERE c.expireTime < :currentTime AND c.status IN ('INITIALIZED', 'UPLOADING')")
    List<ChunkUploadRecord> findExpiredUploads(LocalDateTime currentTime);
    
    /**
     * 根据状态查找上传记录
     */
    List<ChunkUploadRecord> findByStatus(ChunkUploadRecord.UploadStatus status);
    
    /**
     * 统计用户的上传记录数量
     */
    long countByUploader(User uploader);
}