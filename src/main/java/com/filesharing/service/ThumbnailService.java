package com.filesharing.service;

import com.filesharing.entity.FileEntity;
import com.filesharing.entity.Thumbnail;
import org.springframework.core.io.Resource;

import java.util.List;

/**
 * 缩略图服务接口
 */
public interface ThumbnailService {
    
    /**
     * 为文件生成缩略图
     */
    void generateThumbnails(FileEntity file);
    
    /**
     * 生成图片缩略图
     */
    Thumbnail generateImageThumbnail(FileEntity file, Thumbnail.SizeSpec sizeSpec);
    
    /**
     * 生成视频关键帧缩略图
     */
    Thumbnail generateVideoThumbnail(FileEntity file, Thumbnail.SizeSpec sizeSpec);
    
    /**
     * 生成PDF文档预览缩略图
     */
    List<Thumbnail> generatePdfThumbnails(FileEntity file, int maxPages);
    
    /**
     * 生成Office文档预览缩略图
     */
    Thumbnail generateOfficeThumbnail(FileEntity file, Thumbnail.SizeSpec sizeSpec);
    
    /**
     * 获取文件的缩略图
     */
    Resource getThumbnail(Long fileId, Thumbnail.SizeSpec sizeSpec);
    
    /**
     * 获取文件的所有缩略图
     */
    List<ThumbnailInfo> getFileThumbnails(Long fileId);
    
    /**
     * 批量生成缩略图
     */
    void batchGenerateThumbnails(List<FileEntity> files);
    
    /**
     * 重新生成失败的缩略图
     */
    void regenerateFailedThumbnails();
    
    /**
     * 清理无效的缩略图
     */
    void cleanupInvalidThumbnails();
    
    /**
     * 获取缩略图生成统计
     */
    ThumbnailStatistics getThumbnailStatistics();
    
    /**
     * 异步生成缩略图
     */
    void generateThumbnailsAsync(FileEntity file);
    
    /**
     * 缩略图信息DTO
     */
    class ThumbnailInfo {
        private Long id;
        private String url;
        private Thumbnail.SizeSpec sizeSpec;
        private Integer width;
        private Integer height;
        private Long fileSize;
        private String contentType;
        private Thumbnail.GenerationStatus status;
        
        // 构造函数
        public ThumbnailInfo() {}
        
        public ThumbnailInfo(Long id, String url, Thumbnail.SizeSpec sizeSpec, 
                           Integer width, Integer height, Long fileSize, 
                           String contentType, Thumbnail.GenerationStatus status) {
            this.id = id;
            this.url = url;
            this.sizeSpec = sizeSpec;
            this.width = width;
            this.height = height;
            this.fileSize = fileSize;
            this.contentType = contentType;
            this.status = status;
        }
        
        // getters and setters
        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        
        public String getUrl() { return url; }
        public void setUrl(String url) { this.url = url; }
        
        public Thumbnail.SizeSpec getSizeSpec() { return sizeSpec; }
        public void setSizeSpec(Thumbnail.SizeSpec sizeSpec) { this.sizeSpec = sizeSpec; }
        
        public Integer getWidth() { return width; }
        public void setWidth(Integer width) { this.width = width; }
        
        public Integer getHeight() { return height; }
        public void setHeight(Integer height) { this.height = height; }
        
        public Long getFileSize() { return fileSize; }
        public void setFileSize(Long fileSize) { this.fileSize = fileSize; }
        
        public String getContentType() { return contentType; }
        public void setContentType(String contentType) { this.contentType = contentType; }
        
        public Thumbnail.GenerationStatus getStatus() { return status; }
        public void setStatus(Thumbnail.GenerationStatus status) { this.status = status; }
    }
    
    /**
     * 缩略图统计信息
     */
    class ThumbnailStatistics {
        private Long totalThumbnails;
        private Long successfulCount;
        private Long failedCount;
        private Long pendingCount;
        private Long processingCount;
        private Double successRate;
        private List<TypeStat> typeStats;
        private List<SizeStat> sizeStats;
        
        // 构造函数
        public ThumbnailStatistics() {}
        
        public ThumbnailStatistics(Long totalThumbnails, Long successfulCount, Long failedCount) {
            this.totalThumbnails = totalThumbnails;
            this.successfulCount = successfulCount;
            this.failedCount = failedCount;
            this.pendingCount = totalThumbnails - successfulCount - failedCount;
            this.successRate = totalThumbnails > 0 ? (double) successfulCount / totalThumbnails * 100 : 0.0;
        }
        
        // 内部类：类型统计
        public static class TypeStat {
            private String type;
            private Long count;
            private Double percentage;
            
            public TypeStat() {}
            
            public TypeStat(String type, Long count, Double percentage) {
                this.type = type;
                this.count = count;
                this.percentage = percentage;
            }
            
            // getters and setters
            public String getType() { return type; }
            public void setType(String type) { this.type = type; }
            
            public Long getCount() { return count; }
            public void setCount(Long count) { this.count = count; }
            
            public Double getPercentage() { return percentage; }
            public void setPercentage(Double percentage) { this.percentage = percentage; }
        }
        
        // 内部类：尺寸统计
        public static class SizeStat {
            private String sizeSpec;
            private Long count;
            private Double percentage;
            
            public SizeStat() {}
            
            public SizeStat(String sizeSpec, Long count, Double percentage) {
                this.sizeSpec = sizeSpec;
                this.count = count;
                this.percentage = percentage;
            }
            
            // getters and setters
            public String getSizeSpec() { return sizeSpec; }
            public void setSizeSpec(String sizeSpec) { this.sizeSpec = sizeSpec; }
            
            public Long getCount() { return count; }
            public void setCount(Long count) { this.count = count; }
            
            public Double getPercentage() { return percentage; }
            public void setPercentage(Double percentage) { this.percentage = percentage; }
        }
        
        // getters and setters
        public Long getTotalThumbnails() { return totalThumbnails; }
        public void setTotalThumbnails(Long totalThumbnails) { this.totalThumbnails = totalThumbnails; }
        
        public Long getSuccessfulCount() { return successfulCount; }
        public void setSuccessfulCount(Long successfulCount) { this.successfulCount = successfulCount; }
        
        public Long getFailedCount() { return failedCount; }
        public void setFailedCount(Long failedCount) { this.failedCount = failedCount; }
        
        public Long getPendingCount() { return pendingCount; }
        public void setPendingCount(Long pendingCount) { this.pendingCount = pendingCount; }
        
        public Long getProcessingCount() { return processingCount; }
        public void setProcessingCount(Long processingCount) { this.processingCount = processingCount; }
        
        public Double getSuccessRate() { return successRate; }
        public void setSuccessRate(Double successRate) { this.successRate = successRate; }
        
        public List<TypeStat> getTypeStats() { return typeStats; }
        public void setTypeStats(List<TypeStat> typeStats) { this.typeStats = typeStats; }
        
        public List<SizeStat> getSizeStats() { return sizeStats; }
        public void setSizeStats(List<SizeStat> sizeStats) { this.sizeStats = sizeStats; }
    }
}