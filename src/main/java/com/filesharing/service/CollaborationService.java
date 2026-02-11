package com.filesharing.service;

import com.filesharing.dto.request.*;
import com.filesharing.dto.response.*;
import com.filesharing.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface CollaborationService {
    
    // 项目管理相关方法
    CollaborationProjectResponse createProject(ProjectCreateRequest request, User currentUser);
    CollaborationProjectResponse getProjectById(Long projectId);
    CollaborationProjectResponse updateProject(Long projectId, ProjectUpdateRequest request, User currentUser);
    void deleteProject(Long projectId, User currentUser);
    Page<CollaborationProjectResponse> getUserProjects(User user, Pageable pageable);
    Page<CollaborationProjectResponse> searchProjects(String keyword, Pageable pageable);
    
    // 项目成员管理相关方法
    ProjectMemberResponse addMemberToProject(Long projectId, MemberAddRequest request, User currentUser);
    void removeMemberFromProject(Long projectId, Long memberId, User currentUser);
    ProjectMemberResponse updateMemberRole(Long projectId, Long memberId, MemberRoleUpdateRequest request, User currentUser);
    Page<ProjectMemberResponse> getProjectMembers(Long projectId, Pageable pageable);
    boolean isUserMemberOfProject(Long projectId, User user);
    List<ProjectMemberResponse> getProjectAdmins(Long projectId);
    
    // 协作文档相关方法
    CollaborativeDocumentResponse createDocument(DocumentCreateRequest request, User currentUser);
    CollaborativeDocumentResponse getDocumentById(Long documentId);
    CollaborativeDocumentResponse updateDocument(Long documentId, DocumentUpdateRequest request, User currentUser);
    void deleteDocument(Long documentId, User currentUser);
    Page<CollaborativeDocumentResponse> getProjectDocuments(Long projectId, Pageable pageable);
    Page<CollaborativeDocumentResponse> searchDocuments(String keyword, Pageable pageable);
    CollaborativeDocumentResponse lockDocument(Long documentId, User currentUser);
    CollaborativeDocumentResponse unlockDocument(Long documentId, User currentUser);
    boolean isDocumentLocked(Long documentId);
    
    // 评论相关方法
    CommentResponse addComment(Long documentId, CommentCreateRequest request, User currentUser);
    CommentResponse updateComment(Long commentId, CommentUpdateRequest request, User currentUser);
    void deleteComment(Long commentId, User currentUser);
    Page<CommentResponse> getDocumentComments(Long documentId, Pageable pageable);
    Page<CommentResponse> getUserComments(User user, Pageable pageable);
    
    // 权限检查方法
    boolean canViewProject(Long projectId, User user);
    boolean canEditProject(Long projectId, User user);
    boolean canManageProject(Long projectId, User user);
    boolean canViewDocument(Long documentId, User user);
    boolean canEditDocument(Long documentId, User user);
    boolean canDeleteDocument(Long documentId, User user);
    boolean canManageDocument(Long documentId, User user);
    
    // 统计相关方法
    ProjectStatisticsResponse getProjectStatistics(Long projectId);
    UserCollaborationStatsResponse getUserCollaborationStats(User user);
}