package com.filesharing.util;

import lombok.extern.slf4j.Slf4j;
import net.coobird.thumbnailator.Thumbnails;
import net.coobird.thumbnailator.geometry.Positions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

/**
 * 缩略图生成工具类
 */
@Slf4j
@Component
public class ThumbnailGenerator {
    
    @Value("${file.thumbnail.path:./thumbnails/}")
    private String thumbnailPath;
    
    private static final Color WATERMARK_COLOR = new Color(0, 0, 0, 100); // 半透明黑色
    
    /**
     * 初始化缩略图目录
     */
    public void initThumbnailDirectory() {
        try {
            Path path = Paths.get(thumbnailPath);
            if (!Files.exists(path)) {
                Files.createDirectories(path);
                log.info("创建缩略图目录: {}", thumbnailPath);
            }
        } catch (IOException e) {
            log.error("创建缩略图目录失败: {}", e.getMessage());
        }
    }
    
    /**
     * 生成图片缩略图
     */
    public byte[] generateImageThumbnail(byte[] imageBytes, int width, int height, boolean keepAspectRatio) {
        try {
            BufferedImage originalImage = ImageIO.read(new java.io.ByteArrayInputStream(imageBytes));
            if (originalImage == null) {
                throw new IOException("无法读取图片数据");
            }
            
            BufferedImage thumbnail;
            if (keepAspectRatio) {
                thumbnail = Thumbnails.of(originalImage)
                        .size(width, height)
                        .keepAspectRatio(true)
                        .crop(Positions.CENTER)
                        .asBufferedImage();
            } else {
                thumbnail = Thumbnails.of(originalImage)
                        .size(width, height)
                        .keepAspectRatio(false)
                        .asBufferedImage();
            }
            
            // 添加水印（可选）
            thumbnail = addWatermark(thumbnail, "Preview");
            
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(thumbnail, "JPEG", baos);
            return baos.toByteArray();
            
        } catch (IOException e) {
            log.error("生成图片缩略图失败: {}", e.getMessage());
            throw new RuntimeException("生成缩略图失败", e);
        }
    }
    
    /**
     * 生成指定尺寸的缩略图
     */
    public byte[] generateThumbnail(byte[] imageBytes, String sizeSpec) {
        int[] dimensions = parseSizeSpec(sizeSpec);
        return generateImageThumbnail(imageBytes, dimensions[0], dimensions[1], true);
    }
    
    /**
     * 解析尺寸规格
     */
    private int[] parseSizeSpec(String sizeSpec) {
        switch (sizeSpec.toUpperCase()) {
            case "SMALL":
                return new int[]{128, 128};
            case "MEDIUM":
                return new int[]{256, 256};
            case "LARGE":
                return new int[]{512, 512};
            default:
                // 格式: WIDTHxHEIGHT
                String[] parts = sizeSpec.split("x");
                if (parts.length == 2) {
                    try {
                        return new int[]{Integer.parseInt(parts[0]), Integer.parseInt(parts[1])};
                    } catch (NumberFormatException e) {
                        log.warn("无效的尺寸规格: {}, 使用默认尺寸", sizeSpec);
                    }
                }
                return new int[]{256, 256}; // 默认中等尺寸
        }
    }
    
    /**
     * 添加水印
     */
    private BufferedImage addWatermark(BufferedImage image, String watermarkText) {
        Graphics2D g2d = image.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setColor(WATERMARK_COLOR);
        g2d.setFont(new Font("Arial", Font.BOLD, 12));
        
        FontMetrics fontMetrics = g2d.getFontMetrics();
        int textWidth = fontMetrics.stringWidth(watermarkText);
        
        int x = image.getWidth() - textWidth - 10;
        int y = image.getHeight() - 10;
        
        g2d.drawString(watermarkText, x, y);
        g2d.dispose();
        
        return image;
    }
    
    /**
     * 生成文件缩略图并保存到磁盘
     */
    public String generateAndSaveThumbnail(byte[] fileBytes, String originalFileName, 
                                         String sizeSpec, String contentType) {
        try {
            byte[] thumbnailBytes = generateThumbnail(fileBytes, sizeSpec);
            
            // 生成唯一的缩略图文件名
            String fileExtension = getFileExtension(originalFileName);
            String thumbnailFileName = UUID.randomUUID().toString() + "_thumb_" + sizeSpec + "." + fileExtension;
            Path thumbnailPathObj = Paths.get(thumbnailPath, thumbnailFileName);
            
            // 保存文件
            Files.write(thumbnailPathObj, thumbnailBytes);
            
            log.info("缩略图生成成功: {}", thumbnailFileName);
            return thumbnailPathObj.toString();
            
        } catch (Exception e) {
            log.error("保存缩略图失败: {}", e.getMessage());
            throw new RuntimeException("保存缩略图失败", e);
        }
    }
    
    /**
     * 获取文件扩展名
     */
    private String getFileExtension(String fileName) {
        if (fileName == null || fileName.lastIndexOf('.') == -1) {
            return "jpg";
        }
        String extension = fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase();
        // 确保是支持的图片格式
        switch (extension) {
            case "png":
            case "gif":
            case "bmp":
            case "webp":
                return extension;
            default:
                return "jpg"; // 默认JPEG格式
        }
    }
    
    /**
     * 清理旧的缩略图文件
     */
    public void cleanupOldThumbnails(int daysOld) {
        try {
            Path thumbPath = Paths.get(thumbnailPath);
            if (!Files.exists(thumbPath)) {
                return;
            }
            
            Files.walk(thumbPath)
                    .filter(Files::isRegularFile)
                    .filter(path -> {
                        try {
                            return Files.getLastModifiedTime(path).toMillis() < 
                                   System.currentTimeMillis() - (daysOld * 24L * 60 * 60 * 1000);
                        } catch (IOException e) {
                            return false;
                        }
                    })
                    .forEach(path -> {
                        try {
                            Files.delete(path);
                            log.info("删除旧缩略图: {}", path.getFileName());
                        } catch (IOException e) {
                            log.warn("删除缩略图失败: {}", path.getFileName());
                        }
                    });
                    
        } catch (IOException e) {
            log.error("清理旧缩略图失败: {}", e.getMessage());
        }
    }
    
    /**
     * 验证缩略图目录是否存在且可写
     */
    public boolean isThumbnailDirectoryWritable() {
        try {
            Path path = Paths.get(thumbnailPath);
            return Files.exists(path) && Files.isWritable(path);
        } catch (Exception e) {
            log.error("检查缩略图目录失败: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * 获取缩略图目录路径
     */
    public String getThumbnailPath() {
        return thumbnailPath;
    }
    
    /**
     * 设置缩略图目录路径
     */
    public void setThumbnailPath(String thumbnailPath) {
        this.thumbnailPath = thumbnailPath;
    }
}