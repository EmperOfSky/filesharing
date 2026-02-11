package com.filesharing.service.impl;

import com.filesharing.entity.FileEntity;
import com.filesharing.entity.Thumbnail;
import com.filesharing.entity.User;
import com.filesharing.exception.BusinessException;
import com.filesharing.repository.FileRepository;
import com.filesharing.repository.ThumbnailRepository;
import com.filesharing.service.ThumbnailService;
import com.filesharing.util.ThumbnailGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.MalformedURLException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;

/**
 * 缩略图服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class ThumbnailServiceImpl implements ThumbnailService {
    
    private final ThumbnailRepository thumbnailRepository;
    private final FileRepository fileRepository;
    private final ThumbnailGenerator thumbnailGenerator;
    
    @Override
    public Thumbnail generateThumbnail(Long fileId, User user) {
        FileEntity file = getFileById(fileId);
        
        // 检查文件权限
        if (!file.getUploader().getId().equals(user.getId()) && !file.getIsPublic()) {
            throw new BusinessException("无权限生成此文件的缩略图");
        }
        
        try {
            // 生成缩略图
            byte[] thumbnailData = thumbnailGenerator.generateThumbnail(
                    file.getFilePath(), file.getFileType(), 200, 200);
            
            // 保存缩略图记录
            Thumbnail thumbnail = new Thumbnail();
            thumbnail.setFile(file);
            thumbnail.setThumbnailData(thumbnailData);
            thumbnail.setThumbnailType("image/jpeg");
            thumbnail.setWidth(200);
            thumbnail.setHeight(200);
            thumbnail.setGeneratedAt(LocalDateTime.now());
            
            Thumbnail savedThumbnail = thumbnailRepository.save(thumbnail);
            log.info("生成缩略图: 文件ID={}, 用户={}", fileId, user.getUsername());
            
            return savedThumbnail;
            
        } catch (Exception e) {
            log.error("生成缩略图失败: 文件ID={}, 错误={}", fileId, e.getMessage());
            throw new BusinessException("生成缩略图失败: " + e.getMessage());
        }
    }
    
    @Override
    @Transactional(readOnly = true)
    public Thumbnail getThumbnailByFileId(Long fileId) {
        return thumbnailRepository.findByFileId(fileId)
                .orElseThrow(() -> new BusinessException("缩略图不存在"));
    }
    
    @Override
    @Transactional(readOnly = true)
    public Resource getThumbnailResource(Long fileId) {
        Thumbnail thumbnail = getThumbnailByFileId(fileId);
        
        try {
            // 这里应该返回实际的缩略图资源
            // 简化实现：返回空资源
            Path path = Paths.get("./thumbnails/" + fileId + ".jpg");
            return new UrlResource(path.toUri());
        } catch (MalformedURLException e) {
            throw new BusinessException("获取缩略图资源失败");
        }
    }
    
    @Override
    public void deleteThumbnail(Long fileId, User user) {
        Thumbnail thumbnail = getThumbnailByFileId(fileId);
        
        // 检查权限
        if (!thumbnail.getFile().getUploader().getId().equals(user.getId())) {
            throw new BusinessException("无权限删除此缩略图");
        }
        
        thumbnailRepository.delete(thumbnail);
        log.info("删除缩略图: 文件ID={}, 用户={}", fileId, user.getUsername());
    }
    
    @Override
    @Transactional(readOnly = true)
    public boolean hasThumbnail(Long fileId) {
        return thumbnailRepository.existsByFileId(fileId);
    }
    
    // ==================== 私有方法 ====================
    
    private FileEntity getFileById(Long fileId) {
        return fileRepository.findById(fileId)
                .orElseThrow(() -> new BusinessException("文件不存在"));
    }
}