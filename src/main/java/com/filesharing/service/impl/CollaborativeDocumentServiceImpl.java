package com.filesharing.service.impl;

import com.filesharing.dto.DocumentCreateDTO;
import com.filesharing.dto.DocumentResponseDTO;
import com.filesharing.dto.DocumentStatsDTO;
import com.filesharing.entity.CollaborativeDocument;
import com.filesharing.entity.CollaborationProject;
import com.filesharing.entity.User;
import com.filesharing.exception.BusinessException;
import com.filesharing.repository.CollaborativeDocumentRepository;
import com.filesharing.repository.CollaborationProjectRepository;
import com.filesharing.service.CollaborativeDocumentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 协作文档服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class CollaborativeDocumentServiceImpl implements CollaborativeDocumentService {
    
    private final CollaborativeDocumentRepository documentRepository;
    private final CollaborationProjectRepository projectRepository;
    
    @Override
    public DocumentResponseDTO createDocument(DocumentCreateDTO documentCreateDTO, User currentUser) {
        // 验证项目权限
        CollaborationProject project = projectRepository.findById(documentCreateDTO.getProjectId())
                .orElseThrow(() -> new BusinessException("项目不存在"));
        
        // 检查创建权限
        if (!hasProjectPermission(project, currentUser)) {
            throw new BusinessException("无权限在此项目中创建文档");
        }
        
        // 创建文档
        CollaborativeDocument document = new CollaborativeDocument();
        document.setProject(project);
        document.setDocumentName(documentCreateDTO.getDocumentName());
        document.setContent(documentCreateDTO.getContent() != null ? documentCreateDTO.getContent() : "");
        document.setDocumentType(documentCreateDTO.getDocumentType());
        document.setCreatedBy(currentUser);
        document.setLastEditedBy(currentUser);
        document.setLastEditedAt(LocalDateTime.now());
        document.setVersion(1);
        document.setStatus(CollaborativeDocument.Status.DRAFT);
        document.setCreatedAt(LocalDateTime.now());
        document.setUpdatedAt(LocalDateTime.now());
        
        // 设置标签
        if (documentCreateDTO.getTags() != null) {
            document.setTags(documentCreateDTO.getTags());
        }
        
        CollaborativeDocument savedDocument = documentRepository.save(document);
        
        log.info("创建协作文档: ID={}, 名称={}, 项目ID={}, 创建者={}", 
                savedDocument.getId(), savedDocument.getDocumentName(), 
                project.getId(), currentUser.getUsername());
        
        return convertToDocumentResponse(savedDocument);
    }
    
    @Override
    @Transactional(readOnly = true)
    public DocumentResponseDTO getDocumentById(Long documentId, User currentUser) {
        CollaborativeDocument document = getDocumentById(documentId);
        
        // 检查访问权限
        if (!hasDocumentAccess(document, currentUser)) {
            throw new BusinessException("无权限访问此文档");
        }
        
        return convertToDocumentResponse(document);
    }
    
    @Override
    public DocumentResponseDTO updateDocumentContent(Long documentId, String newContent, User currentUser) {
        CollaborativeDocument document = getDocumentById(documentId);
        
        // 检查编辑权限
        if (!hasDocumentEditPermission(document, currentUser)) {
            throw new BusinessException("无权限编辑此文档");
        }
        
        // 更新文档内容
        if (newContent != null) {
            document.setContent(newContent);
            document.setVersion(document.getVersion() + 1);
        }
        
        document.setLastEditedBy(currentUser);
        document.setLastEditedAt(LocalDateTime.now());
        document.setUpdatedAt(LocalDateTime.now());
        
        CollaborativeDocument updatedDocument = documentRepository.save(document);
        
        log.info("更新协作文档: ID={}, 更新者={}", documentId, currentUser.getUsername());
        return convertToDocumentResponse(updatedDocument);
    }
    
    @Override
    public void deleteDocument(Long documentId, User currentUser) {
        CollaborativeDocument document = getDocumentById(documentId);
        
        // 检查删除权限（只有创建者或项目管理员可以删除）
        if (!document.getCreatedBy().getId().equals(currentUser.getId()) && 
            !hasProjectAdminPermission(document.getProject(), currentUser)) {
            throw new BusinessException("无权限删除此文档");
        }
        
        document.setStatus(CollaborativeDocument.Status.ARCHIVED);
        document.setUpdatedAt(LocalDateTime.now());
        documentRepository.save(document);
        
        log.info("删除协作文档: ID={}, 删除者={}", documentId, currentUser.getUsername());
    }
    
    @Override
    @Transactional(readOnly = true)
    public Page<DocumentResponseDTO> getProjectDocuments(Long projectId, Pageable pageable) {
        CollaborationProject project = projectRepository.findById(projectId)
                .orElseThrow(() -> new BusinessException("项目不存在"));
        return documentRepository.findByProjectOrderByUpdatedAtDesc(project, pageable)
                .map(this::convertToDocumentResponse);
    }
    
    @Override
    @Transactional(readOnly = true)
    public Page<DocumentResponseDTO> searchDocuments(String keyword, Pageable pageable) {
        return documentRepository.findByDocumentNameContainingIgnoreCaseOrderByUpdatedAtDesc(keyword, pageable)
                .map(this::convertToDocumentResponse);
    }
    
    @Override
    @Transactional(readOnly = true)
    public Page<DocumentResponseDTO> getDocumentsByTag(String tag, Pageable pageable) {
        // 简化实现：Repository 中没有直接按标签查询的方法
        return Page.empty();
    }
    
    @Override
    @Transactional(readOnly = true)
    public Page<DocumentResponseDTO> getDocumentsByType(CollaborativeDocument.DocumentType documentType, Pageable pageable) {
        return documentRepository.findByDocumentTypeOrderByUpdatedAtDesc(documentType, pageable)
                .map(this::convertToDocumentResponse);
    }
    
    @Override
    @Transactional(readOnly = true)
    public Page<DocumentResponseDTO> getRecentlyUpdatedDocuments(Pageable pageable) {
        LocalDateTime since = LocalDateTime.now().minusDays(7); // 最近7天
        return documentRepository.findRecentlyEditedSince(since, pageable)
                .map(this::convertToDocumentResponse);
    }
    
    @Override
    @Transactional(readOnly = true)
    public Page<DocumentResponseDTO> getUserDocuments(User user, Pageable pageable) {
        return documentRepository.findByCreatedByOrderByUpdatedAtDesc(user, pageable)
                .map(this::convertToDocumentResponse);
    }
    
    @Override
    @Transactional(readOnly = true)
    public boolean hasDocumentAccess(Long documentId, User user) {
        CollaborativeDocument document = getDocumentById(documentId);
        return hasDocumentAccess(document, user);
    }
    
    @Override
    public DocumentResponseDTO saveDocumentVersion(Long documentId, String commitMessage, User currentUser) {
        CollaborativeDocument document = getDocumentById(documentId);
        
        // 检查编辑权限
        if (!hasDocumentEditPermission(document, currentUser)) {
            throw new BusinessException("无权限保存文档版本");
        }
        
        // 创建新版本（简化实现，实际应该保存到版本历史表）
        document.setVersion(document.getVersion() + 1);
        document.setLastEditedBy(currentUser);
        document.setLastEditedAt(LocalDateTime.now());
        document.setUpdatedAt(LocalDateTime.now());
        
        CollaborativeDocument updatedDocument = documentRepository.save(document);
        
        log.info("保存文档版本: ID={}, 版本={}, 提交信息={}, 用户={}", 
                documentId, document.getVersion(), commitMessage, currentUser.getUsername());
        
        return convertToDocumentResponse(updatedDocument);
    }
    
    @Override
    @Transactional(readOnly = true)
    public Page<DocumentResponseDTO> getDocumentVersions(Long documentId, Pageable pageable) {
        // 简化实现：返回当前文档信息
        CollaborativeDocument document = getDocumentById(documentId);
        List<DocumentResponseDTO> versions = new ArrayList<>();
        versions.add(convertToDocumentResponse(document));
        
        return Page.empty(); // 实际应该查询版本历史表
    }
    
    @Override
    public DocumentResponseDTO restoreToVersion(Long documentId, Integer versionNumber, User currentUser) {
        CollaborativeDocument document = getDocumentById(documentId);
        
        // 检查编辑权限
        if (!hasDocumentEditPermission(document, currentUser)) {
            throw new BusinessException("无权限恢复文档版本");
        }
        
        // 简化实现：实际应该从版本历史中恢复内容
        document.setVersion(versionNumber);
        document.setLastEditedBy(currentUser);
        document.setLastEditedAt(LocalDateTime.now());
        document.setUpdatedAt(LocalDateTime.now());
        
        CollaborativeDocument updatedDocument = documentRepository.save(document);
        
        log.info("恢复文档版本: ID={}, 版本={}, 用户={}", 
                documentId, versionNumber, currentUser.getUsername());
        
        return convertToDocumentResponse(updatedDocument);
    }
    
    @Override
    public void startEditing(Long documentId, User currentUser) {
        CollaborativeDocument document = getDocumentById(documentId);
        
        // 检查访问权限
        if (!hasDocumentAccess(document, currentUser)) {
            throw new BusinessException("无权限编辑此文档");
        }
        
        // 简化实现：实际应该维护编辑状态
        log.info("开始编辑文档: ID={}, 用户={}", documentId, currentUser.getUsername());
    }
    
    @Override
    public void stopEditing(Long documentId, User currentUser) {
        // 简化实现：实际应该清除编辑状态
        log.info("停止编辑文档: ID={}, 用户={}", documentId, currentUser.getUsername());
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<DocumentResponseDTO.UserInfoDTO> getEditingUsers(Long documentId) {
        // 简化实现：返回空列表
        return new ArrayList<>();
    }
    
    @Override
    @Transactional(readOnly = true)
    public DocumentStatsDTO getDocumentStats(Long documentId) {
        getDocumentById(documentId);
        
        DocumentStatsDTO stats = new DocumentStatsDTO();
        stats.setDocumentId(documentId);
        // 使用 DTO 中实际存在的字段
        return stats;
    }
    
    // ==================== 私有方法 ====================
    
    private CollaborativeDocument getDocumentById(Long documentId) {
        return documentRepository.findById(documentId)
                .orElseThrow(() -> new BusinessException("文档不存在"));
    }
    
    private boolean hasProjectPermission(CollaborationProject project, User user) {
        // 项目所有者和成员都有权限
        return project.getOwner().getId().equals(user.getId()) ||
               project.getMembers().stream()
                      .anyMatch(member -> member.getUser().getId().equals(user.getId()) &&
                                        member.getStatus() == com.filesharing.entity.ProjectMember.MemberStatus.ACTIVE);
    }
    
    private boolean hasProjectAdminPermission(CollaborationProject project, User user) {
        // 只有项目所有者和管理员有权限
        if (project.getOwner().getId().equals(user.getId())) {
            return true;
        }
        
        return project.getMembers().stream()
                     .anyMatch(member -> member.getUser().getId().equals(user.getId()) &&
                                       member.getStatus() == com.filesharing.entity.ProjectMember.MemberStatus.ACTIVE &&
                                       (member.getRole() == com.filesharing.entity.ProjectMember.MemberRole.ADMIN ||
                                        member.getRole() == com.filesharing.entity.ProjectMember.MemberRole.ADMIN));
    }
    
    private boolean hasDocumentAccess(CollaborativeDocument document, User user) {
        return hasProjectPermission(document.getProject(), user);
    }
    
    private boolean hasDocumentEditPermission(CollaborativeDocument document, User user) {
        // 文档创建者和项目编辑权限用户可以编辑
        if (document.getCreatedBy().getId().equals(user.getId())) {
            return true;
        }
        
        return hasProjectAdminPermission(document.getProject(), user);
    }
    
    private DocumentResponseDTO convertToDocumentResponse(CollaborativeDocument document) {
        DocumentResponseDTO response = new DocumentResponseDTO();
        response.setId(document.getId());
        response.setDocumentName(document.getDocumentName());
        response.setContent(document.getContent());
        response.setDocumentType(document.getDocumentType());
        response.setVersionNumber(document.getVersion());
        response.setEditCount(0L);
        response.setCreatedAt(document.getCreatedAt());
        response.setUpdatedAt(document.getUpdatedAt());
        response.setLastEditAt(document.getLastEditedAt());
        
        // 设置所有者信息
        DocumentResponseDTO.UserInfoDTO owner = new DocumentResponseDTO.UserInfoDTO();
        owner.setId(document.getCreatedBy().getId());
        owner.setUsername(document.getCreatedBy().getUsername());
        owner.setEmail(document.getCreatedBy().getEmail());
        owner.setAvatar(document.getCreatedBy().getAvatar());
        response.setOwner(owner);
        
        // 设置项目信息
        DocumentResponseDTO.ProjectInfoDTO project = new DocumentResponseDTO.ProjectInfoDTO();
        project.setId(document.getProject().getId());
        project.setProjectName(document.getProject().getProjectName());
        response.setProject(project);
        
        return response;
    }
}