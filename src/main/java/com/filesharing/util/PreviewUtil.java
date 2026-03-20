package com.filesharing.util;

import lombok.extern.slf4j.Slf4j;
import org.apache.tika.Tika;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.sax.BodyContentHandler;
import org.springframework.stereotype.Component;

import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.file.Path;

/**
 * 文件预览工具类
 */
@Slf4j
@Component
public class PreviewUtil {
    
    private final Tika tika = new Tika();
    private final AutoDetectParser parser = new AutoDetectParser();
    
    /**
     * 检测文件类型
     */
    public String detectFileType(Path filePath) {
        try (InputStream inputStream = new FileInputStream(filePath.toFile())) {
            return tika.detect(inputStream);
        } catch (Exception e) {
            log.error("检测文件类型失败: {}", e.getMessage());
            return "application/octet-stream";
        }
    }
    
    /**
     * 检测文件类型（通过字节数组）
     */
    public String detectFileType(byte[] fileBytes) {
        try {
            return tika.detect(fileBytes);
        } catch (Exception e) {
            log.error("检测文件类型失败: {}", e.getMessage());
            return "application/octet-stream";
        }
    }
    
    /**
     * 提取文件元数据
     */
    public Metadata extractMetadata(Path filePath) {
        try (InputStream inputStream = new FileInputStream(filePath.toFile())) {
            Metadata metadata = new Metadata();
            BodyContentHandler handler = new BodyContentHandler(-1);
            ParseContext context = new ParseContext();
            
            parser.parse(inputStream, handler, metadata, context);
            return metadata;
        } catch (Exception e) {
            log.error("提取文件元数据失败: {}", e.getMessage());
            return new Metadata();
        }
    }
    
    /**
     * 判断文件是否支持预览
     */
    public boolean isPreviewSupported(String contentType, String fileExtension) {
        return isImage(contentType, fileExtension) ||
               isText(contentType, fileExtension) ||
               isPdf(contentType, fileExtension) ||
               isOfficeDocument(contentType, fileExtension) ||
               isAudio(contentType, fileExtension) ||
               isVideo(contentType, fileExtension);
    }
    
    /**
     * 判断是否为图片文件
     */
    public boolean isImage(String contentType, String fileExtension) {
        if (contentType != null && contentType.startsWith("image/")) {
            return true;
        }
        
        if (fileExtension != null) {
            String ext = fileExtension.toLowerCase();
            return ext.equals("jpg") || ext.equals("jpeg") || ext.equals("png") || 
                   ext.equals("gif") || ext.equals("bmp") || ext.equals("webp") ||
                   ext.equals("svg") || ext.equals("tiff");
        }
        
        return false;
    }
    
    /**
     * 判断是否为文本文件
     */
    public boolean isText(String contentType, String fileExtension) {
        if (contentType != null) {
            return contentType.startsWith("text/") || 
                   contentType.equals("application/json") ||
                   contentType.equals("application/xml") ||
                   contentType.equals("application/javascript");
        }
        
        if (fileExtension != null) {
            String ext = fileExtension.toLowerCase();
            return ext.equals("txt") || ext.equals("log") || ext.equals("md") ||
                   ext.equals("csv") || ext.equals("json") || ext.equals("xml") ||
                   ext.equals("html") || ext.equals("css") || ext.equals("js") ||
                   ext.equals("sql") || ext.equals("properties") || ext.equals("yml") ||
                   ext.equals("yaml") || ext.equals("ini") || ext.equals("conf");
        }
        
        return false;
    }
    
    /**
     * 判断是否为PDF文件
     */
    public boolean isPdf(String contentType, String fileExtension) {
        if (contentType != null && contentType.equals("application/pdf")) {
            return true;
        }
        
        if (fileExtension != null) {
            return fileExtension.toLowerCase().equals("pdf");
        }
        
        return false;
    }
    
    /**
     * 判断是否为Office文档
     */
    public boolean isOfficeDocument(String contentType, String fileExtension) {
        if (contentType != null) {
            return contentType.equals("application/msword") ||
                   contentType.equals("application/vnd.openxmlformats-officedocument.wordprocessingml.document") ||
                   contentType.equals("application/vnd.ms-excel") ||
                   contentType.equals("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet") ||
                   contentType.equals("application/vnd.ms-powerpoint") ||
                   contentType.equals("application/vnd.openxmlformats-officedocument.presentationml.presentation");
        }
        
        if (fileExtension != null) {
            String ext = fileExtension.toLowerCase();
            return ext.equals("doc") || ext.equals("docx") || ext.equals("xls") ||
                   ext.equals("xlsx") || ext.equals("ppt") || ext.equals("pptx");
        }
        
        return false;
    }
    
    /**
     * 判断是否为音频文件
     */
    public boolean isAudio(String contentType, String fileExtension) {
        if (contentType != null && contentType.startsWith("audio/")) {
            return true;
        }
        
        if (fileExtension != null) {
            String ext = fileExtension.toLowerCase();
            return ext.equals("mp3") || ext.equals("wav") || ext.equals("flac") ||
                   ext.equals("aac") || ext.equals("ogg") || ext.equals("wma");
        }
        
        return false;
    }
    
    /**
     * 判断是否为视频文件
     */
    public boolean isVideo(String contentType, String fileExtension) {
        if (contentType != null && contentType.startsWith("video/")) {
            return true;
        }
        
        if (fileExtension != null) {
            String ext = fileExtension.toLowerCase();
            return ext.equals("mp4") || ext.equals("avi") || ext.equals("mkv") ||
                   ext.equals("mov") || ext.equals("wmv") || ext.equals("flv") ||
                   ext.equals("webm") || ext.equals("m4v");
        }
        
        return false;
    }
    
    /**
     * 获取预览类型字符串
     */
    public String getPreviewType(String contentType, String fileExtension) {
        if (isImage(contentType, fileExtension)) {
            return "IMAGE";
        } else if (isText(contentType, fileExtension)) {
            return "TEXT";
        } else if (isPdf(contentType, fileExtension)) {
            return "PDF";
        } else if (isOfficeDocument(contentType, fileExtension)) {
            return "OFFICE";
        } else if (isAudio(contentType, fileExtension)) {
            return "AUDIO";
        } else if (isVideo(contentType, fileExtension)) {
            return "VIDEO";
        } else {
            return "OTHER";
        }
    }
    
    /**
     * 获取文件扩展名
     */
    public String getFileExtension(String fileName) {
        if (fileName == null || fileName.lastIndexOf('.') == -1) {
            return "";
        }
        return fileName.substring(fileName.lastIndexOf('.') + 1);
    }
    
    /**
     * 获取不带扩展名的文件名
     */
    public String getFileNameWithoutExtension(String fileName) {
        if (fileName == null || fileName.lastIndexOf('.') == -1) {
            return fileName;
        }
        return fileName.substring(0, fileName.lastIndexOf('.'));
    }
}