package com.filesharing.service.impl;

import com.filesharing.dto.request.*;
import com.filesharing.dto.response.*;
import com.filesharing.entity.CollaborationProject;
import com.filesharing.entity.CollaborativeDocument;
import com.filesharing.entity.CollaborativeDocumentSnapshot;
import com.filesharing.entity.Comment;
import com.filesharing.entity.ProjectMember;
import com.filesharing.entity.User;
import com.filesharing.exception.BusinessException;
import com.filesharing.repository.CollaborationProjectRepository;
import com.filesharing.repository.CollaborativeDocumentRepository;
import com.filesharing.repository.CollaborativeDocumentSnapshotRepository;
import com.filesharing.repository.CommentRepository;
import com.filesharing.repository.ProjectMemberRepository;
import com.filesharing.repository.UserRepository;
import com.filesharing.service.CollaborationService;
import com.filesharing.service.DocumentBlockService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 协作服务实现类（简化版）
 * @author Admin
 */
@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class CollaborationServiceImpl implements CollaborationService {

    private static final String DOC_MARKER_PREFIX = "[#DOC:";
    private static final String AUTO_SNAPSHOT_MESSAGE = "自动保存";

    private final CollaborationProjectRepository projectRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final CollaborativeDocumentRepository documentRepository;
    private final CollaborativeDocumentSnapshotRepository snapshotRepository;
    private final CommentRepository commentRepository;
    private final UserRepository userRepository;
    private final DocumentBlockService documentBlockService;
    
    @Override
    public CollaborationProjectResponse getProjectById(Long projectId) {
        CollaborationProject project = findProject(projectId);
        return toProjectResponse(project, null);
    }
    
    @Override
    public CollaborationProjectResponse createProject(ProjectCreateRequest request, User currentUser) {
        if (currentUser == null) {
            throw new BusinessException("用户未登录");
        }
        if (request == null || request.getProjectName() == null || request.getProjectName().isBlank()) {
            throw new BusinessException("项目名称不能为空");
        }

        log.info("创建协作项目: 项目名称={}, 创建者={}", request.getProjectName(), currentUser.getUsername());

        CollaborationProject project = new CollaborationProject();
        project.setProjectName(request.getProjectName().trim());
        project.setDescription(request.getDescription());
        project.setTags(request.getTags());
        project.setStatus(CollaborationProject.ProjectStatus.ACTIVE);
        project.setOwner(currentUser);
        project.setMemberCount(1);
        project.setFileCount(0);
        project.setLastActivity(java.time.LocalDateTime.now());

        CollaborationProject savedProject = projectRepository.save(project);

        ProjectMember ownerMember = new ProjectMember();
        ownerMember.setProject(savedProject);
        ownerMember.setUser(currentUser);
        ownerMember.setRole(ProjectMember.MemberRole.OWNER);
        ownerMember.setStatus(ProjectMember.MemberStatus.ACTIVE);
        ownerMember.setInviteStatus(ProjectMember.InviteStatus.ACCEPTED);
        ownerMember.setInvitedBy(currentUser);
        ownerMember.setLastActivity(java.time.LocalDateTime.now());
        projectMemberRepository.save(ownerMember);

        return toProjectResponse(savedProject, currentUser);
    }
    
    // 注：以下方法已在接口中移除，保留空实现以避免破坏现有调用
    public void inviteUserToCollaborate(Long resourceId, String resourceType, 
                                      String inviteeEmail, String permission, User inviter) {
        // 简化实现
        log.info("邀请用户协作: 资源ID={}, 类型={}, 邀请者={}", resourceId, resourceType, inviter.getUsername());
    }
    
    public List<String> getResourceCollaborators(Long resourceId, String resourceType) {
        // 简化实现
        return new ArrayList<>();
    }
    
    public void updateCollaborationPermission(Long resourceId, String resourceType, 
                                            String collaboratorEmail, String newPermission, User updater) {
        // 简化实现
        log.info("更新协作权限: 资源ID={}, 协作者={}, 更新者={}", resourceId, collaboratorEmail, updater.getUsername());
    }
    
    public void removeCollaborator(Long resourceId, String resourceType, 
                                 String collaboratorEmail, User remover) {
        // 简化实现
        log.info("移除协作者: 资源ID={}, 协作者={}, 移除者={}", resourceId, collaboratorEmail, remover.getUsername());
    }
    
    @Transactional(readOnly = true)
    public boolean hasCollaborationPermission(Long resourceId, String resourceType, 
                                            String userEmail, String requiredPermission) {
        // 简化实现：假设所有者有完全权限
        return true;
    }
    
    // ==================== 以下为简化实现的其他方法 ====================
    
    @Override
    public CollaborationProjectResponse updateProject(Long projectId, ProjectUpdateRequest request, User currentUser) {
        CollaborationProject project = findProject(projectId);
        if (!canManageProject(projectId, currentUser)) {
            throw new BusinessException("无权限修改项目");
        }

        if (request.getProjectName() != null && !request.getProjectName().isBlank()) {
            project.setProjectName(request.getProjectName().trim());
        }
        if (request.getDescription() != null) {
            project.setDescription(request.getDescription());
        }
        if (request.getTags() != null) {
            project.setTags(request.getTags());
        }
        project.setLastActivity(java.time.LocalDateTime.now());

        CollaborationProject saved = projectRepository.save(project);
        log.info("更新项目: 项目ID={}", projectId);
        return toProjectResponse(saved, currentUser);
    }
    
    @Override
    public void deleteProject(Long projectId, User currentUser) {
        CollaborationProject project = findProject(projectId);
        if (!canManageProject(projectId, currentUser)) {
            throw new BusinessException("无权限删除项目");
        }

        List<CollaborativeDocument> projectDocuments = documentRepository
                .findByProjectOrderByUpdatedAtDesc(project, Pageable.unpaged())
                .getContent();
        for (CollaborativeDocument projectDocument : projectDocuments) {
            snapshotRepository.deleteByDocument(projectDocument);
        }

        documentRepository.deleteByProject(project);
        List<Comment> projectComments = commentRepository.findAll().stream()
                .filter(c -> c.getProject() != null && Objects.equals(c.getProject().getId(), projectId))
                .collect(Collectors.toList());
        if (!projectComments.isEmpty()) {
            commentRepository.deleteAll(projectComments);
        }
        projectMemberRepository.deleteByProject(project);
        projectRepository.delete(project);
        log.info("删除项目: 项目ID={}", projectId);
    }
    
    @Override
    public Page<CollaborationProjectResponse> getUserProjects(User user, Pageable pageable) {
        List<CollaborationProjectResponse> results = projectRepository.findProjectsByUser(user).stream()
            .sorted(Comparator.comparing(CollaborationProject::getUpdatedAt,
                Comparator.nullsLast(Comparator.reverseOrder())))
            .map(project -> toProjectResponse(project, user))
            .collect(Collectors.toList());
        return toPage(results, pageable);
    }
    
    @Override
    public Page<CollaborationProjectResponse> searchProjects(String keyword, Pageable pageable) {
        Page<CollaborationProject> page;
        if (keyword == null || keyword.isBlank()) {
            page = projectRepository.findActiveProjects(pageable);
        } else {
            page = projectRepository.findByProjectNameContainingIgnoreCase(keyword.trim(), pageable);
        }
        return page.map(project -> toProjectResponse(project, null));
    }
    
    @Override
    public ProjectMemberResponse addMemberToProject(Long projectId, MemberAddRequest request, User currentUser) {
        CollaborationProject project = findProject(projectId);
        if (!canManageProject(projectId, currentUser)) {
            throw new BusinessException("无权限添加成员");
        }

        User memberUser = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new BusinessException("邀请用户不存在"));

        ProjectMember member = projectMemberRepository.findByProjectAndUser(project, memberUser)
                .orElseGet(ProjectMember::new);
        member.setProject(project);
        member.setUser(memberUser);
        member.setRole(parseMemberRole(request.getRole()));
        member.setStatus(ProjectMember.MemberStatus.ACTIVE);
        member.setInviteStatus(ProjectMember.InviteStatus.ACCEPTED);
        member.setInvitedBy(currentUser);
        member.setLastActivity(java.time.LocalDateTime.now());

        ProjectMember saved = projectMemberRepository.save(member);
        project.setMemberCount(Math.toIntExact(projectMemberRepository.countByProject(project)));
        project.setLastActivity(java.time.LocalDateTime.now());
        projectRepository.save(project);

        log.info("添加项目成员: 项目ID={}, 用户ID={}", projectId, memberUser.getId());
        return toMemberResponse(saved);
    }
    
    @Override
    public void removeMemberFromProject(Long projectId, Long memberId, User currentUser) {
        CollaborationProject project = findProject(projectId);
        if (!canManageProject(projectId, currentUser)) {
            throw new BusinessException("无权限移除成员");
        }

        ProjectMember member = projectMemberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException("成员不存在"));
        if (!Objects.equals(member.getProject().getId(), projectId)) {
            throw new BusinessException("成员不属于该项目");
        }
        if (member.getRole() == ProjectMember.MemberRole.OWNER) {
            throw new BusinessException("不能移除项目所有者");
        }

        projectMemberRepository.delete(member);
        project.setMemberCount(Math.toIntExact(projectMemberRepository.countByProject(project)));
        project.setLastActivity(java.time.LocalDateTime.now());
        projectRepository.save(project);
        log.info("移除项目成员: 项目ID={}, 成员ID={}", projectId, memberId);
    }
    
    @Override
    public ProjectMemberResponse updateMemberRole(Long projectId, Long memberId, MemberRoleUpdateRequest request, User currentUser) {
        if (!canManageProject(projectId, currentUser)) {
            throw new BusinessException("无权限更新成员角色");
        }

        ProjectMember member = projectMemberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException("成员不存在"));
        if (!Objects.equals(member.getProject().getId(), projectId)) {
            throw new BusinessException("成员不属于该项目");
        }
        if (member.getRole() == ProjectMember.MemberRole.OWNER) {
            throw new BusinessException("不能修改项目所有者角色");
        }

        member.setRole(parseMemberRole(request.getRole()));
        if (request.getPermissions() != null) {
            member.setPermissions(request.getPermissions());
        }
        member.setLastActivity(java.time.LocalDateTime.now());
        ProjectMember saved = projectMemberRepository.save(member);

        log.info("更新成员角色: 项目ID={}, 成员ID={}", projectId, memberId);
        return toMemberResponse(saved);
    }
    
    @Override
    public Page<ProjectMemberResponse> getProjectMembers(Long projectId, Pageable pageable) {
        CollaborationProject project = findProject(projectId);
        List<ProjectMemberResponse> members = projectMemberRepository.findByProject(project).stream()
            .sorted(Comparator.comparing(ProjectMember::getJoinedAt,
                Comparator.nullsLast(Comparator.reverseOrder())))
            .map(this::toMemberResponse)
            .collect(Collectors.toList());
        return toPage(members, pageable);
    }
    
    @Override
    public boolean isUserMemberOfProject(Long projectId, User user) {
        CollaborationProject project = findProject(projectId);
        if (isOwner(project, user)) {
            return true;
        }
        return projectMemberRepository.findByProjectAndUser(project, user)
                .filter(this::isActiveMember)
                .isPresent();
    }
    
    @Override
    public List<ProjectMemberResponse> getProjectAdmins(Long projectId) {
        CollaborationProject project = findProject(projectId);
        return projectMemberRepository.findByProject(project).stream()
            .filter(member -> member.getRole() == ProjectMember.MemberRole.OWNER
                || member.getRole() == ProjectMember.MemberRole.ADMIN)
            .map(this::toMemberResponse)
            .collect(Collectors.toList());
    }
    
    @Override
    public CollaborativeDocumentResponse createDocument(DocumentCreateRequest request, User currentUser) {
        CollaborationProject project = findProject(request.getProjectId());
        if (!canEditProject(project.getId(), currentUser)) {
            throw new BusinessException("无权限在该项目中创建文档");
        }

        CollaborativeDocument document = new CollaborativeDocument();
        document.setProject(project);
        document.setDocumentName(request.getTitle());
        document.setDocumentType(parseDocumentType(request.getDocumentType()));
        document.setContent(request.getDescription());
        document.setCreatedBy(currentUser);
        document.setLastEditedBy(currentUser);
        document.setLastEditedAt(java.time.LocalDateTime.now());
        document.setVersion(1);
        document.setStatus(CollaborativeDocument.Status.DRAFT);
        document.setIsLocked(false);
        document.setCommentCount(0);

        CollaborativeDocument saved = documentRepository.save(document);
        int memberCount = (int) Math.max(1L, projectMemberRepository.countByProject(project));
        documentBlockService.initDocumentBlocks(saved.getId(), memberCount, document.getContent());
        persistDocumentSnapshot(saved, currentUser, "初始化版本");

        project.setFileCount(Math.toIntExact(documentRepository.countByProject(project)));
        project.setLastActivity(java.time.LocalDateTime.now());
        projectRepository.save(project);

        log.info("创建协作文档: 文档ID={}, 文档名称={}", saved.getId(), request.getTitle());
        return toDocumentResponse(saved, currentUser);
    }
    
    @Override
    public CollaborativeDocumentResponse getDocumentById(Long documentId) {
        CollaborativeDocument document = findDocument(documentId);
        log.info("获取协作文档: 文档ID={}", documentId);
        return toDocumentResponse(document, null);
    }
    
    @Override
    public CollaborativeDocumentResponse updateDocument(Long documentId, DocumentUpdateRequest request, User currentUser) {
        CollaborativeDocument document = findDocument(documentId);
        if (!canEditDocument(documentId, currentUser)) {
            throw new BusinessException("无权限编辑该文档");
        }
        if (Boolean.TRUE.equals(document.getIsLocked())
                && document.getLockedBy() != null
                && !Objects.equals(document.getLockedBy().getId(), currentUser.getId())
                && !canManageProject(document.getProject().getId(), currentUser)) {
            throw new BusinessException("文档已被其他用户锁定");
        }

        if (request.getTitle() != null && !request.getTitle().isBlank()) {
            document.setDocumentName(request.getTitle());
        }
        if (request.getContent() != null) {
            document.setContent(request.getContent());
        }
        document.setVersion((document.getVersion() == null ? 0 : document.getVersion()) + 1);
        document.setLastEditedBy(currentUser);
        document.setLastEditedAt(java.time.LocalDateTime.now());

        CollaborativeDocument saved = documentRepository.save(document);
        persistDocumentSnapshot(saved, currentUser, AUTO_SNAPSHOT_MESSAGE);
        log.info("更新协作文档: 文档ID={}", documentId);
        return toDocumentResponse(saved, currentUser);
    }
    
    @Override
    public void deleteDocument(Long documentId, User currentUser) {
        CollaborativeDocument document = findDocument(documentId);
        if (!canDeleteDocument(documentId, currentUser)) {
            throw new BusinessException("无权限删除该文档");
        }
        snapshotRepository.deleteByDocument(document);
        documentRepository.delete(document);

        CollaborationProject project = document.getProject();
        project.setFileCount(Math.toIntExact(documentRepository.countByProject(project)));
        project.setLastActivity(java.time.LocalDateTime.now());
        projectRepository.save(project);

        log.info("删除协作文档: 文档ID={}", documentId);
    }
    
    @Override
    public Page<CollaborativeDocumentResponse> getProjectDocuments(Long projectId, Pageable pageable) {
        CollaborationProject project = findProject(projectId);
        return documentRepository.findByProjectOrderByUpdatedAtDesc(project, pageable)
                .map(doc -> toDocumentResponse(doc, null));
    }
    
    @Override
    public Page<CollaborativeDocumentResponse> searchDocuments(String keyword, Pageable pageable) {
        String safeKeyword = keyword == null ? "" : keyword.trim();
        return documentRepository.findByDocumentNameContainingIgnoreCaseOrderByUpdatedAtDesc(safeKeyword, pageable)
                .map(doc -> toDocumentResponse(doc, null));
    }
    
    @Override
    public CollaborativeDocumentResponse lockDocument(Long documentId, User currentUser) {
        CollaborativeDocument document = findDocument(documentId);
        if (!canEditDocument(documentId, currentUser)) {
            throw new BusinessException("无权限锁定文档");
        }

        if (Boolean.TRUE.equals(document.getIsLocked())
                && document.getLockedBy() != null
                && !Objects.equals(document.getLockedBy().getId(), currentUser.getId())
                && !canManageProject(document.getProject().getId(), currentUser)) {
            throw new BusinessException("文档已被其他用户锁定");
        }

        document.setIsLocked(true);
        document.setLockedBy(currentUser);
        document.setLockedAt(java.time.LocalDateTime.now());
        CollaborativeDocument saved = documentRepository.save(document);

        log.info("锁定文档: 文档ID={}, 用户ID={}", documentId, currentUser.getId());
        return toDocumentResponse(saved, currentUser);
    }
    
    @Override
    public CollaborativeDocumentResponse unlockDocument(Long documentId, User currentUser) {
        CollaborativeDocument document = findDocument(documentId);
        if (!Boolean.TRUE.equals(document.getIsLocked())) {
            return toDocumentResponse(document, currentUser);
        }

        boolean isLocker = document.getLockedBy() != null
                && Objects.equals(document.getLockedBy().getId(), currentUser.getId());
        if (!isLocker && !canManageProject(document.getProject().getId(), currentUser)) {
            throw new BusinessException("无权限解锁文档");
        }

        document.setIsLocked(false);
        document.setLockedBy(null);
        document.setLockedAt(null);
        CollaborativeDocument saved = documentRepository.save(document);

        log.info("解锁文档: 文档ID={}, 用户ID={}", documentId, currentUser.getId());
        return toDocumentResponse(saved, currentUser);
    }
    
    @Override
    public boolean isDocumentLocked(Long documentId) {
        return Boolean.TRUE.equals(findDocument(documentId).getIsLocked());
    }

    @Override
    public Page<CollaborativeDocumentSnapshotResponse> getDocumentSnapshots(Long documentId, Pageable pageable, User currentUser) {
        CollaborativeDocument document = findDocument(documentId);
        if (!canViewDocument(documentId, currentUser)) {
            throw new BusinessException("无权限查看文档快照");
        }

        return snapshotRepository.findByDocumentOrderByCreatedAtDesc(document, pageable)
                .map(this::toSnapshotResponse);
    }

    @Override
    public CollaborativeDocumentResponse restoreDocumentSnapshot(Long documentId, Long snapshotId, User currentUser) {
        CollaborativeDocument document = findDocument(documentId);
        if (!canEditDocument(documentId, currentUser)) {
            throw new BusinessException("无权限恢复文档快照");
        }

        CollaborativeDocumentSnapshot snapshot = snapshotRepository.findById(snapshotId)
                .orElseThrow(() -> new BusinessException("文档快照不存在"));
        if (!Objects.equals(snapshot.getDocument().getId(), documentId)) {
            throw new BusinessException("快照不属于当前文档");
        }

        document.setDocumentName(snapshot.getTitle());
        document.setContent(snapshot.getContent());
        document.setVersion((document.getVersion() == null ? 0 : document.getVersion()) + 1);
        document.setLastEditedBy(currentUser);
        document.setLastEditedAt(java.time.LocalDateTime.now());

        CollaborativeDocument saved = documentRepository.save(document);
        persistDocumentSnapshot(saved, currentUser, "恢复自快照 #" + snapshot.getVersionNumber());

        return toDocumentResponse(saved, currentUser);
    }
    
    @Override
    public CommentResponse addComment(Long documentId, CommentCreateRequest request, User currentUser) {
        CollaborativeDocument document = findDocument(documentId);
        if (!canViewDocument(documentId, currentUser)) {
            throw new BusinessException("无权限评论该文档");
        }

        Comment comment = new Comment();
        comment.setProject(document.getProject());
        comment.setAuthor(currentUser);
        comment.setCommentType(Comment.CommentType.DISCUSSION);
        comment.setParentId(request.getParentCommentId());
        comment.setContent(encodeDocumentComment(documentId, request.getContent()));
        comment.setIsResolved(false);

        Comment saved = commentRepository.save(comment);

        document.setCommentCount((document.getCommentCount() == null ? 0 : document.getCommentCount()) + 1);
        document.setLastEditedAt(java.time.LocalDateTime.now());
        documentRepository.save(document);

        log.info("添加评论: 文档ID={}, 评论ID={}", documentId, saved.getId());
        return toCommentResponse(saved, documentId, currentUser);
    }
    
    @Override
    public CommentResponse updateComment(Long commentId, CommentUpdateRequest request, User currentUser) {
        Comment comment = findComment(commentId);
        boolean isAuthor = comment.getAuthor() != null && Objects.equals(comment.getAuthor().getId(), currentUser.getId());
        if (!isAuthor && !canManageProject(comment.getProject().getId(), currentUser)) {
            throw new BusinessException("无权限修改评论");
        }

        Long documentId = extractDocumentId(comment.getContent());
        comment.setContent(encodeDocumentComment(documentId, request.getContent()));
        Comment saved = commentRepository.save(comment);

        log.info("更新评论: 评论ID={}", commentId);
        return toCommentResponse(saved, documentId, currentUser);
    }
    
    @Override
    public void deleteComment(Long commentId, User currentUser) {
        Comment comment = findComment(commentId);
        boolean isAuthor = comment.getAuthor() != null && Objects.equals(comment.getAuthor().getId(), currentUser.getId());
        if (!isAuthor && !canManageProject(comment.getProject().getId(), currentUser)) {
            throw new BusinessException("无权限删除评论");
        }

        Long documentId = extractDocumentId(comment.getContent());
        List<Comment> replies = commentRepository.findByParentId(commentId);
        if (!replies.isEmpty()) {
            commentRepository.deleteAll(replies);
        }
        commentRepository.delete(comment);

        if (documentId != null) {
            documentRepository.findById(documentId).ifPresent(document -> {
                int currentCount = document.getCommentCount() == null ? 0 : document.getCommentCount();
                document.setCommentCount(Math.max(currentCount - 1, 0));
                document.setLastEditedAt(java.time.LocalDateTime.now());
                documentRepository.save(document);
            });
        }

        log.info("删除评论: 评论ID={}", commentId);
    }
    
    @Override
    public Page<CommentResponse> getDocumentComments(Long documentId, Pageable pageable) {
        CollaborativeDocument document = findDocument(documentId);
        List<CommentResponse> comments = commentRepository.findAll().stream()
                .filter(c -> c.getProject() != null && Objects.equals(c.getProject().getId(), document.getProject().getId()))
                .filter(c -> Objects.equals(extractDocumentId(c.getContent()), documentId))
                .sorted(Comparator.comparing(Comment::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .map(c -> toCommentResponse(c, documentId, null))
                .collect(Collectors.toList());
        return toPage(comments, pageable);
    }
    
    @Override
    public Page<CommentResponse> getUserComments(User user, Pageable pageable) {
        return commentRepository.findByAuthorOrderByCreatedAtDesc(user, pageable)
                .map(comment -> toCommentResponse(comment, extractDocumentId(comment.getContent()), user));
    }
    
    @Override
    public boolean canViewProject(Long projectId, User user) {
        CollaborationProject project = findProject(projectId);
        if (Boolean.TRUE.equals(project.getIsPublic())) {
            return true;
        }
        if (user == null) {
            return false;
        }
        if (isOwner(project, user)) {
            return true;
        }
        return projectMemberRepository.findByProjectAndUser(project, user)
                .filter(this::isActiveMember)
                .isPresent();
    }
    
    @Override
    public boolean canEditProject(Long projectId, User user) {
        CollaborationProject project = findProject(projectId);
        if (user == null) {
            return false;
        }
        if (isOwner(project, user)) {
            return true;
        }
        return projectMemberRepository.findByProjectAndUser(project, user)
                .filter(this::isActiveMember)
                .map(member -> member.getRole() == ProjectMember.MemberRole.ADMIN
                        || member.getRole() == ProjectMember.MemberRole.MEMBER)
                .orElse(false);
    }
    
    @Override
    public boolean canManageProject(Long projectId, User user) {
        CollaborationProject project = findProject(projectId);
        if (user == null) {
            return false;
        }
        if (isOwner(project, user)) {
            return true;
        }
        return projectMemberRepository.findByProjectAndUser(project, user)
                .filter(this::isActiveMember)
                .map(member -> member.getRole() == ProjectMember.MemberRole.ADMIN)
                .orElse(false);
    }
    
    @Override
    public boolean canViewDocument(Long documentId, User user) {
        CollaborativeDocument document = findDocument(documentId);
        return canViewProject(document.getProject().getId(), user);
    }
    
    @Override
    public boolean canEditDocument(Long documentId, User user) {
        CollaborativeDocument document = findDocument(documentId);
        if (!canEditProject(document.getProject().getId(), user)) {
            return false;
        }
        if (!Boolean.TRUE.equals(document.getIsLocked())) {
            return true;
        }
        return document.getLockedBy() != null && user != null
                && Objects.equals(document.getLockedBy().getId(), user.getId());
    }
    
    @Override
    public boolean canDeleteDocument(Long documentId, User user) {
        CollaborativeDocument document = findDocument(documentId);
        if (user == null) {
            return false;
        }
        if (canManageProject(document.getProject().getId(), user)) {
            return true;
        }
        return document.getCreatedBy() != null && Objects.equals(document.getCreatedBy().getId(), user.getId());
    }
    
    @Override
    public boolean canManageDocument(Long documentId, User user) {
        CollaborativeDocument document = findDocument(documentId);
        return canManageProject(document.getProject().getId(), user)
                || (user != null && document.getCreatedBy() != null
                && Objects.equals(document.getCreatedBy().getId(), user.getId()));
    }
    
    @Override
    public ProjectStatisticsResponse getProjectStatistics(Long projectId) {
        log.info("获取项目统计: 项目ID={}", projectId);
        CollaborationProject project = findProject(projectId);
        long totalMembers = projectMemberRepository.countByProject(project);
        long totalDocuments = documentRepository.countByProject(project);

        List<CollaborativeDocument> documents = documentRepository.findByProjectOrderByUpdatedAtDesc(
            project,
            Pageable.unpaged()
        ).getContent();
        long activeDocuments = documents.stream()
            .filter(doc -> doc.getStatus() != CollaborativeDocument.Status.ARCHIVED)
            .count();
        long lockedDocuments = documents.stream().filter(doc -> Boolean.TRUE.equals(doc.getIsLocked())).count();

        long totalComments = commentRepository.findAll().stream()
            .filter(c -> c.getProject() != null && Objects.equals(c.getProject().getId(), projectId))
            .count();

        return ProjectStatisticsResponse.builder()
            .projectId(project.getId())
            .projectName(project.getProjectName())
            .totalMembers(totalMembers)
            .totalDocuments(totalDocuments)
            .totalComments(totalComments)
            .activeDocuments(activeDocuments)
            .lockedDocuments(lockedDocuments)
            .build();
    }
    
    @Override
    public UserCollaborationStatsResponse getUserCollaborationStats(User user) {
        log.info("获取用户协作统计: 用户ID={}", user.getId());
        long projectsOwned = projectRepository.countByOwner(user);
        long totalProjects = projectRepository.findProjectsByUser(user).size();
        long projectsParticipated = Math.max(totalProjects - projectsOwned, 0);

        long totalDocuments = documentRepository.findAll().stream()
                .filter(doc -> doc.getCreatedBy() != null && Objects.equals(doc.getCreatedBy().getId(), user.getId()))
                .count();

        long totalComments = commentRepository.findByAuthorOrderByCreatedAtDesc(user, Pageable.unpaged()).getTotalElements();

        return UserCollaborationStatsResponse.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .totalProjects(totalProjects)
                .projectsOwned(projectsOwned)
                .projectsParticipated(projectsParticipated)
                .totalDocuments(totalDocuments)
                .totalComments(totalComments)
                .build();
    }

    private CollaborationProject findProject(Long projectId) {
        return projectRepository.findById(projectId)
                .orElseThrow(() -> new BusinessException("项目不存在"));
    }

    private CollaborativeDocument findDocument(Long documentId) {
        return documentRepository.findById(documentId)
                .orElseThrow(() -> new BusinessException("文档不存在"));
    }

    private Comment findComment(Long commentId) {
        return commentRepository.findById(commentId)
                .orElseThrow(() -> new BusinessException("评论不存在"));
    }

    private boolean isOwner(CollaborationProject project, User user) {
        return user != null
                && project.getOwner() != null
                && Objects.equals(project.getOwner().getId(), user.getId());
    }

    private boolean isActiveMember(ProjectMember member) {
        return member.getStatus() == ProjectMember.MemberStatus.ACTIVE
                && member.getInviteStatus() == ProjectMember.InviteStatus.ACCEPTED;
    }

    private ProjectMember.MemberRole parseMemberRole(String role) {
        if (role == null || role.isBlank()) {
            return ProjectMember.MemberRole.MEMBER;
        }
        String normalized = role.trim().toUpperCase(Locale.ROOT);
        return switch (normalized) {
            case "OWNER" -> ProjectMember.MemberRole.OWNER;
            case "ADMIN" -> ProjectMember.MemberRole.ADMIN;
            case "VIEWER" -> ProjectMember.MemberRole.VIEWER;
            case "EDITOR", "MEMBER" -> ProjectMember.MemberRole.MEMBER;
            default -> ProjectMember.MemberRole.MEMBER;
        };
    }

    private CollaborativeDocument.DocumentType parseDocumentType(String documentType) {
        if (documentType == null || documentType.isBlank()) {
            return CollaborativeDocument.DocumentType.TEXT;
        }
        String normalized = documentType.trim().toUpperCase(Locale.ROOT);
        return switch (normalized) {
            case "MARKDOWN" -> CollaborativeDocument.DocumentType.MARKDOWN;
            case "WIKI" -> CollaborativeDocument.DocumentType.WIKI;
            default -> CollaborativeDocument.DocumentType.TEXT;
        };
    }

    private CollaborationProjectResponse toProjectResponse(CollaborationProject project, User currentUser) {
        long memberCount = projectMemberRepository.countByProject(project);
        long documentCount = documentRepository.countByProject(project);

        return CollaborationProjectResponse.builder()
                .id(project.getId())
                .projectName(project.getProjectName())
                .description(project.getDescription())
                .tags(project.getTags())
                .status(project.getStatus() == null ? null : project.getStatus().name())
                .owner(toUserSimple(project.getOwner()))
                .memberCount(memberCount)
                .documentCount(documentCount)
                .createdAt(project.getCreatedAt())
                .updatedAt(project.getUpdatedAt())
                .currentUserIsOwner(currentUser != null && isOwner(project, currentUser))
                .currentUserIsMember(currentUser != null && isUserMemberOfProject(project.getId(), currentUser))
                .build();
    }

    private ProjectMemberResponse toMemberResponse(ProjectMember member) {
        boolean canManage = member.getRole() == ProjectMember.MemberRole.OWNER
                || member.getRole() == ProjectMember.MemberRole.ADMIN;
        boolean canEdit = canManage || member.getRole() == ProjectMember.MemberRole.MEMBER;

        return ProjectMemberResponse.builder()
                .id(member.getId())
                .user(toUserSimple(member.getUser()))
                .role(member.getRole() == null ? null : member.getRole().name())
                .status(member.getStatus() == null ? null : member.getStatus().name())
                .invitedByEmail(member.getInvitedBy() == null ? null : member.getInvitedBy().getEmail())
                .canEdit(canEdit)
                .canManage(canManage)
                .build();
    }

    private CollaborativeDocumentResponse toDocumentResponse(CollaborativeDocument document, User currentUser) {
        return CollaborativeDocumentResponse.builder()
                .id(document.getId())
                .title(document.getDocumentName())
                .description(null)
                .documentType(document.getDocumentType() == null ? null : document.getDocumentType().name())
                .status(document.getStatus() == null ? null : document.getStatus().name())
                .content(document.getContent())
                .createdBy(toUserSimple(document.getCreatedBy()))
                .lastEditedBy(toUserSimple(document.getLastEditedBy()))
                .projectId(document.getProject() == null ? null : document.getProject().getId())
                .projectName(document.getProject() == null ? null : document.getProject().getProjectName())
                .createdAt(document.getCreatedAt())
                .updatedAt(document.getUpdatedAt())
                .lastEditedAt(document.getLastEditedAt())
                .isLocked(Boolean.TRUE.equals(document.getIsLocked()))
                .lockedBy(toUserSimple(document.getLockedBy()))
                .commentCount(document.getCommentCount() == null ? 0L : document.getCommentCount().longValue())
                .canEdit(currentUser != null && canEditDocument(document.getId(), currentUser))
                .canDelete(currentUser != null && canDeleteDocument(document.getId(), currentUser))
                .build();
    }

    private CommentResponse toCommentResponse(Comment comment, Long documentId, User currentUser) {
        boolean canManageProject = currentUser != null && comment.getProject() != null
                && canManageProject(comment.getProject().getId(), currentUser);
        boolean isAuthor = currentUser != null && comment.getAuthor() != null
                && Objects.equals(comment.getAuthor().getId(), currentUser.getId());

        String plainContent = decodeCommentContent(comment.getContent());
        int replyCount = commentRepository.findByParentId(comment.getId()).size();

        return CommentResponse.builder()
                .id(comment.getId())
                .content(plainContent)
                .author(toUserSimple(comment.getAuthor()))
                .documentId(documentId)
                .parentCommentId(comment.getParentId())
                .createdAt(comment.getCreatedAt())
                .updatedAt(comment.getUpdatedAt())
                .canEdit(isAuthor || canManageProject)
                .canDelete(isAuthor || canManageProject)
                .replyCount(replyCount)
                .build();
    }

    private UserSimpleResponse toUserSimple(User user) {
        if (user == null) {
            return null;
        }
        return UserSimpleResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .nickname(user.getNickname())
                .avatar(user.getAvatar())
                .build();
    }

    private void persistDocumentSnapshot(CollaborativeDocument document, User operator, String commitMessage) {
        if (document == null || document.getId() == null) {
            return;
        }

        Optional<CollaborativeDocumentSnapshot> latest = snapshotRepository.findTopByDocumentOrderByVersionNumberDesc(document);
        if (latest.isPresent()) {
            CollaborativeDocumentSnapshot latestSnapshot = latest.get();
            boolean titleUnchanged = Objects.equals(latestSnapshot.getTitle(), document.getDocumentName());
            boolean contentUnchanged = Objects.equals(latestSnapshot.getContent(), document.getContent());
            if (titleUnchanged && contentUnchanged) {
                return;
            }
        }

        CollaborativeDocumentSnapshot snapshot = new CollaborativeDocumentSnapshot();
        snapshot.setDocument(document);
        snapshot.setVersionNumber(document.getVersion() == null ? 1 : document.getVersion());
        snapshot.setTitle(document.getDocumentName() == null ? "未命名文档" : document.getDocumentName());
        snapshot.setContent(document.getContent());
        snapshot.setCommitMessage(commitMessage);
        snapshot.setCreatedBy(operator);
        snapshotRepository.save(snapshot);
    }

    private CollaborativeDocumentSnapshotResponse toSnapshotResponse(CollaborativeDocumentSnapshot snapshot) {
        return CollaborativeDocumentSnapshotResponse.builder()
                .id(snapshot.getId())
                .documentId(snapshot.getDocument() == null ? null : snapshot.getDocument().getId())
                .versionNumber(snapshot.getVersionNumber())
                .title(snapshot.getTitle())
                .commitMessage(snapshot.getCommitMessage())
                .createdBy(toUserSimple(snapshot.getCreatedBy()))
                .createdAt(snapshot.getCreatedAt())
                .build();
    }

    private String encodeDocumentComment(Long documentId, String content) {
        String safeContent = content == null ? "" : content;
        if (documentId == null) {
            return safeContent;
        }
        return DOC_MARKER_PREFIX + documentId + "]" + safeContent;
    }

    private Long extractDocumentId(String content) {
        if (content == null || !content.startsWith(DOC_MARKER_PREFIX)) {
            return null;
        }
        int end = content.indexOf(']');
        if (end <= DOC_MARKER_PREFIX.length()) {
            return null;
        }
        try {
            return Long.parseLong(content.substring(DOC_MARKER_PREFIX.length(), end));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String decodeCommentContent(String content) {
        if (content == null) {
            return null;
        }
        if (!content.startsWith(DOC_MARKER_PREFIX)) {
            return content;
        }
        int end = content.indexOf(']');
        if (end < 0 || end + 1 >= content.length()) {
            return "";
        }
        return content.substring(end + 1);
    }

    private <T> Page<T> toPage(List<T> source, Pageable pageable) {
        int total = source.size();
        int from = Math.min((int) pageable.getOffset(), total);
        int to = Math.min(from + pageable.getPageSize(), total);
        return new PageImpl<>(source.subList(from, to), pageable, total);
    }
}