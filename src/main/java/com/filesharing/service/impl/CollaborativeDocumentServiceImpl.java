package com.filesharing.service.impl;

import com.filesharing.dto.request.DocumentCreateRequest;
import com.filesharing.dto.response.DocumentResponseDTO;
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
import java.util.List;
import java.util.stream.Collectors;

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
    public DocumentResponseDTO createDocument(DocumentCreateRequest request, User currentUser) {
        // 验证项目权限
        CollaborationProject project = projectRepository.findById(request.getProjectId())
                .orElseThrow(() -> new BusinessException("项目不存在"));
        
        // 检查创建权限
        if (!hasProjectPermission(project, currentUser)) {
            throw new BusinessException("无权限在此项目中创建文档");
        }
        
        // 创建文档
        CollaborativeDocument document = new CollaborativeDocument();
        document.setProject(project);
        document.setDocumentName(request.getDocumentName());
        document.setContent(request.getContent() != null ? request.getContent() : "");
        document.setDocumentType(CollaborativeDocument.DocumentType.TEXT);
        document.setCreatedBy(currentUser);
        document.setLastEditedBy(currentUser);
        document.setLastEditedAt(LocalDateTime.now());
        document.setVersion(1);
        document.setStatus(CollaborativeDocument.DocumentStatus.DRAFT);
        document.setCreatedAt(LocalDateTime.now());
        document.setUpdatedAt(LocalDateTime.now());
        
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
    public DocumentResponseDTO updateDocument(Long documentId, DocumentCreateRequest request, User currentUser) {
        CollaborativeDocument document = getDocumentById(documentId);
        
        // 检查编辑权限
        if (!hasDocumentEditPermission(document, currentUser)) {
            throw new BusinessException("无权限编辑此文档");
        }
        
        // 更新文档内容
        if (request.getDocumentName() != null) {
            document.setDocumentName(request.getDocumentName());
        }
        if (request.getContent() != null) {
            document.setContent(request.getContent());
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
        
        document.setStatus(CollaborativeDocument.DocumentStatus.DELETED);
        document.setUpdatedAt(LocalDateTime.now());
        documentRepository.save(document);
        
        log.info("删除协作文档: ID={}, 删除者={}", documentId, currentUser.getUsername());
    }
    
    @Override
    @Transactional(readOnly = true)
    public Page<DocumentResponseDTO> getProjectDocuments(Long projectId, Pageable pageable) {
        return documentRepository.findByProjectId(projectId, pageable)
                .map(this::convertToDocumentResponse);
    }
    
    @Override
    @Transactional(readOnly = true)
    public Page<DocumentResponseDTO> searchDocuments(String keyword, Pageable pageable) {
        return documentRepository.findByContentContainingIgnoreCase(keyword, pageable)
                .map(this::convertToDocumentResponse);
    }
    
    @Override
    @Transactional(readOnly = true)
    public Page<DocumentResponseDTO> getDocumentsByTag(String tag, Pageable pageable) {
        return documentRepository.findByTagsContaining(tag, pageable)
                .map(this::convertToDocumentResponse);
    }
    
    @Override
    @Transactional(readOnly = true)
    public Page<DocumentResponseDTO> getDocumentsByType(CollaborativeDocument.DocumentType documentType, Pageable pageable) {
        return documentRepository.findByDocumentType(documentType, pageable)
                .map(this::convertToDocumentResponse);
    }
    
    @Override
    @Transactional(readOnly = true)
    public Page<DocumentResponseDTO> getRecentlyUpdatedDocuments(Pageable pageable) {
        return documentRepository.findRecentlyUpdatedDocuments(pageable)
                .map(this::convertToDocumentResponse);
    }
    
    @Override
    @Transactional(readOnly = true)
    public Page<DocumentResponseDTO> getUserDocuments(User user, Pageable pageable) {
        return documentRepository.findByCreatedBy(user, pageable)
                .map(this::convertToDocumentResponse);
    }
    
    @Override
    @Transactional(readOnly = true)
    public boolean hasDocumentAccess(Long documentId, User user) {
        CollaborativeDocument document = getDocumentById(documentId);
        return hasDocumentAccess(document, user);
    }
    
    @Override
    @Transactional(readOnly = true)
    public DocumentStatsDTO getDocumentStats(Long documentId) {
        CollaborativeDocument document = getDocumentById(documentId);
        
        DocumentStatsDTO stats = new DocumentStatsDTO();
        stats.setDocumentId(documentId);
        stats.setViewCount(0L); // 简化实现
        stats.setEditCount(0L); // 简化实现
        stats.setCommentCount(0L); // 简化实现
        stats.setShareCount(0L); // 简化实现
        stats.setLastEditedAt(document.getLastEditedAt());
        stats.setVersionCount(document.getVersion());
        
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
                                        member.getRole() == com.filesharing.entity.ProjectMember.MemberRole.EDITOR));
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
        response.setDocumentType(document.getDocumentType().name());
        response.setVersion(document.getVersion());
        response.setStatus(document.getStatus().name());
        response.setProjectId(document.getProject().getId());
        response.setProjectName(document.getProject().getProjectName());
        response.setCreatedById(document.getCreatedBy().getId());
        response.setCreatedByName(document.getCreatedBy().getUsername());
        response.setLastEditedById(document.getLastEditedBy().getId());
        response.setLastEditedByName(document.getLastEditedBy().getUsername());
        response.setLastEditedAt(document.getLastEditedAt());
        response.setCreatedAt(document.getCreatedAt());
        response.setUpdatedAt(document.getUpdatedAt());
        return response;
    }
}