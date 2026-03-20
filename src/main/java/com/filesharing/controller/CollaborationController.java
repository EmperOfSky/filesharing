package com.filesharing.controller;

import com.filesharing.dto.ApiResponse;
import com.filesharing.dto.request.CommentCreateRequest;
import com.filesharing.dto.request.CommentUpdateRequest;
import com.filesharing.dto.request.DocumentCreateRequest;
import com.filesharing.dto.request.DocumentUpdateRequest;
import com.filesharing.dto.request.MemberAddRequest;
import com.filesharing.dto.request.MemberRoleUpdateRequest;
import com.filesharing.dto.request.ProjectCreateRequest;
import com.filesharing.dto.request.ProjectUpdateRequest;
import com.filesharing.dto.response.CollaborationProjectResponse;
import com.filesharing.dto.response.CollaborativeDocumentResponse;
import com.filesharing.dto.response.CollaborativeDocumentSnapshotResponse;
import com.filesharing.dto.response.CommentResponse;
import com.filesharing.dto.response.ProjectMemberResponse;
import com.filesharing.dto.response.ProjectStatisticsResponse;
import com.filesharing.dto.response.UserCollaborationStatsResponse;
import com.filesharing.entity.User;
import com.filesharing.service.CollaborationService;
import com.filesharing.service.DocumentBlockService;
import com.filesharing.service.RealTimeCollaborationService;
import com.filesharing.service.UserService;
import com.filesharing.websocket.CollaborationWebSocketHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 协作模块控制器。
 */
@Slf4j
@RestController
@RequestMapping("/api/collaboration")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class CollaborationController {

    private final CollaborationService collaborationService;
    private final DocumentBlockService documentBlockService;
    private final RealTimeCollaborationService realTimeCollaborationService;
    private final UserService userService;
    private final CollaborationWebSocketHandler collaborationWebSocketHandler;

    @PostMapping("/projects")
    public ResponseEntity<ApiResponse<CollaborationProjectResponse>> createProject(
            @Valid @RequestBody ProjectCreateRequest request,
            HttpServletRequest httpRequest) {
        try {
            User currentUser = userService.getCurrentUser(httpRequest);
            CollaborationProjectResponse response = collaborationService.createProject(request, currentUser);
            return ResponseEntity.ok(ApiResponse.success("创建项目成功", response));
        } catch (Exception e) {
            log.error("创建项目失败", e);
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/projects")
    public ResponseEntity<ApiResponse<Page<CollaborationProjectResponse>>> getMyProjects(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            HttpServletRequest httpRequest) {
        try {
            User currentUser = userService.getCurrentUser(httpRequest);
            Pageable pageable = PageRequest.of(Math.max(page, 0), Math.max(size, 1));
            Page<CollaborationProjectResponse> result = collaborationService.getUserProjects(currentUser, pageable);
            return ResponseEntity.ok(ApiResponse.success(result));
        } catch (Exception e) {
            log.error("获取我的项目失败", e);
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/projects/search")
    public ResponseEntity<ApiResponse<Page<CollaborationProjectResponse>>> searchProjects(
            @RequestParam(defaultValue = "") String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        try {
            Pageable pageable = PageRequest.of(Math.max(page, 0), Math.max(size, 1));
            Page<CollaborationProjectResponse> result = collaborationService.searchProjects(keyword, pageable);
            return ResponseEntity.ok(ApiResponse.success(result));
        } catch (Exception e) {
            log.error("搜索项目失败", e);
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/projects/{projectId}")
    public ResponseEntity<ApiResponse<CollaborationProjectResponse>> getProjectById(@PathVariable Long projectId) {
        try {
            CollaborationProjectResponse result = collaborationService.getProjectById(projectId);
            return ResponseEntity.ok(ApiResponse.success(result));
        } catch (Exception e) {
            log.error("获取项目详情失败: projectId={}", projectId, e);
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @PutMapping("/projects/{projectId}")
    public ResponseEntity<ApiResponse<CollaborationProjectResponse>> updateProject(
            @PathVariable Long projectId,
            @Valid @RequestBody ProjectUpdateRequest request,
            HttpServletRequest httpRequest) {
        try {
            User currentUser = userService.getCurrentUser(httpRequest);
            CollaborationProjectResponse result = collaborationService.updateProject(projectId, request, currentUser);
            return ResponseEntity.ok(ApiResponse.success("更新项目成功", result));
        } catch (Exception e) {
            log.error("更新项目失败: projectId={}", projectId, e);
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @DeleteMapping("/projects/{projectId}")
    public ResponseEntity<ApiResponse<String>> deleteProject(
            @PathVariable Long projectId,
            HttpServletRequest httpRequest) {
        try {
            User currentUser = userService.getCurrentUser(httpRequest);
            collaborationService.deleteProject(projectId, currentUser);
            return ResponseEntity.ok(ApiResponse.success("删除项目成功"));
        } catch (Exception e) {
            log.error("删除项目失败: projectId={}", projectId, e);
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/projects/{projectId}/members")
    public ResponseEntity<ApiResponse<Page<ProjectMemberResponse>>> getProjectMembers(
            @PathVariable Long projectId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        try {
            Pageable pageable = PageRequest.of(Math.max(page, 0), Math.max(size, 1));
            Page<ProjectMemberResponse> result = collaborationService.getProjectMembers(projectId, pageable);
            return ResponseEntity.ok(ApiResponse.success(result));
        } catch (Exception e) {
            log.error("获取项目成员失败: projectId={}", projectId, e);
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/projects/{projectId}/members")
    public ResponseEntity<ApiResponse<ProjectMemberResponse>> addProjectMember(
            @PathVariable Long projectId,
            @Valid @RequestBody MemberAddRequest request,
            HttpServletRequest httpRequest) {
        try {
            User currentUser = userService.getCurrentUser(httpRequest);
            ProjectMemberResponse result = collaborationService.addMemberToProject(projectId, request, currentUser);
            return ResponseEntity.ok(ApiResponse.success("添加成员成功", result));
        } catch (Exception e) {
            log.error("添加成员失败: projectId={}", projectId, e);
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @PutMapping("/projects/{projectId}/members/{memberId}/role")
    public ResponseEntity<ApiResponse<ProjectMemberResponse>> updateMemberRole(
            @PathVariable Long projectId,
            @PathVariable Long memberId,
            @Valid @RequestBody MemberRoleUpdateRequest request,
            HttpServletRequest httpRequest) {
        try {
            User currentUser = userService.getCurrentUser(httpRequest);
            ProjectMemberResponse result = collaborationService.updateMemberRole(projectId, memberId, request, currentUser);
            return ResponseEntity.ok(ApiResponse.success("更新成员角色成功", result));
        } catch (Exception e) {
            log.error("更新成员角色失败: projectId={}, memberId={}", projectId, memberId, e);
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @DeleteMapping("/projects/{projectId}/members/{memberId}")
    public ResponseEntity<ApiResponse<String>> removeMember(
            @PathVariable Long projectId,
            @PathVariable Long memberId,
            HttpServletRequest httpRequest) {
        try {
            User currentUser = userService.getCurrentUser(httpRequest);
            collaborationService.removeMemberFromProject(projectId, memberId, currentUser);
            return ResponseEntity.ok(ApiResponse.success("移除成员成功"));
        } catch (Exception e) {
            log.error("移除成员失败: projectId={}, memberId={}", projectId, memberId, e);
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/projects/{projectId}/admins")
    public ResponseEntity<ApiResponse<List<ProjectMemberResponse>>> getProjectAdmins(@PathVariable Long projectId) {
        try {
            List<ProjectMemberResponse> result = collaborationService.getProjectAdmins(projectId);
            return ResponseEntity.ok(ApiResponse.success(result));
        } catch (Exception e) {
            log.error("获取项目管理员失败: projectId={}", projectId, e);
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/projects/{projectId}/statistics")
    public ResponseEntity<ApiResponse<ProjectStatisticsResponse>> getProjectStatistics(@PathVariable Long projectId) {
        try {
            ProjectStatisticsResponse result = collaborationService.getProjectStatistics(projectId);
            return ResponseEntity.ok(ApiResponse.success(result));
        } catch (Exception e) {
            log.error("获取项目统计失败: projectId={}", projectId, e);
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/documents")
    public ResponseEntity<ApiResponse<CollaborativeDocumentResponse>> createDocument(
            @Valid @RequestBody DocumentCreateRequest request,
            HttpServletRequest httpRequest) {
        try {
            User currentUser = userService.getCurrentUser(httpRequest);
            CollaborativeDocumentResponse result = collaborationService.createDocument(request, currentUser);
            return ResponseEntity.ok(ApiResponse.success("创建文档成功", result));
        } catch (Exception e) {
            log.error("创建文档失败", e);
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/documents/{documentId}")
    public ResponseEntity<ApiResponse<CollaborativeDocumentResponse>> getDocumentById(
            @PathVariable Long documentId,
            HttpServletRequest httpRequest) {
        try {
            User currentUser = userService.getCurrentUser(httpRequest);
            CollaborativeDocumentResponse result = collaborationService.getDocumentById(documentId);
            result = enrichDocumentPermissions(result, currentUser);
            return ResponseEntity.ok(ApiResponse.success(result));
        } catch (Exception e) {
            log.error("获取文档详情失败: documentId={}", documentId, e);
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @PutMapping("/documents/{documentId}")
    public ResponseEntity<ApiResponse<CollaborativeDocumentResponse>> updateDocument(
            @PathVariable Long documentId,
            @Valid @RequestBody DocumentUpdateRequest request,
            HttpServletRequest httpRequest) {
        try {
            User currentUser = userService.getCurrentUser(httpRequest);
            CollaborativeDocumentResponse result = collaborationService.updateDocument(documentId, request, currentUser);
            return ResponseEntity.ok(ApiResponse.success("更新文档成功", result));
        } catch (Exception e) {
            log.error("更新文档失败: documentId={}", documentId, e);
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @DeleteMapping("/documents/{documentId}")
    public ResponseEntity<ApiResponse<String>> deleteDocument(
            @PathVariable Long documentId,
            HttpServletRequest httpRequest) {
        try {
            User currentUser = userService.getCurrentUser(httpRequest);
            collaborationService.deleteDocument(documentId, currentUser);
            return ResponseEntity.ok(ApiResponse.success("删除文档成功"));
        } catch (Exception e) {
            log.error("删除文档失败: documentId={}", documentId, e);
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/projects/{projectId}/documents")
    public ResponseEntity<ApiResponse<Page<CollaborativeDocumentResponse>>> getProjectDocuments(
            @PathVariable Long projectId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            HttpServletRequest httpRequest) {
        try {
            User currentUser = userService.getCurrentUser(httpRequest);
            Pageable pageable = PageRequest.of(Math.max(page, 0), Math.max(size, 1));
            Page<CollaborativeDocumentResponse> result = collaborationService.getProjectDocuments(projectId, pageable)
                    .map(doc -> enrichDocumentPermissions(doc, currentUser));
            return ResponseEntity.ok(ApiResponse.success(result));
        } catch (Exception e) {
            log.error("获取项目文档失败: projectId={}", projectId, e);
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/documents/search")
    public ResponseEntity<ApiResponse<Page<CollaborativeDocumentResponse>>> searchDocuments(
            @RequestParam(defaultValue = "") String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            HttpServletRequest httpRequest) {
        try {
            User currentUser = userService.getCurrentUser(httpRequest);
            Pageable pageable = PageRequest.of(Math.max(page, 0), Math.max(size, 1));
            Page<CollaborativeDocumentResponse> result = collaborationService.searchDocuments(keyword, pageable)
                    .map(doc -> enrichDocumentPermissions(doc, currentUser));
            return ResponseEntity.ok(ApiResponse.success(result));
        } catch (Exception e) {
            log.error("搜索文档失败", e);
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/documents/{documentId}/lock")
    public ResponseEntity<ApiResponse<CollaborativeDocumentResponse>> lockDocument(
            @PathVariable Long documentId,
            HttpServletRequest httpRequest) {
        try {
            User currentUser = userService.getCurrentUser(httpRequest);
            CollaborativeDocumentResponse result = collaborationService.lockDocument(documentId, currentUser);
            return ResponseEntity.ok(ApiResponse.success("锁定文档成功", result));
        } catch (Exception e) {
            log.error("锁定文档失败: documentId={}", documentId, e);
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/documents/{documentId}/unlock")
    public ResponseEntity<ApiResponse<CollaborativeDocumentResponse>> unlockDocument(
            @PathVariable Long documentId,
            HttpServletRequest httpRequest) {
        try {
            User currentUser = userService.getCurrentUser(httpRequest);
            CollaborativeDocumentResponse result = collaborationService.unlockDocument(documentId, currentUser);
            return ResponseEntity.ok(ApiResponse.success("解锁文档成功", result));
        } catch (Exception e) {
            log.error("解锁文档失败: documentId={}", documentId, e);
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/documents/{documentId}/locked")
    public ResponseEntity<ApiResponse<Map<String, Object>>> isDocumentLocked(@PathVariable Long documentId) {
        try {
            boolean locked = collaborationService.isDocumentLocked(documentId);
            Map<String, Object> result = new HashMap<>();
            result.put("documentId", documentId);
            result.put("locked", locked);
            return ResponseEntity.ok(ApiResponse.success(result));
        } catch (Exception e) {
            log.error("查询文档锁状态失败: documentId={}", documentId, e);
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/documents/{documentId}/snapshots")
    public ResponseEntity<ApiResponse<Page<CollaborativeDocumentSnapshotResponse>>> getDocumentSnapshots(
            @PathVariable Long documentId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            HttpServletRequest httpRequest) {
        try {
            User currentUser = userService.getCurrentUser(httpRequest);
            Pageable pageable = PageRequest.of(Math.max(page, 0), Math.max(size, 1));
            Page<CollaborativeDocumentSnapshotResponse> result = collaborationService
                    .getDocumentSnapshots(documentId, pageable, currentUser);
            return ResponseEntity.ok(ApiResponse.success(result));
        } catch (Exception e) {
            log.error("获取文档快照失败: documentId={}", documentId, e);
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/documents/{documentId}/snapshots/{snapshotId}/restore")
    public ResponseEntity<ApiResponse<CollaborativeDocumentResponse>> restoreDocumentSnapshot(
            @PathVariable Long documentId,
            @PathVariable Long snapshotId,
            HttpServletRequest httpRequest) {
        try {
            User currentUser = userService.getCurrentUser(httpRequest);
            CollaborativeDocumentResponse result = collaborationService
                    .restoreDocumentSnapshot(documentId, snapshotId, currentUser);
            return ResponseEntity.ok(ApiResponse.success("恢复文档快照成功", result));
        } catch (Exception e) {
            log.error("恢复文档快照失败: documentId={}, snapshotId={}", documentId, snapshotId, e);
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/documents/{documentId}/comments")
    public ResponseEntity<ApiResponse<CommentResponse>> addComment(
            @PathVariable Long documentId,
            @Valid @RequestBody CommentCreateRequest request,
            HttpServletRequest httpRequest) {
        try {
            User currentUser = userService.getCurrentUser(httpRequest);
            CommentResponse result = collaborationService.addComment(documentId, request, currentUser);
            return ResponseEntity.ok(ApiResponse.success("添加评论成功", result));
        } catch (Exception e) {
            log.error("添加评论失败: documentId={}", documentId, e);
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @PutMapping("/comments/{commentId}")
    public ResponseEntity<ApiResponse<CommentResponse>> updateComment(
            @PathVariable Long commentId,
            @Valid @RequestBody CommentUpdateRequest request,
            HttpServletRequest httpRequest) {
        try {
            User currentUser = userService.getCurrentUser(httpRequest);
            CommentResponse result = collaborationService.updateComment(commentId, request, currentUser);
            return ResponseEntity.ok(ApiResponse.success("更新评论成功", result));
        } catch (Exception e) {
            log.error("更新评论失败: commentId={}", commentId, e);
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @DeleteMapping("/comments/{commentId}")
    public ResponseEntity<ApiResponse<String>> deleteComment(
            @PathVariable Long commentId,
            HttpServletRequest httpRequest) {
        try {
            User currentUser = userService.getCurrentUser(httpRequest);
            collaborationService.deleteComment(commentId, currentUser);
            return ResponseEntity.ok(ApiResponse.success("删除评论成功"));
        } catch (Exception e) {
            log.error("删除评论失败: commentId={}", commentId, e);
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/documents/{documentId}/comments")
    public ResponseEntity<ApiResponse<Page<CommentResponse>>> getDocumentComments(
            @PathVariable Long documentId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        try {
            Pageable pageable = PageRequest.of(Math.max(page, 0), Math.max(size, 1));
            Page<CommentResponse> result = collaborationService.getDocumentComments(documentId, pageable);
            return ResponseEntity.ok(ApiResponse.success(result));
        } catch (Exception e) {
            log.error("获取文档评论失败: documentId={}", documentId, e);
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/comments/my")
    public ResponseEntity<ApiResponse<Page<CommentResponse>>> getMyComments(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            HttpServletRequest httpRequest) {
        try {
            User currentUser = userService.getCurrentUser(httpRequest);
            Pageable pageable = PageRequest.of(Math.max(page, 0), Math.max(size, 1));
            Page<CommentResponse> result = collaborationService.getUserComments(currentUser, pageable);
            return ResponseEntity.ok(ApiResponse.success(result));
        } catch (Exception e) {
            log.error("获取我的评论失败", e);
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/users/me/statistics")
    public ResponseEntity<ApiResponse<UserCollaborationStatsResponse>> getMyStatistics(HttpServletRequest httpRequest) {
        try {
            User currentUser = userService.getCurrentUser(httpRequest);
            UserCollaborationStatsResponse result = collaborationService.getUserCollaborationStats(currentUser);
            return ResponseEntity.ok(ApiResponse.success(result));
        } catch (Exception e) {
            log.error("获取协作统计失败", e);
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/realtime/documents/{documentId}/session")
    public ResponseEntity<ApiResponse<RealTimeCollaborationService.CollaborationSession>> initializeRealtimeSession(
            @PathVariable Long documentId,
            HttpServletRequest httpRequest) {
        try {
            User currentUser = userService.getCurrentUser(httpRequest);
            RealTimeCollaborationService.CollaborationSession session =
                    realTimeCollaborationService.initializeCollaborationSession(documentId, currentUser);
            return ResponseEntity.ok(ApiResponse.success("初始化实时协作会话成功", session));
        } catch (Exception e) {
            log.error("初始化实时协作会话失败: documentId={}", documentId, e);
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/realtime/documents/{documentId}/join")
    public ResponseEntity<ApiResponse<Boolean>> joinRealtimeCollaboration(
            @PathVariable Long documentId,
            HttpServletRequest httpRequest) {
        try {
            User currentUser = userService.getCurrentUser(httpRequest);
            boolean result = realTimeCollaborationService.joinCollaboration(documentId, currentUser);
            return ResponseEntity.ok(ApiResponse.success("加入实时协作成功", result));
        } catch (Exception e) {
            log.error("加入实时协作失败: documentId={}", documentId, e);
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/realtime/documents/{documentId}/leave")
    public ResponseEntity<ApiResponse<Boolean>> leaveRealtimeCollaboration(
            @PathVariable Long documentId,
            HttpServletRequest httpRequest) {
        try {
            User currentUser = userService.getCurrentUser(httpRequest);
            boolean result = realTimeCollaborationService.leaveCollaboration(documentId, currentUser);
            return ResponseEntity.ok(ApiResponse.success("离开实时协作成功", result));
        } catch (Exception e) {
            log.error("离开实时协作失败: documentId={}", documentId, e);
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/realtime/documents/{documentId}/edit")
    public ResponseEntity<ApiResponse<RealTimeCollaborationService.EditOperationResult>> submitRealtimeEdit(
            @PathVariable Long documentId,
            @Valid @RequestBody RealtimeEditRequest request,
            HttpServletRequest httpRequest) {
        try {
            User currentUser = userService.getCurrentUser(httpRequest);
            RealTimeCollaborationService.EditOperation operation = new RealTimeCollaborationService.EditOperation();
            operation.setDocumentId(documentId);
            operation.setType(request.getType());
            operation.setContent(request.getContent());
            operation.setPosition(request.getPosition());
            operation.setLength(request.getLength());
            operation.setVersion(request.getVersion());
            operation.setTimestamp(request.getTimestamp() != null ? request.getTimestamp() : System.currentTimeMillis());
            operation.setUserId(String.valueOf(currentUser.getId()));

            RealTimeCollaborationService.EditOperationResult result =
                    realTimeCollaborationService.processEditOperation(operation, currentUser);

            if (result.isSuccess() && result.getNewState() != null) {
                collaborationWebSocketHandler.broadcastDocumentUpdated(
                        documentId,
                        result.getNewState(),
                        String.valueOf(currentUser.getId()),
                        currentUser.getUsername());
            }
            return ResponseEntity.ok(ApiResponse.success(result));
        } catch (Exception e) {
            log.error("提交实时编辑失败: documentId={}", documentId, e);
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/realtime/documents/{documentId}/sync")
    public ResponseEntity<ApiResponse<RealTimeCollaborationService.DocumentState>> syncRealtimeDocumentState(
            @PathVariable Long documentId,
            HttpServletRequest httpRequest) {
        try {
            User currentUser = userService.getCurrentUser(httpRequest);
            RealTimeCollaborationService.DocumentState state =
                    realTimeCollaborationService.syncDocumentState(documentId, currentUser);
            return ResponseEntity.ok(ApiResponse.success(state));
        } catch (Exception e) {
            log.error("同步实时文档状态失败: documentId={}", documentId, e);
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/realtime/documents/{documentId}/collaborators")
    public ResponseEntity<ApiResponse<List<RealTimeCollaborationService.CollaboratorInfo>>> getRealtimeCollaborators(
            @PathVariable Long documentId) {
        try {
            List<RealTimeCollaborationService.CollaboratorInfo> collaborators =
                    realTimeCollaborationService.getDocumentCollaborators(documentId);
            return ResponseEntity.ok(ApiResponse.success(collaborators));
        } catch (Exception e) {
            log.error("获取实时协作者失败: documentId={}", documentId, e);
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/realtime/documents/{documentId}/chat")
    public ResponseEntity<ApiResponse<Boolean>> sendRealtimeChatMessage(
            @PathVariable Long documentId,
            @Valid @RequestBody RealtimeChatRequest request,
            HttpServletRequest httpRequest) {
        try {
            User currentUser = userService.getCurrentUser(httpRequest);
            RealTimeCollaborationService.ChatMessage message = new RealTimeCollaborationService.ChatMessage();
            message.setDocumentId(documentId);
            message.setContent(request.getContent());
            message.setMessageType(request.getMessageType());

            boolean result = realTimeCollaborationService.sendChatMessage(message, currentUser);
            return ResponseEntity.ok(ApiResponse.success("发送实时聊天消息成功", result));
        } catch (Exception e) {
            log.error("发送实时聊天消息失败: documentId={}", documentId, e);
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/realtime/documents/{documentId}/blocks")
    public ResponseEntity<ApiResponse<List<DocumentBlockService.BlockView>>> getRealtimeBlocks(
            @PathVariable Long documentId,
            HttpServletRequest httpRequest) {
        try {
            User currentUser = userService.getCurrentUser(httpRequest);
            List<DocumentBlockService.BlockView> blocks = documentBlockService.getDocumentBlocks(documentId, currentUser);
            return ResponseEntity.ok(ApiResponse.success(blocks));
        } catch (Exception e) {
            log.error("获取文档块列表失败: documentId={}", documentId, e);
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/realtime/documents/{documentId}/blocks/init")
    public ResponseEntity<ApiResponse<List<DocumentBlockService.BlockView>>> initializeRealtimeBlocks(
            @PathVariable Long documentId,
            @RequestBody(required = false) RealtimeInitBlockRequest request,
            HttpServletRequest httpRequest) {
        try {
            User currentUser = userService.getCurrentUser(httpRequest);
            String initialContent = (request != null && request.getContent() != null) ? request.getContent() : "";
            // Ensure document has at least one block
            documentBlockService.ensureDocumentHasBlocks(documentId, initialContent);
            List<DocumentBlockService.BlockView> blocks = documentBlockService.getDocumentBlocks(documentId, currentUser);
            return ResponseEntity.ok(ApiResponse.success("初始化文档块成功", blocks));
        } catch (Exception e) {
            log.error("初始化文档块失败: documentId={}", documentId, e);
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/realtime/documents/{documentId}/blocks/{blockId}/lock")
    public ResponseEntity<ApiResponse<DocumentBlockService.BlockView>> lockRealtimeBlock(
            @PathVariable Long documentId,
            @PathVariable Long blockId,
            HttpServletRequest httpRequest) {
        try {
            User currentUser = userService.getCurrentUser(httpRequest);
            DocumentBlockService.BlockView block = documentBlockService.lockBlock(documentId, blockId, currentUser);
            collaborationWebSocketHandler.broadcastBlockLocked(documentId, block, currentUser);
            return ResponseEntity.ok(ApiResponse.success("锁定段落成功", block));
        } catch (Exception e) {
            log.error("锁定文档块失败: documentId={}, blockId={}", documentId, blockId, e);
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/realtime/documents/{documentId}/blocks/{blockId}/unlock")
    public ResponseEntity<ApiResponse<DocumentBlockService.BlockView>> unlockRealtimeBlock(
            @PathVariable Long documentId,
            @PathVariable Long blockId,
            HttpServletRequest httpRequest) {
        try {
            User currentUser = userService.getCurrentUser(httpRequest);
            DocumentBlockService.BlockView block = documentBlockService.unlockBlock(documentId, blockId, currentUser);
            collaborationWebSocketHandler.broadcastBlockUnlocked(documentId, block, currentUser);
            return ResponseEntity.ok(ApiResponse.success("解锁段落成功", block));
        } catch (Exception e) {
            log.error("解锁文档块失败: documentId={}, blockId={}", documentId, blockId, e);
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @PutMapping("/realtime/documents/{documentId}/blocks/{blockId}")
    public ResponseEntity<ApiResponse<DocumentBlockService.BlockView>> updateRealtimeBlock(
            @PathVariable Long documentId,
            @PathVariable Long blockId,
            @Valid @RequestBody RealtimeUpdateBlockRequest request,
            HttpServletRequest httpRequest) {
        try {
            User currentUser = userService.getCurrentUser(httpRequest);
            DocumentBlockService.BlockView block = documentBlockService.updateBlock(
                    documentId,
                    blockId,
                    request.getContent(),
                    request.getVersion(),
                    currentUser);
            collaborationWebSocketHandler.broadcastBlockUpdated(documentId, block, currentUser);
            return ResponseEntity.ok(ApiResponse.success("更新段落成功", block));
        } catch (Exception e) {
            log.error("更新文档块失败: documentId={}, blockId={}", documentId, blockId, e);
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/realtime/documents/{documentId}/blocks")
    public ResponseEntity<ApiResponse<DocumentBlockService.BlockView>> createRealtimeBlock(
            @PathVariable Long documentId,
            @Valid @RequestBody RealtimeCreateBlockRequest request,
            HttpServletRequest httpRequest) {
        try {
            User currentUser = userService.getCurrentUser(httpRequest);
            DocumentBlockService.BlockView block = documentBlockService.createBlockAfter(
                    documentId,
                    request.getAfterBlockId(),
                    currentUser);
            collaborationWebSocketHandler.broadcastBlockCreated(
                    documentId,
                    request.getAfterBlockId(),
                    block,
                    currentUser);
            return ResponseEntity.ok(ApiResponse.success("新增段落成功", block));
        } catch (Exception e) {
            log.error("新增文档块失败: documentId={}", documentId, e);
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/realtime/statistics")
    public ResponseEntity<ApiResponse<RealTimeCollaborationService.CollaborationStatistics>> getRealtimeStatistics() {
        try {
            RealTimeCollaborationService.CollaborationStatistics stats =
                    realTimeCollaborationService.getCollaborationStatistics();
            return ResponseEntity.ok(ApiResponse.success(stats));
        } catch (Exception e) {
            log.error("获取实时协作统计失败", e);
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    public static class RealtimeEditRequest {
        private String type;
        private String content;
        private Integer position;
        private Integer length;
        private Long version;
        private Long timestamp;

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public String getContent() {
            return content;
        }

        public void setContent(String content) {
            this.content = content;
        }

        public Integer getPosition() {
            return position;
        }

        public void setPosition(Integer position) {
            this.position = position;
        }

        public Integer getLength() {
            return length;
        }

        public void setLength(Integer length) {
            this.length = length;
        }

        public Long getVersion() {
            return version;
        }

        public void setVersion(Long version) {
            this.version = version;
        }

        public Long getTimestamp() {
            return timestamp;
        }

        public void setTimestamp(Long timestamp) {
            this.timestamp = timestamp;
        }
    }

    public static class RealtimeChatRequest {
        private String content;
        private String messageType;

        public String getContent() {
            return content;
        }

        public void setContent(String content) {
            this.content = content;
        }

        public String getMessageType() {
            return messageType;
        }

        public void setMessageType(String messageType) {
            this.messageType = messageType;
        }
    }

    public static class RealtimeUpdateBlockRequest {
        private String content;
        private Integer version;

        public String getContent() {
            return content;
        }

        public void setContent(String content) {
            this.content = content;
        }

        public Integer getVersion() {
            return version;
        }

        public void setVersion(Integer version) {
            this.version = version;
        }
    }

    public static class RealtimeCreateBlockRequest {
        private Long afterBlockId;

        public Long getAfterBlockId() {
            return afterBlockId;
        }

        public void setAfterBlockId(Long afterBlockId) {
            this.afterBlockId = afterBlockId;
        }
    }

    public static class RealtimeInitBlockRequest {
        private String content;

        public String getContent() {
            return content;
        }

        public void setContent(String content) {
            this.content = content;
        }
    }

    private CollaborativeDocumentResponse enrichDocumentPermissions(
            CollaborativeDocumentResponse document,
            User currentUser) {
        if (document == null || currentUser == null || document.getId() == null) {
            return document;
        }

        document.setCanEdit(collaborationService.canEditDocument(document.getId(), currentUser));
        document.setCanDelete(collaborationService.canDeleteDocument(document.getId(), currentUser));
        return document;
    }
}
