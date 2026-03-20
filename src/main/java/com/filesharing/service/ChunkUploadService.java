package com.filesharing.service;

import com.filesharing.dto.request.ChunkInitRequest;
import com.filesharing.dto.request.ChunkUploadRequest;
import com.filesharing.dto.response.ChunkInitResponse;
import com.filesharing.dto.response.ChunkUploadResponse;
import com.filesharing.entity.User;

/**
 * 分片上传服务接口
 */
public interface ChunkUploadService {
    
    /**
     * 初始化分片上传
     */
    ChunkInitResponse initChunkUpload(ChunkInitRequest request, User uploader);
    
    /**
     * 上传分片
     */
    ChunkUploadResponse uploadChunk(ChunkUploadRequest request, User uploader);
    
    /**
     * 获取上传进度
     */
    ChunkUploadResponse getUploadProgress(String uploadId);
    
    /**
     * 取消上传
     */
    void cancelUpload(String uploadId, User user);
    
    /**
     * 清理过期的上传记录
     */
    void cleanupExpiredUploads();
}