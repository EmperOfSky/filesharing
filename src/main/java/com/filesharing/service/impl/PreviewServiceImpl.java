package com.filesharing.service.impl;

import com.filesharing.dto.PreviewResponse;
import com.filesharing.entity.FileEntity;
import com.filesharing.entity.User;
import com.filesharing.exception.BusinessException;
import com.filesharing.repository.FileRepository;
import com.filesharing.service.PreviewService;
import com.filesharing.util.FileStorageUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.hwpf.HWPFDocument;
import org.apache.poi.hwpf.extractor.WordExtractor;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
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
    private final FileStorageUtil fileStorageUtil;
    
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
        FileEntity file = fileRepository.findById(fileId)
                .orElseThrow(() -> new RuntimeException("文件不存在"));

        if ("image".equalsIgnoreCase(previewType)) {
            return getImagePreview(file, null, null);
        }
        if ("pdf".equalsIgnoreCase(previewType)) {
            return new ByteArrayResource(getPdfPreview(file));
        }
        if ("text".equalsIgnoreCase(previewType)) {
            return new ByteArrayResource(getTextPreview(file).getBytes(StandardCharsets.UTF_8));
        }

        return resolveFileResource(file);
    }
    
    @Override
    public String getTextPreview(FileEntity file) {
        Resource resource = resolveFileResource(file);
        String fileName = file.getOriginalName() == null ? "" : file.getOriginalName().toLowerCase();

        if (fileName.endsWith(".docx") || fileName.endsWith(".doc")) {
            try {
                return extractWordText(resource, fileName);
            } catch (Throwable t) {
                log.error("Word 文档文本提取失败: fileName={}, error={}", file.getOriginalName(), t.getMessage(), t);
                return "当前 Word 文档暂无法解析，请先下载后查看。";
            }
        }

        try (InputStream in = resource.getInputStream()) {
            byte[] buffer = new byte[64 * 1024];
            int read = in.read(buffer);
            if (read <= 0) {
                return "";
            }
            return new String(buffer, 0, read, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new BusinessException("读取文本预览失败: " + e.getMessage(), e);
        }
    }
    
    @Override
    public byte[] getPdfPreview(FileEntity file) {
        Resource resource = resolveFileResource(file);
        try (InputStream in = resource.getInputStream(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[8192];
            int len;
            while ((len = in.read(buffer)) != -1) {
                out.write(buffer, 0, len);
            }
            return out.toByteArray();
        } catch (IOException e) {
            throw new BusinessException("读取PDF预览失败: " + e.getMessage(), e);
        }
    }
    
    @Override
    public Resource getImagePreview(FileEntity file, Integer width, Integer height) {
        // 当前返回原图资源；尺寸参数预留给后续缩略图优化。
        return resolveFileResource(file);
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

    private Resource resolveFileResource(FileEntity file) {
        Resource resource = fileStorageUtil.loadAsResource(file.getStorageName(), file.getFilePath());
        if (resource == null || !resource.exists()) {
            throw new BusinessException("预览文件不存在");
        }
        return resource;
    }

    private String extractWordText(Resource resource, String fileName) {
        try (InputStream in = resource.getInputStream()) {
            if (fileName.endsWith(".docx")) {
                try (XWPFDocument document = new XWPFDocument(in);
                     XWPFWordExtractor extractor = new XWPFWordExtractor(document)) {
                    return normalizeExtractedText(extractor.getText());
                }
            }

            if (fileName.endsWith(".doc")) {
                try (HWPFDocument document = new HWPFDocument(in);
                     WordExtractor extractor = new WordExtractor(document)) {
                    return normalizeExtractedText(extractor.getText());
                }
            }
        } catch (Throwable e) {
            throw new BusinessException("读取 Word 预览失败: " + e.getMessage(), e);
        }

        throw new BusinessException("不支持的 Word 文档类型");
    }

    private String normalizeExtractedText(String text) {
        if (text == null) {
            return "";
        }
        String normalized = text.trim();
        return normalized.isEmpty() ? "(文档内容为空或无法提取文本)" : normalized;
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