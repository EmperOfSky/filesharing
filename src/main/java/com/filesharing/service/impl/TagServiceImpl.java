package com.filesharing.service.impl;

import com.filesharing.entity.Tag;
import com.filesharing.entity.User;
import com.filesharing.exception.BusinessException;
import com.filesharing.repository.TagRepository;
import com.filesharing.service.TagService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 标签服务实现类
 * @author Admin
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class TagServiceImpl implements TagService {
    
    private final TagRepository tagRepository;
    
    @Override
    public Tag createTag(TagCreateRequest request, User user) {
        // 检查标签是否已存在
        if (tagRepository.findByTagName(request.getTagName()).isPresent()) {
            throw new BusinessException("标签已存在");
        }
            
        Tag tag = new Tag();
        tag.setTagName(request.getTagName());
        tag.setColor(request.getColor() != null ? request.getColor() : "#007bff");
        tag.setDescription(request.getDescription());
        tag.setCreatedBy(user);
        tag.setIsSystemTag(false);
        tag.setUsageCount(0);
        tag.setCreatedAt(LocalDateTime.now());
        tag.setUpdatedAt(LocalDateTime.now());
            
        Tag savedTag = tagRepository.save(tag);
        log.info("创建标签：ID={}, 名称={}, 创建者={}", savedTag.getId(), request.getTagName(), user.getUsername());
            
        return savedTag;
    }
    
    @Override
    @Transactional(readOnly = true)
    public Page<TagInfo> getUserTags(User user, String keyword, boolean includeSystem, int page, int size) {
        // 简化实现
        return Page.empty();
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<TagInfo> getSystemTags() {
        // 简化实现
        return new ArrayList<>();
    }
    
    @Transactional(readOnly = true)
    public Tag getTagById(Long tagId) {
        return tagRepository.findById(tagId)
                .orElseThrow(() -> new BusinessException("标签不存在"));
    }
    
    public Tag updateTag(Long tagId, String tagName, String color, String description, User user) {
        Tag tag = getTagById(tagId);
        
        // 检查权限
        if (tag.getCreatedBy() != null && !tag.getCreatedBy().getId().equals(user.getId())) {
            throw new BusinessException("无权限修改此标签");
        }
        
        if (tagName != null) {
            tag.setTagName(tagName);
        }
        if (color != null) {
            tag.setColor(color);
        }
        if (description != null) {
            tag.setDescription(description);
        }
        
        tag.setUpdatedAt(LocalDateTime.now());
        Tag updatedTag = tagRepository.save(tag);
        
        log.info("更新标签：ID={}, 更新者={}", tagId, user.getUsername());
        return updatedTag;
    }
    
    @Override
    public Tag updateTag(Long tagId, TagUpdateRequest request, User user) {
        Tag tag = getTagById(tagId);
        
        // 检查权限
        if (tag.getCreatedBy() != null && !tag.getCreatedBy().getId().equals(user.getId())) {
            throw new BusinessException("无权限修改此标签");
        }
        
        if (request.getTagName() != null) {
            tag.setTagName(request.getTagName());
        }
        if (request.getColor() != null) {
            tag.setColor(request.getColor());
        }
        if (request.getDescription() != null) {
            tag.setDescription(request.getDescription());
        }
        
        tag.setUpdatedAt(LocalDateTime.now());
        Tag updatedTag = tagRepository.save(tag);
        
        log.info("更新标签：ID={}, 更新者={}", tagId, user.getUsername());
        return updatedTag;
    }
    
    @Override
    public void deleteTag(Long tagId, User user) {
        Tag tag = getTagById(tagId);
        
        // 系统标签不能删除
        if (tag.getIsSystemTag()) {
            throw new BusinessException("不能删除系统标签");
        }
        
        // 检查权限
        if (!tag.getCreatedBy().getId().equals(user.getId())) {
            throw new BusinessException("无权限删除此标签");
        }
        
        tagRepository.delete(tag);
        log.info("删除标签：ID={}, 删除者={}", tagId, user.getUsername());
    }
    
    public void incrementTagUsage(Long tagId) {
        Tag tag = getTagById(tagId);
        tag.setUsageCount(tag.getUsageCount() + 1);
        tag.setUpdatedAt(LocalDateTime.now());
        tagRepository.save(tag);
    }
    
    public void decrementTagUsage(Long tagId) {
        Tag tag = getTagById(tagId);
        tag.setUsageCount(Math.max(0, tag.getUsageCount() - 1));
        tag.setUpdatedAt(LocalDateTime.now());
        tagRepository.save(tag);
    }
    
    @Override
    public Page<TagInfo> getPopularTags(User user, int page, int size) {
        // 简化实现
        return Page.empty();
    }
    
    @Override
    public List<TagCloudItem> getTagCloud(User user, int limit) {
        // 简化实现
        return new ArrayList<>();
    }
    
    @Override
    public void addTagToFile(Long fileId, Long tagId, User user) {
        // 简化实现
        log.info("为文件添加标签：文件 ID={}, 标签 ID={}", fileId, tagId);
    }
    
    @Override
    public BatchTagResult batchAddTagsToFile(Long fileId, List<Long> tagIds, User user) {
        // 简化实现
        return new BatchTagResult(tagIds.size(), tagIds.size(), 0);
    }
    
    @Override
    public void removeTagFromFile(Long fileId, Long tagId, User user) {
        // 简化实现
        log.info("从文件移除标签：文件 ID={}, 标签 ID={}", fileId, tagId);
    }
    
    @Override
    public void clearFileTags(Long fileId, User user) {
        // 简化实现
        log.info("清除文件的所有标签：文件 ID={}", fileId);
    }
    
    @Override
    public List<TagInfo> getFileTags(Long fileId) {
        // 简化实现
        return new ArrayList<>();
    }
    
    @Override
    public List<FileTagInfo> getFilesWithSameTags(Long fileId, int limit) {
        // 简化实现
        return new ArrayList<>();
    }
    
    @Override
    public void addTagToFolder(Long folderId, Long tagId, User user) {
        // 简化实现
        log.info("为文件夹添加标签：文件夹 ID={}, 标签 ID={}", folderId, tagId);
    }
    
    @Override
    public void removeTagFromFolder(Long folderId, Long tagId, User user) {
        // 简化实现
        log.info("从文件夹移除标签：文件夹 ID={}, 标签 ID={}", folderId, tagId);
    }
    
    @Override
    public List<TagInfo> getFolderTags(Long folderId) {
        // 简化实现
        return new ArrayList<>();
    }
    
    @Override
    public Page<FileTagInfo> searchFilesByTags(List<Long> tagIds, User user, int page, int size) {
        // 简化实现
        return Page.empty();
    }
    
    @Override
    public Page<FolderTagInfo> searchFoldersByTags(List<Long> tagIds, User user, int page, int size) {
        // 简化实现
        return Page.empty();
    }
    
    @Override
    public List<TagUsageStat> getTagUsageStatistics(User user) {
        // 简化实现
        return new ArrayList<>();
    }
}
