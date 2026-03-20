package com.filesharing.dto.request;

import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

/**
 * 分片上传初始化请求
 */
@Data
public class ChunkInitRequest {
    
    /**
     * 文件名
     */
    private String fileName;
    
    /**
     * 文件总大小
     */
    private Long fileSize;
    
    /**
     * 分片大小
     */
    private Long chunkSize;
    
    /**
     * 总分片数
     */
    private Integer totalChunks;
    
    /**
     * 目标文件夹ID
     */
    private Long folderId;
    
    /**
     * 文件描述
     */
    private String description;
}