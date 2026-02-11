package com.filesharing.dto;

import lombok.Data;
import lombok.Builder;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

/**
 * 文件预览响应DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PreviewResponse {
    
    private Long fileId;
    private String fileName;
    private String fileType;
    private String previewType;
    private String previewUrl;
    private String content; // 文本内容或base64编码的图片
    private Long fileSize;
    private String contentType;
    private Boolean isPreviewSupported;
    private String previewMessage;
    private Integer pageCount; // PDF页数
    private Integer duration; // 音视频时长（秒）
    private String[] thumbnails; // 缩略图URL数组
    private Metadata metadata; // 文件元数据
    
    /**
     * 元数据内部类
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Metadata {
        private String author;
        private String title;
        private String subject;
        private String keywords;
        private String creationDate;
        private String modificationDate;
        private String producer;
        private String creator;
        private Integer width; // 图片宽度
        private Integer height; // 图片高度
        private String format; // 图片格式
        private String bitrate; // 音视频比特率
        private String codec; // 编码格式
    }
}