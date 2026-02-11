package com.filesharing.service.impl;

import com.filesharing.dto.ShareCreateRequest;
import com.filesharing.dto.ShareResponse;
import com.filesharing.entity.FileEntity;
import com.filesharing.entity.Folder;
import com.filesharing.entity.ShareRecord;
import com.filesharing.entity.User;
import com.filesharing.exception.BusinessException;
import com.filesharing.repository.FileRepository;
import com.filesharing.repository.FolderRepository;
import com.filesharing.repository.ShareRepository;
import com.filesharing.service.ShareService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 分享服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class ShareServiceImpl implements ShareService {
    
    private final ShareRepository shareRepository;
    private final FileRepository fileRepository;
    private final FolderRepository folderRepository;
    
    @Override
    public ShareResponse createShare(ShareCreateRequest request, User sharer) {
        ShareRecord share = new ShareRecord();
        share.setShareKey(UUID.randomUUID().toString().replace("-", "").substring(0, 16));
        share.setTitle(request.getTitle());
        share.setDescription(request.getDescription());
        share.setSharer(sharer);
        share.setMaxAccessCount(request.getMaxAccessCount());
        share.setCurrentAccessCount(0);
        share.setAllowDownload(request.getAllowDownload() != null ? request.getAllowDownload() : true);
        share.setStatus(ShareRecord.ShareStatus.ACTIVE);
        share.setCreatedAt(LocalDateTime.now());
        share.setUpdatedAt(LocalDateTime.now());
        
        // 设置过期时间
        if (request.getExpireDays() != null && request.getExpireDays() > 0) {
            share.setExpireTime(LocalDateTime.now().plusDays(request.getExpireDays()));
        }
        
        // 设置访问密码
        if (request.getPassword() != null && !request.getPassword().isEmpty()) {
            share.setPassword(request.getPassword());
        }
        
        // 根据分享类型设置关联对象
        if ("FILE".equals(request.getShareType()) && request.getFileId() != null) {
            FileEntity file = fileRepository.findById(request.getFileId())
                    .orElseThrow(() -> new BusinessException("文件不存在"));
            share.setFile(file);
            share.setShareType(ShareRecord.ShareType.FILE);
        } else if ("FOLDER".equals(request.getShareType()) && request.getFolderId() != null) {
            Folder folder = folderRepository.findById(request.getFolderId())
                    .orElseThrow(() -> new BusinessException("文件夹不存在"));
            share.setFolder(folder);
            share.setShareType(ShareRecord.ShareType.FOLDER);
        } else {
            throw new BusinessException("无效的分享类型或缺少必要参数");
        }
        
        ShareRecord savedShare = shareRepository.save(share);
        log.info("创建分享: 分享ID={}, 分享者={}", savedShare.getId(), sharer.getUsername());
        
        return convertToShareResponse(savedShare);
    }
    
    @Override
    @Transactional(readOnly = true)
    public ShareResponse getShareByShareKey(String shareKey) {
        ShareRecord share = shareRepository.findByShareKey(shareKey)
                .orElseThrow(() -> new BusinessException("分享不存在或已过期"));
        
        // 检查分享状态
        if (share.getStatus() != ShareRecord.ShareStatus.ACTIVE) {
            throw new BusinessException("分享已失效");
        }
        
        // 检查是否过期
        if (share.getExpireTime() != null && share.getExpireTime().isBefore(LocalDateTime.now())) {
            share.setStatus(ShareRecord.ShareStatus.EXPIRED);
            shareRepository.save(share);
            throw new BusinessException("分享已过期");
        }
        
        // 检查访问次数限制
        if (share.getMaxAccessCount() > 0 && 
            share.getCurrentAccessCount() >= share.getMaxAccessCount()) {
            share.setStatus(ShareRecord.ShareStatus.DISABLED);
            shareRepository.save(share);
            throw new BusinessException("分享访问次数已达上限");
        }
        
        // 增加访问次数
        share.setCurrentAccessCount(share.getCurrentAccessCount() + 1);
        shareRepository.save(share);
        
        return convertToShareResponse(share);
    }
    
    @Override
    @Transactional(readOnly = true)
    public Page<ShareResponse> getUserShares(User user, Pageable pageable) {
        return shareRepository.findBySharerOrderByCreatedAtDesc(user, pageable)
                .map(this::convertToShareResponse);
    }
    
    @Override
    public void cancelShare(Long shareId, User user) {
        ShareRecord share = getShareById(shareId);
        
        if (!share.getSharer().getId().equals(user.getId())) {
            throw new BusinessException("无权限取消此分享");
        }
        
        share.setStatus(ShareRecord.ShareStatus.DISABLED);
        share.setUpdatedAt(LocalDateTime.now());
        shareRepository.save(share);
        
        log.info("取消分享: 分享ID={}, 用户={}", shareId, user.getUsername());
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<ShareResponse> getActiveShares() {
        return shareRepository.findActiveShares()
                .stream()
                .map(this::convertToShareResponse)
                .collect(Collectors.toList());
    }
    
    @Override
    public void updateSharePassword(Long shareId, String newPassword, User user) {
        ShareRecord share = getShareById(shareId);
        
        if (!share.getSharer().getId().equals(user.getId())) {
            throw new BusinessException("无权限修改此分享");
        }
        
        share.setPassword(newPassword);
        share.setUpdatedAt(LocalDateTime.now());
        shareRepository.save(share);
        
        log.info("更新分享密码: 分享ID={}, 用户={}", shareId, user.getUsername());
    }
    
    @Override
    @Transactional(readOnly = true)
    public boolean validateSharePassword(Long shareId, String password) {
        ShareRecord share = getShareById(shareId);
        return share.getPassword() == null || share.getPassword().equals(password);
    }
    
    // ==================== 私有方法 ====================
    
    private ShareRecord getShareById(Long shareId) {
        return shareRepository.findById(shareId)
                .orElseThrow(() -> new BusinessException("分享记录不存在"));
    }
    
    private ShareResponse convertToShareResponse(ShareRecord share) {
        ShareResponse response = new ShareResponse();
        response.setId(share.getId());
        response.setShareKey(share.getShareKey());
        response.setTitle(share.getTitle());
        response.setDescription(share.getDescription());
        response.setShareType(share.getShareType().name());
        response.setSharerName(share.getSharer().getUsername());
        response.setSharerId(share.getSharer().getId());
        
        if (share.getExpireTime() != null) {
            response.setExpireTime(share.getExpireTime().toString());
        }
        
        response.setMaxAccessCount(share.getMaxAccessCount());
        response.setCurrentAccessCount(share.getCurrentAccessCount());
        response.setStatus(share.getStatus().name());
        response.setAllowDownload(share.getAllowDownload());
        response.setAccessUrl("/api/shares/" + share.getShareKey());
        response.setCreatedAt(share.getCreatedAt().toString());
        response.setUpdatedAt(share.getUpdatedAt().toString());
        
        return response;
    }
}