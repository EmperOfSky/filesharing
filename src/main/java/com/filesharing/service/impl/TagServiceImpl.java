package com.filesharing.service.impl;

import com.filesharing.entity.Tag;
import com.filesharing.entity.User;
import com.filesharing.exception.BusinessException;
import com.filesharing.repository.TagRepository;
import com.filesharing.service.TagService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 标签服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class TagServiceImpl implements TagService {
    
    private final TagRepository tagRepository;
    
    @Override
    public Tag createTag(String tagName, String color, String description, User createdBy) {
        // 检查标签是否已存在
        if (tagRepository.findByTagName(tagName).isPresent()) {
            throw new BusinessException("标签已存在");
        }
        
        Tag tag = new Tag();
        tag.setTagName(tagName);
        tag.setColor(color != null ? color : "#007bff");
        tag.setDescription(description);
        tag.setCreatedBy(createdBy);
        tag.setIsSystemTag(false);
        tag.setUsageCount(0);
        tag.setCreatedAt(LocalDateTime.now());
        tag.setUpdatedAt(LocalDateTime.now());
        
        Tag savedTag = tagRepository.save(tag);
        log.info("创建标签: ID={}, 名称={}, 创建者={}", savedTag.getId(), tagName, createdBy.getUsername());
        
        return savedTag;
    }
    
    @Override
    @Transactional(readOnly = true)
    public Page<Tag> getUserTags(User user, Pageable pageable) {
        return tagRepository.findByUserOrSystem(user, pageable);
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<Tag> getSystemTags() {
        return tagRepository.findSystemTags();
    }
    
    @Override
    @Transactional(readOnly = true)
    public Tag getTagById(Long tagId) {
        return tagRepository.findById(tagId)
                .orElseThrow(() -> new BusinessException("标签不存在"));
    }
    
    @Override
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
        
        log.info("更新标签: ID={}, 更新者={}", tagId, user.getUsername());
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
        log.info("删除标签: ID={}, 删除者={}", tagId, user.getUsername());
    }
    
    @Override
    public void incrementTagUsage(Long tagId) {
        Tag tag = getTagById(tagId);
        tag.setUsageCount(tag.getUsageCount() + 1);
        tag.setUpdatedAt(LocalDateTime.now());
        tagRepository.save(tag);
    }
    
    @Override
    public void decrementTagUsage(Long tagId) {
        Tag tag = getTagById(tagId);
        tag.setUsageCount(Math.max(0, tag.getUsageCount() - 1));
        tag.setUpdatedAt(LocalDateTime.now());
        tagRepository.save(tag);
    }
}