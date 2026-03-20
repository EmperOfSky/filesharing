package com.filesharing.ai.util;

import lombok.extern.slf4j.Slf4j;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.*;

import javax.imageio.ImageIO;

/**
 * 图像处理工具类
 * 提供图像分析和处理相关功能
 */
@Slf4j
public class ImageUtils {
    
    // 常见图像格式
    private static final Set<String> SUPPORTED_FORMATS = new HashSet<>(
        Arrays.asList("jpg", "jpeg", "png", "gif", "bmp", "webp")
    );
    
    /**
     * 检查图像格式是否支持
     */
    public static boolean isSupportedFormat(String format) {
        return format != null && SUPPORTED_FORMATS.contains(format.toLowerCase());
    }
    
    /**
     * 获取图像基本信息
     */
    public static Map<String, Object> getImageInfo(byte[] imageData) {
        Map<String, Object> info = new HashMap<>();
        
        try {
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(imageData));
            if (image != null) {
                info.put("width", image.getWidth());
                info.put("height", image.getHeight());
                info.put("format", "unknown"); // 需要额外库来检测格式
                info.put("colorModel", image.getColorModel().getClass().getSimpleName());
                info.put("pixelSize", image.getWidth() * image.getHeight());
                info.put("aspectRatio", (double) image.getWidth() / image.getHeight());
            }
        } catch (IOException e) {
            log.error("读取图像信息失败", e);
            info.put("error", "无法读取图像信息");
        }
        
        return info;
    }
    
    /**
     * 计算图像直方图
     */
    public static Map<String, int[]> calculateHistogram(byte[] imageData) {
        Map<String, int[]> histogram = new HashMap<>();
        
        try {
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(imageData));
            if (image != null) {
                int[] redHist = new int[256];
                int[] greenHist = new int[256];
                int[] blueHist = new int[256];
                int[] grayHist = new int[256];
                
                for (int y = 0; y < image.getHeight(); y++) {
                    for (int x = 0; x < image.getWidth(); x++) {
                        int rgb = image.getRGB(x, y);
                        int red = (rgb >> 16) & 0xFF;
                        int green = (rgb >> 8) & 0xFF;
                        int blue = rgb & 0xFF;
                        int gray = (red + green + blue) / 3;
                        
                        redHist[red]++;
                        greenHist[green]++;
                        blueHist[blue]++;
                        grayHist[gray]++;
                    }
                }
                
                histogram.put("red", redHist);
                histogram.put("green", greenHist);
                histogram.put("blue", blueHist);
                histogram.put("gray", grayHist);
            }
        } catch (IOException e) {
            log.error("计算图像直方图失败", e);
        }
        
        return histogram;
    }
    
    /**
     * 检测图像模糊度
     */
    public static double detectBlur(byte[] imageData) {
        try {
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(imageData));
            if (image != null) {
                // 简化的模糊度检测（拉普拉斯算子变体）
                double variance = 0.0;
                int count = 0;
                
                for (int y = 1; y < image.getHeight() - 1; y++) {
                    for (int x = 1; x < image.getWidth() - 1; x++) {
                        int center = getGrayValue(image, x, y);
                        int laplacian = Math.abs(
                            4 * center - 
                            getGrayValue(image, x-1, y) - 
                            getGrayValue(image, x+1, y) - 
                            getGrayValue(image, x, y-1) - 
                            getGrayValue(image, x, y+1)
                        );
                        variance += laplacian * laplacian;
                        count++;
                    }
                }
                
                return count > 0 ? variance / count : 0.0;
            }
        } catch (IOException e) {
            log.error("模糊度检测失败", e);
        }
        
        return 0.0;
    }
    
    /**
     * 评估图像质量
     */
    public static Map<String, Object> assessImageQuality(byte[] imageData) {
        Map<String, Object> quality = new HashMap<>();
        
        try {
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(imageData));
            if (image != null) {
                // 锐度评估
                double sharpness = detectBlur(imageData);
                quality.put("sharpness", normalizeValue(sharpness, 0, 10000, 0, 1));
                
                // 亮度评估
                double brightness = calculateAverageBrightness(image);
                quality.put("brightness", brightness);
                
                // 对比度评估
                double contrast = calculateContrast(image);
                quality.put("contrast", contrast);
                
                // 质量判断
                quality.put("isBlurry", sharpness < 100);
                quality.put("isOverexposed", brightness > 0.8);
                quality.put("isUnderexposed", brightness < 0.2);
            }
        } catch (IOException e) {
            log.error("图像质量评估失败", e);
            quality.put("error", "评估失败");
        }
        
        return quality;
    }
    
    /**
     * 提取主要颜色
     */
    public static List<Map<String, Object>> extractDominantColors(byte[] imageData, int maxColors) {
        List<Map<String, Object>> colors = new ArrayList<>();
        
        try {
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(imageData));
            if (image != null) {
                Map<Integer, Integer> colorCount = new HashMap<>();
                
                // 统计颜色出现次数
                for (int y = 0; y < image.getHeight(); y++) {
                    for (int x = 0; x < image.getWidth(); x++) {
                        int rgb = image.getRGB(x, y) & 0xFFFFFF; // 去除alpha通道
                        colorCount.put(rgb, colorCount.getOrDefault(rgb, 0) + 1);
                    }
                }
                
                // 按出现次数排序
                colorCount.entrySet().stream()
                    .sorted(Map.Entry.<Integer, Integer>comparingByValue().reversed())
                    .limit(maxColors)
                    .forEach(entry -> {
                        Map<String, Object> colorInfo = new HashMap<>();
                        int rgb = entry.getKey();
                        String hex = String.format("#%06X", rgb);
                        colorInfo.put("hexCode", hex);
                        colorInfo.put("red", (rgb >> 16) & 0xFF);
                        colorInfo.put("green", (rgb >> 8) & 0xFF);
                        colorInfo.put("blue", rgb & 0xFF);
                        colorInfo.put("count", entry.getValue());
                        colorInfo.put("percentage", (double) entry.getValue() / (image.getWidth() * image.getHeight()) * 100);
                        colors.add(colorInfo);
                    });
            }
        } catch (IOException e) {
            log.error("颜色提取失败", e);
        }
        
        return colors;
    }
    
    // 私有辅助方法
    
    private static int getGrayValue(BufferedImage image, int x, int y) {
        int rgb = image.getRGB(x, y);
        return (rgb & 0xFF) + ((rgb >> 8) & 0xFF) + ((rgb >> 16) & 0xFF);
    }
    
    private static double calculateAverageBrightness(BufferedImage image) {
        long sum = 0;
        int count = 0;
        
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                int rgb = image.getRGB(x, y);
                int gray = ((rgb >> 16) & 0xFF + (rgb >> 8) & 0xFF + rgb & 0xFF) / 3;
                sum += gray;
                count++;
            }
        }
        
        return count > 0 ? (double) sum / (count * 255) : 0.0;
    }
    
    private static double calculateContrast(BufferedImage image) {
        long sum = 0;
        int count = 0;
        double avgBrightness = calculateAverageBrightness(image) * 255;
        
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                int rgb = image.getRGB(x, y);
                int gray = ((rgb >> 16) & 0xFF + (rgb >> 8) & 0xFF + rgb & 0xFF) / 3;
                sum += Math.pow(gray - avgBrightness, 2);
                count++;
            }
        }
        
        return count > 0 ? Math.sqrt(sum / count) / 127.5 : 0.0;
    }
    
    private static double normalizeValue(double value, double min, double max, double targetMin, double targetMax) {
        return targetMin + (value - min) * (targetMax - targetMin) / (max - min);
    }
}