package com.filesharing.dto.request;

import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

/**
 * 分片上传请求
 */
@Data
public class ChunkUploadRequest {
    
    /**
     * 上传ID
     */
    private String uploadId;
    
    /**
     * 分片索引
     */
    private Integer chunkIndex;
    
    /**
     * 总分片数
     */
    private Integer totalChunks;
    
    /**
     * 分片文件
     */
    private MultipartFile chunk;
}