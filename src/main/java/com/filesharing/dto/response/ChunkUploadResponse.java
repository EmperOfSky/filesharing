package com.filesharing.dto.response;

import lombok.Builder;
import lombok.Data;

/**
 * 分片上传响应
 */
@Data
@Builder
public class ChunkUploadResponse {
    
    /**
     * 上传ID
     */
    private String uploadId;
    
    /**
     * 分片索引
     */
    private Integer chunkIndex;
    
    /**
     * 是否上传成功
     */
    private Boolean success;
    
    /**
     * 已上传分片数
     */
    private Integer uploadedChunks;
    
    /**
     * 总分片数
     */
    private Integer totalChunks;
    
    /**
     * 上传进度百分比
     */
    private Double progress;
    
    /**
     * 消息
     */
    private String message;
}