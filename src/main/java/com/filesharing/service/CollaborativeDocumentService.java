package com.filesharing.service;

import com.filesharing.dto.DocumentCreateDTO;
import com.filesharing.dto.DocumentResponseDTO;
import com.filesharing.dto.DocumentStatsDTO;
import com.filesharing.entity.CollaborativeDocument;
import com.filesharing.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

/**
 * 协作文档服务接口
 */
public interface CollaborativeDocumentService {
    
    /**
     * 创建协作文档
     */
    DocumentResponseDTO createDocument(DocumentCreateDTO documentCreateDTO, User currentUser);
    
    /**
     * 获取文档详情
     */
    DocumentResponseDTO getDocumentById(Long documentId, User currentUser);
    
    /**
     * 更新文档内容
     */
    DocumentResponseDTO updateDocumentContent(Long documentId, String newContent, User currentUser);
    
    /**
     * 保存文档版本
     */
    DocumentResponseDTO saveDocumentVersion(Long documentId, String commitMessage, User currentUser);
    
    /**
     * 获取文档历史版本
     */
    Page<DocumentResponseDTO> getDocumentVersions(Long documentId, Pageable pageable);
    
    /**
     * 恢复到指定版本
     */
    DocumentResponseDTO restoreToVersion(Long documentId, Integer versionNumber, User currentUser);
    
    /**
     * 开始编辑文档
     */
    void startEditing(Long documentId, User currentUser);
    
    /**
     * 结束编辑文档
     */
    void stopEditing(Long documentId, User currentUser);
    
    /**
     * 获取正在编辑该文档的用户列表
     */
    List<DocumentResponseDTO.UserInfoDTO> getEditingUsers(Long documentId);
    
    /**
     * 获取项目的所有文档
     */
    Page<DocumentResponseDTO> getProjectDocuments(Long projectId, Pageable pageable);
    
    /**
     * 搜索文档
     */
    Page<DocumentResponseDTO> searchDocuments(String keyword, Pageable pageable);
    
    /**
     * 根据标签筛选文档
     */
    Page<DocumentResponseDTO> getDocumentsByTag(String tag, Pageable pageable);
    
    /**
     * 根据文档类型筛选
     */
    Page<DocumentResponseDTO> getDocumentsByType(CollaborativeDocument.DocumentType documentType, Pageable pageable);
    
    /**
     * 删除文档
     */
    void deleteDocument(Long documentId, User currentUser);
    
    /**
     * 获取最近更新的文档
     */
    Page<DocumentResponseDTO> getRecentlyUpdatedDocuments(Pageable pageable);
    
    /**
     * 获取用户拥有的文档
     */
    Page<DocumentResponseDTO> getUserDocuments(User user, Pageable pageable);
    
    /**
     * 检查用户是否有文档访问权限
     */
    boolean hasDocumentAccess(Long documentId, User user);
    
    /**
     * 获取文档统计信息
     */
    DocumentStatsDTO getDocumentStats(Long documentId);
}