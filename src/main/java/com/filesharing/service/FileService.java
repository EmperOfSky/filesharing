package com.filesharing.service;

import com.filesharing.dto.FileResponse;
import com.filesharing.dto.FileUploadResponse;
import com.filesharing.dto.request.MobileUploadRequest;
import com.filesharing.dto.response.FileSimpleResponse;
import com.filesharing.entity.FileEntity;
import com.filesharing.entity.User;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 文件服务接口
 */
public interface FileService {
    
    /**
     * 上传文件
     */
    FileUploadResponse uploadFile(MultipartFile file, Long folderId, User uploader);
    
    /**
     * 获取文件信息
     */
    FileResponse getFileById(Long fileId);
    
    /**
     * 获取用户的所有文件
     */
    List<FileResponse> getUserFiles(User user, String fileName, int page, int size);
    
    /**
     * 下载文件
     */
    FileEntity downloadFile(Long fileId);
    
    /**
     * 删除文件
     */
    void deleteFile(Long fileId, User currentUser);
    
    /**
     * 移动文件到指定文件夹
     */
    FileResponse moveFile(Long fileId, Long targetFolderId, User currentUser);
    
    /**
     * 重命名文件
     */
    FileResponse renameFile(Long fileId, String newName, User currentUser);

    /**
     * 复制文件到目标文件夹
     */
    FileResponse copyFile(Long fileId, Long targetFolderId, User currentUser);
    
    /**
     * 设置文件公开状态
     */
    FileResponse setFilePublic(Long fileId, Boolean isPublic, User currentUser);
    
    /**
     * 获取公开文件列表
     */
    List<FileResponse> getPublicFiles(int page, int size);
    
    /**
     * 根据扩展名搜索文件
     */
    List<FileResponse> searchFilesByExtension(String extension, int page, int size);
    
    /**
     * 根据MD5检查文件是否已存在（用于秒传功能）
     */
    FileEntity checkFileExists(String md5Hash);
    
    /**
     * 获取文件实体
     */
    FileEntity getFileEntityById(Long fileId);
    
    /**
     * 清理已删除的文件
     */
    void cleanupDeletedFiles();
    
    /**
     * 分片上传初始化
     */
    FileUploadResponse initChunkUpload(String fileName, Long fileSize, Long chunkSize, 
                                     Integer totalChunks, Long folderId, User uploader);
    
    /**
     * 上传分片
     */
    FileUploadResponse uploadChunk(MultipartFile chunk, String uploadId, Integer chunkIndex, 
                                 Integer totalChunks, User uploader);
    
    /**
     * 获取上传进度
     */
    Double getUploadProgress(String uploadId);
    
    // 移动端专用方法
    
    /**
     * 移动端文件上传
     */
    FileUploadResponse uploadFile(MobileUploadRequest request, User uploader);
    
    /**
     * 获取最近文件
     */
    List<FileSimpleResponse> getRecentFiles(User user, Integer limit);
    
    /**
     * 获取收藏文件
     */
    List<FileSimpleResponse> getFavoriteFiles(User user);
    
    /**
     * 收藏文件
     */
    void favoriteFile(Long fileId, User user);
    
    /**
     * 取消收藏文件
     */
    void unfavoriteFile(Long fileId, User user);
    
    /**
     * 搜索文件（移动端优化）
     */
    List<FileSimpleResponse> searchFiles(String keyword, User user, Integer limit);
    
    /**
     * 获取离线可用文件
     */
    List<FileSimpleResponse> getOfflineAvailableFiles(User user);
}