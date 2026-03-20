package com.filesharing.ai.image;

import lombok.Data;
import java.util.List;

/**
 * 图像分析结果
 */
@Data
public class ImageAnalysisResult {
    
    /**
     * 分析是否成功
     */
    private Boolean success;
    
    /**
     * 错误信息
     */
    private String error;
    
    /**
     * 图像类型
     */
    private String imageType;
    
    /**
     * 图像尺寸
     */
    private ImageDimensions dimensions;
    
    /**
     * 主要颜色
     */
    private List<ColorInfo> dominantColors;
    
    /**
     * 检测到的对象
     */
    private List<DetectedObject> objects;
    
    /**
     * 场景识别结果
     */
    private SceneRecognitionResult scene;
    
    /**
     * OCR识别结果
     */
    private OcrResult ocr;
    
    /**
     * 图像质量评估
     */
    private QualityAssessment quality;
    
    /**
     * 推荐标签
     */
    private List<String> recommendedTags;
    
    /**
     * 图像尺寸信息
     */
    @Data
    public static class ImageDimensions {
        private Integer width;
        private Integer height;
    }
    
    /**
     * 颜色信息
     */
    @Data
    public static class ColorInfo {
        private String colorName;
        private String hexCode;
        private Double percentage;
    }
    
    /**
     * 检测到的对象
     */
    @Data
    public static class DetectedObject {
        private String className;
        private Double confidence;
        private BoundingBox boundingBox;
        
        @Data
        public static class BoundingBox {
            private Double x;
            private Double y;
            private Double width;
            private Double height;
        }
    }
    
    /**
     * 场景识别结果
     */
    @Data
    public static class SceneRecognitionResult {
        private String sceneType;
        private Double confidence;
        private List<String> attributes;
    }
    
    /**
     * OCR识别结果
     */
    @Data
    public static class OcrResult {
        private String extractedText;
        private List<TextBlock> textBlocks;
        
        @Data
        public static class TextBlock {
            private String text;
            private BoundingBox boundingBox;
            private Double confidence;
            
            @Data
            public static class BoundingBox {
                private Double x;
                private Double y;
                private Double width;
                private Double height;
            }
        }
    }
    
    /**
     * 图像质量评估
     */
    @Data
    public static class QualityAssessment {
        private Double sharpness;
        private Double brightness;
        private Double contrast;
        private Boolean isBlurry;
        private Boolean isOverexposed;
        private Boolean isUnderexposed;
    }
}