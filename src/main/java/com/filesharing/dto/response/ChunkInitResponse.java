package com.filesharing.dto.response;

import lombok.Builder;
import lombok.Data;

/**
 * 分片上传初始化响应
 */
@Data
@Builder
public class ChunkInitResponse {
    
    /**
     * 上传ID
     */
    private String uploadId;
    
    /**
     * 是否秒传（如果文件已存在）
     */
    private Boolean isFastTransfer;
    
    /**
     * 秒传时返回的文件信息
     */
    private FileSimpleResponse fileInfo;
    
    /**
     * 消息
     */
    private String message;
}