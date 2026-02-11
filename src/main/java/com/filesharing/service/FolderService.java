package com.filesharing.service;

import com.filesharing.dto.FolderCreateRequest;
import com.filesharing.dto.FolderResponse;
import com.filesharing.dto.response.FolderSimpleResponse;
import com.filesharing.entity.Folder;
import com.filesharing.entity.User;

import java.util.List;

/**
 * 文件夹服务接口
 */
public interface FolderService {
    
    /**
     * 创建文件夹
     */
    FolderResponse createFolder(FolderCreateRequest request, User owner);
    
    /**
     * 获取文件夹信息
     */
    FolderResponse getFolderById(Long folderId);
    
    /**
     * 获取用户的根文件夹
     */
    List<FolderResponse> getRootFolders(User owner);
    
    /**
     * 获取文件夹下的子文件夹
     */
    List<FolderSimpleResponse> getSubFolders(Long folderId, User currentUser);
    
    /**
     * 获取文件夹树结构
     */
    List<FolderResponse> getFolderTree(User owner);
    
    /**
     * 删除文件夹
     */
    void deleteFolder(Long folderId, User currentUser);
    
    /**
     * 重命名文件夹
     */
    FolderResponse renameFolder(Long folderId, String newName, User currentUser);
    
    /**
     * 移动文件夹
     */
    FolderResponse moveFolder(Long folderId, Long targetParentId, User currentUser);
    
    /**
     * 设置文件夹公开状态
     */
    FolderResponse setFolderPublic(Long folderId, Boolean isPublic, User currentUser);
    
    /**
     * 获取文件夹路径
     */
    String getFolderPath(Long folderId);
    
    /**
     * 获取文件夹实体
     */
    Folder getFolderEntityById(Long folderId);
    
    /**
     * 验证文件夹权限
     */
    boolean hasFolderPermission(Long folderId, User user);
    
    // 移动端专用方法
    
    /**
     * 获取用户文件夹树结构
     */
    List<FolderSimpleResponse> getUserFolderTree(User user);
    
    /**
     * 获取文件夹面包屑路径
     */
    List<FolderSimpleResponse> getFolderBreadcrumb(Long folderId, User user);
    
    /**
     * 获取快捷访问文件夹
     */
    List<FolderSimpleResponse> getQuickAccessFolders(User user);
}