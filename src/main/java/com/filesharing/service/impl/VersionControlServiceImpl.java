package com.filesharing.service.impl;

import com.filesharing.entity.FileEntity;
import com.filesharing.entity.FileVersion;
import com.filesharing.entity.User;
import com.filesharing.exception.BusinessException;
import com.filesharing.repository.FileRepository;
import com.filesharing.repository.FileVersionRepository;
import com.filesharing.service.VersionControlService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 版本控制服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class VersionControlServiceImpl implements VersionControlService {
    
    private final FileVersionRepository fileVersionRepository;
    private final FileRepository fileRepository;
    
    @Override
    public FileVersion createVersion(Long fileId, String description, User user) {
        FileEntity file = getFileById(fileId);
        
        // 检查权限
        if (!file.getUploader().getId().equals(user.getId())) {
            throw new BusinessException("无权限为此文件创建版本");
        }
        
        // 获取当前最大版本号
        Integer maxVersion = fileVersionRepository.findMaxVersionByFile(file).orElse(0);
        
        // 创建新版本
        FileVersion version = new FileVersion();
        version.setFile(file);
        version.setVersionNumber(maxVersion + 1);
        version.setVersionDescription(description != null ? description : "版本更新");
        version.setStorageName(file.getStorageName());
        version.setFilePath(file.getFilePath());
        version.setFileSize(file.getFileSize());
        version.setMd5Hash(file.getMd5Hash());
        version.setContentType(file.getContentType());
        version.setModifiedBy(user);
        version.setIsCurrent(true);
        version.setIsDeleted(false);
        version.setCreatedAt(LocalDateTime.now());
        
        // 将之前的版本设为非当前版本
        fileVersionRepository.findByFile(file).ifPresent(versions -> 
            versions.forEach(v -> {
                if (v.getIsCurrent()) {
                    v.setIsCurrent(false);
                    fileVersionRepository.save(v);
                }
            })
        );
        
        FileVersion savedVersion = fileVersionRepository.save(version);
        log.info("创建文件版本: 文件ID={}, 版本号={}, 用户={}", 
                fileId, version.getVersionNumber(), user.getUsername());
        
        return savedVersion;
    }
    
    @Override
    @Transactional(readOnly = true)
    public Page<FileVersion> getFileVersions(Long fileId, Pageable pageable) {
        FileEntity file = getFileById(fileId);
        return fileVersionRepository.findByFile(file, pageable);
    }
    
    @Override
    @Transactional(readOnly = true)
    public FileVersion getVersionById(Long versionId) {
        return fileVersionRepository.findById(versionId)
                .orElseThrow(() -> new BusinessException("文件版本不存在"));
    }
    
    @Override
    public FileVersion restoreVersion(Long versionId, User user) {
        FileVersion version = getVersionById(versionId);
        FileEntity file = version.getFile();
        
        // 检查权限
        if (!file.getUploader().getId().equals(user.getId())) {
            throw new BusinessException("无权限恢复此版本");
        }
        
        // 将当前版本设为非当前
        fileVersionRepository.findCurrentVersion(file).ifPresent(currentVersion -> {
            currentVersion.setIsCurrent(false);
            fileVersionRepository.save(currentVersion);
        });
        
        // 恢复指定版本为当前版本
        version.setIsCurrent(true);
        version.setModifiedBy(user);
        FileVersion restoredVersion = fileVersionRepository.save(version);
        
        // 更新文件信息
        file.setStorageName(version.getStorageName());
        file.setFilePath(version.getFilePath());
        file.setFileSize(version.getFileSize());
        file.setMd5Hash(version.getMd5Hash());
        file.setContentType(version.getContentType());
        file.setUpdatedAt(LocalDateTime.now());
        fileRepository.save(file);
        
        log.info("恢复文件版本: 文件ID={}, 版本ID={}, 用户={}", 
                file.getId(), versionId, user.getUsername());
        
        return restoredVersion;
    }
    
    @Override
    public void deleteVersion(Long versionId, User user) {
        FileVersion version = getVersionById(versionId);
        FileEntity file = version.getFile();
        
        // 检查权限
        if (!file.getUploader().getId().equals(user.getId())) {
            throw new BusinessException("无权限删除此版本");
        }
        
        // 不能删除当前版本
        if (version.getIsCurrent()) {
            throw new BusinessException("不能删除当前版本");
        }
        
        version.setIsDeleted(true);
        fileVersionRepository.save(version);
        
        log.info("删除文件版本: 版本ID={}, 用户={}", versionId, user.getUsername());
    }
    
    @Override
    @Transactional(readOnly = true)
    public FileVersion getCurrentVersion(Long fileId) {
        FileEntity file = getFileById(fileId);
        return fileVersionRepository.findCurrentVersion(file)
                .orElseThrow(() -> new BusinessException("当前版本不存在"));
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<FileVersion> getVersionHistory(Long fileId) {
        FileEntity file = getFileById(fileId);
        return fileVersionRepository.findVersionHistory(file);
    }
    
    // ==================== 私有方法 ====================
    
    private FileEntity getFileById(Long fileId) {
        return fileRepository.findById(fileId)
                .orElseThrow(() -> new BusinessException("文件不存在"));
    }
}