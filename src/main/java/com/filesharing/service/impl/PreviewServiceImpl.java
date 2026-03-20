package com.filesharing.service.impl;

import com.filesharing.dto.PreviewResponse;
import com.filesharing.entity.FileEntity;
import com.filesharing.entity.User;
import com.filesharing.repository.FileRepository;
import com.filesharing.service.PreviewService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 文件预览服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PreviewServiceImpl implements PreviewService {
    
    private final FileRepository fileRepository;
    
    @Override
    public PreviewResponse previewFile(Long fileId, User user, String deviceType, 
                                     String userAgent, String ipAddress) {
        FileEntity file = fileRepository.findById(fileId)
                .orElseThrow(() -> new RuntimeException("文件不存在"));
        
        PreviewResponse response = new PreviewResponse();
        response.setFileId(fileId);
        response.setFileName(file.getOriginalName());
        response.setFileSize(file.getFileSize());
        response.setFileType(file.getContentType());
        response.setPreviewUrl("/api/preview/" + fileId + "/content");
        response.setIsPreviewSupported(true);
        response.setPreviewType(getPreviewType(file.getContentType()));
        
        // 记录预览日志
        PreviewRecordDto previewRecord = new PreviewRecordDto(
                fileId, user.getId(), getPreviewType(file.getContentType()), 
                deviceType, userAgent, ipAddress);
        recordPreview(previewRecord);
        
        log.info("用户 {} 预览文件 {}，设备类型: {}, 客户端IP: {}", 
                user.getUsername(), file.getOriginalName(), deviceType, ipAddress);
        
        return response;
    }
    
    @Override
    public Resource getPreviewContent(Long fileId, String previewType) {
        fileRepository.findById(fileId)
            .orElseThrow(() -> new RuntimeException("文件不存在"));
        
        // 返回模拟的内容资源
        byte[] content = "这是文件预览内容".getBytes();
        return new ByteArrayResource(content);
    }
    
    @Override
    public String getTextPreview(FileEntity file) {
        return "这是文本文件的预览内容";
    }
    
    @Override
    public byte[] getPdfPreview(FileEntity file) {
        return new byte[0]; // 返回空的PDF内容
    }
    
    @Override
    public Resource getImagePreview(FileEntity file, Integer width, Integer height) {
        byte[] imageData = new byte[0]; // 返回空的图片数据
        return new ByteArrayResource(imageData);
    }
    
    @Override
    public byte[] getOfficePreview(FileEntity file) {
        return new byte[0]; // 返回空的Office预览内容
    }
    
    @Override
    public PreviewResponse getAudioPreview(FileEntity file) {
        PreviewResponse response = new PreviewResponse();
        response.setFileId(file.getId());
        response.setFileName(file.getOriginalName());
        response.setFileType(file.getContentType());
        response.setPreviewType("audio");
        response.setIsPreviewSupported(true);
        return response;
    }
    
    @Override
    public PreviewResponse getVideoPreview(FileEntity file) {
        PreviewResponse response = new PreviewResponse();
        response.setFileId(file.getId());
        response.setFileName(file.getOriginalName());
        response.setFileType(file.getContentType());
        response.setPreviewType("video");
        response.setIsPreviewSupported(true);
        return response;
    }
    
    @Override
    public FilePreviewStatistics getFilePreviewStatistics(Long fileId) {
        FilePreviewStatistics statistics = new FilePreviewStatistics();
        statistics.setTotalPreviews(100L);
        statistics.setUniqueUsers(50L);
        statistics.setAverageDuration(120.5);
        return statistics;
    }
    
    @Override
    public UserPreviewStatistics getUserPreviewStatistics(Long userId) {
        UserPreviewStatistics statistics = new UserPreviewStatistics();
        statistics.setTotalPreviews(25L);
        statistics.setFavoriteFileTypeCount(3L);
        return statistics;
    }
    
    @Override
    public List<PreviewResponse> getPopularPreviews(int limit) {
        List<PreviewResponse> previews = new ArrayList<>();
        // 返回模拟的热门预览数据
        for (int i = 1; i <= Math.min(limit, 5); i++) {
            PreviewResponse preview = new PreviewResponse();
            preview.setFileId((long) i);
            preview.setFileName("热门文件" + i + ".pdf");
            preview.setFileSize(1024L * i);
            preview.setFileType("application/pdf");
            preview.setPreviewUrl("/api/preview/" + i + "/content");
            preview.setIsPreviewSupported(true);
            preview.setPreviewType("pdf");
            previews.add(preview);
        }
        return previews;
    }
    
    @Override
    public void recordPreview(PreviewRecordDto previewRecord) {
        // 记录预览日志的实现
        log.info("记录预览日志: 文件ID={}, 用户ID={}, 设备类型={}", 
                previewRecord.getFileId(), previewRecord.getUserId(), previewRecord.getDeviceType());
    }
    
    /**
     * 根据文件类型确定预览类型
     */
    private String getPreviewType(String contentType) {
        if (contentType == null) return "unknown";
        
        if (contentType.startsWith("image/")) {
            return "image";
        } else if (contentType.equals("application/pdf")) {
            return "pdf";
        } else if (contentType.startsWith("text/")) {
            return "text";
        } else if (contentType.equals("application/msword") || 
                   contentType.equals("application/vnd.openxmlformats-officedocument.wordprocessingml.document")) {
            return "document";
        } else if (contentType.startsWith("audio/")) {
            return "audio";
        } else if (contentType.startsWith("video/")) {
            return "video";
        } else {
            return "generic";
        }
    }
}