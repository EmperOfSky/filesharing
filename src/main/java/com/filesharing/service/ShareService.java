package com.filesharing.service;

import com.filesharing.dto.ShareCreateRequest;
import com.filesharing.dto.ShareResponse;
import com.filesharing.entity.FileEntity;
import com.filesharing.entity.ShareRecord;
import com.filesharing.entity.User;

import java.util.List;

/**
 * 分享服务接口
 */
public interface ShareService {
    
    /**
     * 创建分享
     */
    ShareResponse createShare(ShareCreateRequest request, User sharer);
    
    /**
     * 获取分享信息
     */
    ShareResponse getShareByShareKey(String shareKey);

    /**
     * 获取公开分享信息（不增加访问次数）
     */
    ShareResponse getPublicShareInfo(String shareKey);
    
    /**
     * 获取用户的分享记录
     */
    List<ShareResponse> getUserShares(User user, int page, int size);
    
    /**
     * 访问分享（增加访问次数）
     */
    ShareResponse accessShare(String shareKey, String password, String ipAddress);

    /**
     * 校验并获取可下载的分享文件
     */
    FileEntity resolveShareFileForDownload(String shareKey, String password);
    
    /**
     * 删除分享
     */
    void deleteShare(Long shareId, User currentUser);
    
    /**
     * 禁用分享
     */
    void disableShare(Long shareId, User currentUser);
    
    /**
     * 启用分享
     */
    void enableShare(Long shareId, User currentUser);
    
    /**
     * 更新分享信息
     */
    ShareResponse updateShare(Long shareId, ShareCreateRequest request, User currentUser);
    
    /**
     * 获取分享统计信息
     */
    ShareStatistics getShareStatistics(User user);
    
    /**
     * 清理过期分享
     */
    void cleanupExpiredShares();
    
    /**
     * 获取分享实体
     */
    ShareRecord getShareEntityById(Long shareId);
    
    /**
     * 验证分享访问权限
     */
    boolean validateShareAccess(ShareRecord share, String password);
    
    /**
     * 分享统计信息内部类
     */
    class ShareStatistics {
        private Long totalShares;
        private Long activeShares;
        private Long expiredShares;
        private Long totalAccessCount;
        
        // 构造函数、getter和setter
        public ShareStatistics() {}
        
        public ShareStatistics(Long totalShares, Long activeShares, Long expiredShares, Long totalAccessCount) {
            this.totalShares = totalShares;
            this.activeShares = activeShares;
            this.expiredShares = expiredShares;
            this.totalAccessCount = totalAccessCount;
        }
        
        // getters and setters
        public Long getTotalShares() { return totalShares; }
        public void setTotalShares(Long totalShares) { this.totalShares = totalShares; }
        
        public Long getActiveShares() { return activeShares; }
        public void setActiveShares(Long activeShares) { this.activeShares = activeShares; }
        
        public Long getExpiredShares() { return expiredShares; }
        public void setExpiredShares(Long expiredShares) { this.expiredShares = expiredShares; }
        
        public Long getTotalAccessCount() { return totalAccessCount; }
        public void setTotalAccessCount(Long totalAccessCount) { this.totalAccessCount = totalAccessCount; }
    }
}