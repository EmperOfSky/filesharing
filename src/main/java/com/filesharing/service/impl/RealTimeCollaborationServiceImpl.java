package com.filesharing.service.impl;

import com.filesharing.entity.CollaborativeDocument;
import com.filesharing.entity.ProjectMember;
import com.filesharing.entity.User;
import com.filesharing.exception.BusinessException;
import com.filesharing.repository.CollaborativeDocumentRepository;
import com.filesharing.repository.ProjectMemberRepository;
import com.filesharing.service.RealTimeCollaborationService;
import com.filesharing.websocket.DocumentLockManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 实时协作服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RealTimeCollaborationServiceImpl implements RealTimeCollaborationService {
    
    private final DocumentLockManager documentLockManager;
    private final CollaborativeDocumentRepository documentRepository;
    private final ProjectMemberRepository projectMemberRepository;
    
    // 存储文档状态
    private final Map<Long, DocumentState> documentStates = new ConcurrentHashMap<>();
    
    // 存储协作者信息
    private final Map<Long, Map<String, CollaboratorInfo>> documentCollaborators = new ConcurrentHashMap<>();
    
    // 存储编辑操作历史
    private final Map<Long, List<EditOperation>> editHistories = new ConcurrentHashMap<>();

    // 存储已应用操作（用于版本落后时进行patch重放）
    private final Map<Long, List<AppliedOperationRecord>> operationHistories = new ConcurrentHashMap<>();

    // 文档级操作锁，保证同一文档编辑串行化
    private final Map<Long, Object> documentOperationLocks = new ConcurrentHashMap<>();

    private static final int MAX_OPERATION_HISTORY = 500;
    
    @Override
    public CollaborationSession initializeCollaborationSession(Long documentId, User user) {
        try {
            // 初始化文档状态（如果不存在）
            documentStates.computeIfAbsent(documentId, this::createInitialDocumentState);
            
            // 添加协作者
            addCollaborator(documentId, user);
            
            // 创建协作会话
            CollaborationSession session = new CollaborationSession();
            session.setDocumentId(documentId);
            session.setSessionId(UUID.randomUUID().toString());
            session.setInitialState(documentStates.get(documentId));
            session.setCreatedAt(System.currentTimeMillis());
            session.setCollaborators(new ArrayList<>(documentCollaborators.get(documentId).values()));
            
            log.info("初始化协作会话: 文档ID={}, 用户ID={}", documentId, user.getId());
            return session;
            
        } catch (Exception e) {
            log.error("初始化协作会话失败: 文档ID={}, 用户ID={}", documentId, user.getId(), e);
            throw new BusinessException("初始化协作会话失败: " + e.getMessage());
        }
    }
    
    @Override
    public boolean joinCollaboration(Long documentId, User user) {
        try {
            // 如果会话尚未初始化，则按首次加入自动初始化。
            documentStates.computeIfAbsent(documentId, this::createInitialDocumentState);
            
            // 添加协作者
            addCollaborator(documentId, user);
            
            log.info("用户加入协作: 文档ID={}, 用户ID={}", documentId, user.getId());
            return true;
            
        } catch (Exception e) {
            log.error("加入协作失败: 文档ID={}, 用户ID={}", documentId, user.getId(), e);
            return false;
        }
    }
    
    @Override
    public boolean leaveCollaboration(Long documentId, User user) {
        try {
            // 移除协作者
            removeCollaborator(documentId, user.getId().toString());
            
            // 释放文档锁
            documentLockManager.releaseLock(documentId.toString(), user.getId().toString());
            
            log.info("用户离开协作: 文档ID={}, 用户ID={}", documentId, user.getId());
            return true;
            
        } catch (Exception e) {
            log.error("离开协作失败: 文档ID={}, 用户ID={}", documentId, user.getId(), e);
            return false;
        }
    }
    
    @Override
    public EditOperationResult processEditOperation(EditOperation operation, User user) {
        EditOperationResult result = new EditOperationResult();
        
        try {
            if (operation == null || operation.getDocumentId() == null || operation.getType() == null) {
                throw new BusinessException("编辑参数不完整");
            }

            Long documentId = operation.getDocumentId();
            String userId = user.getId().toString();

            if (!canUserEditDocument(documentId, user)) {
                throw new BusinessException("无权限编辑该文档");
            }

            synchronized (getDocumentOperationLock(documentId)) {
                // 验证文档状态
                DocumentState currentState = documentStates.get(documentId);
                if (currentState == null) {
                    currentState = createInitialDocumentState(documentId);
                    documentStates.put(documentId, currentState);
                } else {
                    // 有其他入口更新数据库时，优先以数据库最新状态为准。
                    DocumentState latestState = loadDocumentStateFromDatabase(documentId);
                    if (!Objects.equals(currentState.getVersion(), latestState.getVersion())
                            || !Objects.equals(currentState.getContent(), latestState.getContent())) {
                        currentState = latestState;
                        documentStates.put(documentId, latestState);
                    }
                }

                EditOperation normalizedOperation = normalizeOperation(operation);
                normalizedOperation.setDocumentId(documentId);
                normalizedOperation.setUserId(userId);
                normalizedOperation.setTimestamp(
                        operation.getTimestamp() != null ? operation.getTimestamp() : System.currentTimeMillis());

                long clientVersion = normalizedOperation.getVersion() != null
                        ? normalizedOperation.getVersion()
                        : (currentState.getVersion() == null ? 0L : currentState.getVersion());
                long serverVersion = currentState.getVersion() == null ? 0L : currentState.getVersion();

                if (clientVersion > serverVersion) {
                    result.setSuccess(false);
                    result.setMessage("客户端版本超前，请先同步最新版本");
                    return result;
                }

                List<ConflictResolution> autoResolvedConflicts = new ArrayList<>();

                if (clientVersion < serverVersion) {
                    List<AppliedOperationRecord> newerOperations = getOperationsAfterVersion(documentId, clientVersion);
                    if (newerOperations.isEmpty()) {
                        result.setSuccess(false);
                        result.setMessage("文档版本冲突，请先同步最新版本");
                        return result;
                    }

                    EditOperation rebasedOperation = rebaseOperation(normalizedOperation, newerOperations);
                    autoResolvedConflicts.add(buildAutoResolvedConflict(
                            normalizedOperation,
                            rebasedOperation,
                            clientVersion,
                            serverVersion));
                    normalizedOperation = rebasedOperation;
                }

                if (isNoOpOperation(normalizedOperation)) {
                    updateCollaboratorActivity(documentId, userId);
                    result.setSuccess(true);
                    result.setMessage("编辑操作已自动合并，无需额外变更");
                    result.setNewState(currentState);
                    if (!autoResolvedConflicts.isEmpty()) {
                        result.setConflicts(autoResolvedConflicts);
                    }
                    return result;
                }

                // 应用编辑操作
                DocumentState newState = applyEditOperation(currentState, normalizedOperation);

                // 更新文档状态
                documentStates.put(documentId, newState);

                // 持久化实时编辑结果，避免刷新后内容丢失
                persistStateToDocument(documentId, newState, user);

                // 记录编辑历史
                editHistories.computeIfAbsent(documentId, k -> new CopyOnWriteArrayList<>())
                        .add(copyOperation(normalizedOperation));
                recordAppliedOperation(documentId, normalizedOperation, serverVersion, newState.getVersion());

                // 更新协作者的最后活动时间
                updateCollaboratorActivity(documentId, userId);

                result.setSuccess(true);
                result.setMessage(autoResolvedConflicts.isEmpty()
                        ? "编辑操作成功"
                        : "编辑操作成功，已执行patch级自动合并");
                result.setNewState(newState);
                if (!autoResolvedConflicts.isEmpty()) {
                    result.setConflicts(autoResolvedConflicts);
                }
            }
            
            log.debug("处理编辑操作: 文档ID={}, 用户ID={}, 操作类型={}", 
                documentId, userId, operation.getType());
                
        } catch (Exception e) {
            log.error("处理编辑操作失败", e);
            result.setSuccess(false);
            result.setMessage("编辑操作失败: " + e.getMessage());
        }
        
        return result;
    }
    
    @Override
    public DocumentState syncDocumentState(Long documentId, User user) {
        try {
            DocumentState cachedState = documentStates.get(documentId);
            DocumentState latestState = loadDocumentStateFromDatabase(documentId);

            DocumentState state;
            if (cachedState == null
                    || !Objects.equals(cachedState.getVersion(), latestState.getVersion())
                    || !Objects.equals(cachedState.getContent(), latestState.getContent())) {
                documentStates.put(documentId, latestState);
                state = latestState;
            } else {
                state = cachedState;
            }
            
            // 更新协作者活动状态
            updateCollaboratorActivity(documentId, user.getId().toString());
            
            log.debug("同步文档状态: 文档ID={}, 用户ID={}", documentId, user.getId());
            return state;
            
        } catch (Exception e) {
            log.error("同步文档状态失败: 文档ID={}, 用户ID={}", documentId, user.getId(), e);
            throw new BusinessException("同步文档状态失败: " + e.getMessage());
        }
    }
    
    @Override
    public List<CollaboratorInfo> getDocumentCollaborators(Long documentId) {
        try {
            Map<String, CollaboratorInfo> collaborators = documentCollaborators.get(documentId);
            if (collaborators == null) {
                return new ArrayList<>();
            }
            
            // 过滤掉长时间未活动的用户
            long currentTime = System.currentTimeMillis();
            long inactiveThreshold = 300000; // 5分钟
            
            return collaborators.values().stream()
                .filter(collaborator -> currentTime - collaborator.getLastActiveTime() < inactiveThreshold)
                .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
                
        } catch (Exception e) {
            log.error("获取文档协作者失败: 文档ID={}", documentId, e);
            return new ArrayList<>();
        }
    }
    
    @Override
    public boolean sendChatMessage(ChatMessage message, User sender) {
        try {
            // 验证消息内容
            if (message.getContent() == null || message.getContent().trim().isEmpty()) {
                throw new BusinessException("消息内容不能为空");
            }
            
            // 设置消息属性
            message.setUserId(sender.getId().toString());
            message.setUsername(sender.getUsername());
            message.setTimestamp(System.currentTimeMillis());
            message.setMessageType("text");
            
            // 这里应该通过WebSocket广播消息
            // websocketService.broadcastToDocument(message.getDocumentId(), message);
            
            log.debug("发送聊天消息: 文档ID={}, 用户ID={}, 内容={}", 
                message.getDocumentId(), sender.getId(), message.getContent());
                
            return true;
            
        } catch (Exception e) {
            log.error("发送聊天消息失败", e);
            return false;
        }
    }
    
    @Override
    public CollaborationStatistics getCollaborationStatistics() {
        CollaborationStatistics stats = new CollaborationStatistics();
        
        try {
            stats.setActiveSessions(documentCollaborators.size());
            
            int totalCollaborators = documentCollaborators.values().stream()
                .mapToInt(Map::size)
                .sum();
            stats.setTotalCollaborators(totalCollaborators);
            
            int documentsBeingEdited = (int) documentStates.keySet().stream()
                .filter(docId -> documentLockManager.isLocked(docId.toString()))
                .count();
            stats.setDocumentsBeingEdited(documentsBeingEdited);
            
            // 计算平均协作者数
            if (!documentCollaborators.isEmpty()) {
                double avgCollaborators = (double) totalCollaborators / documentCollaborators.size();
                stats.setAverageCollaboratorsPerDocument(avgCollaborators);
            }
            
            // 统计编辑操作类型
            Map<String, Integer> operationStats = new HashMap<>();
            editHistories.values().forEach(history -> 
                history.forEach(op -> 
                    operationStats.merge(op.getType(), 1, Integer::sum)
                )
            );
            stats.setEditOperationStats(operationStats);
            
            log.debug("获取协作统计信息: 活跃会话={}, 总协作者={}, 编辑中文档={}", 
                stats.getActiveSessions(), stats.getTotalCollaborators(), stats.getDocumentsBeingEdited());
                
        } catch (Exception e) {
            log.error("获取协作统计信息失败", e);
        }
        
        return stats;
    }
    
    // ==================== 私有辅助方法 ====================
    
    private DocumentState createInitialDocumentState(Long documentId) {
        return loadDocumentStateFromDatabase(documentId);
    }

    private DocumentState loadDocumentStateFromDatabase(Long documentId) {
        CollaborativeDocument document = documentRepository.findById(documentId)
                .orElseThrow(() -> new BusinessException("文档不存在"));

        DocumentState state = new DocumentState();
        state.setDocumentId(documentId);
        state.setContent(document.getContent() == null ? "" : document.getContent());
        long currentVersion = document.getVersion() == null ? 1L : document.getVersion().longValue();
        state.setVersion(currentVersion);
        state.setMetadata(new HashMap<>());
        state.setLastModified(System.currentTimeMillis());
        return state;
    }

    private void persistStateToDocument(Long documentId, DocumentState state, User user) {
        CollaborativeDocument document = documentRepository.findById(documentId)
                .orElseThrow(() -> new BusinessException("文档不存在"));

        document.setContent(state.getContent());
        document.setLastEditedBy(user);
        document.setLastEditedAt(LocalDateTime.now());
        long nextVersion = state.getVersion() == null
                ? ((document.getVersion() == null ? 0L : document.getVersion().longValue()) + 1L)
                : state.getVersion();
        if (nextVersion > Integer.MAX_VALUE) {
            throw new BusinessException("文档版本号超过上限");
        }
        document.setVersion((int) nextVersion);
        documentRepository.save(document);
    }

    private boolean canUserEditDocument(Long documentId, User user) {
        CollaborativeDocument document = documentRepository.findById(documentId)
                .orElseThrow(() -> new BusinessException("文档不存在"));

        if (user == null || document.getProject() == null) {
            return false;
        }

        User owner = document.getProject().getOwner();
        if (owner != null && Objects.equals(owner.getId(), user.getId())) {
            return true;
        }

        boolean hasProjectEditRole = projectMemberRepository.findByProjectAndUser(document.getProject(), user)
            .filter(member -> member.getStatus() == ProjectMember.MemberStatus.ACTIVE
                && member.getInviteStatus() == ProjectMember.InviteStatus.ACCEPTED)
            .map(member -> member.getRole() == ProjectMember.MemberRole.ADMIN
                || member.getRole() == ProjectMember.MemberRole.MEMBER)
            .orElse(false);

        if (!hasProjectEditRole) {
            return false;
        }

        if (!Boolean.TRUE.equals(document.getIsLocked())) {
            return true;
        }

        return document.getLockedBy() != null
            && Objects.equals(document.getLockedBy().getId(), user.getId());
    }
    
    private void addCollaborator(Long documentId, User user) {
        CollaboratorInfo collaborator = new CollaboratorInfo();
        collaborator.setUserId(user.getId().toString());
        collaborator.setUsername(user.getUsername());
        collaborator.setAvatar(user.getAvatar());
        collaborator.setCursorPosition("0");
        collaborator.setLastActiveTime(System.currentTimeMillis());
        collaborator.setStatus("online");
        
        documentCollaborators.computeIfAbsent(documentId, k -> new ConcurrentHashMap<>())
            .put(user.getId().toString(), collaborator);
            
        documentLockManager.addCollaborator(documentId.toString(), user.getId().toString());
    }
    
    private void removeCollaborator(Long documentId, String userId) {
        Map<String, CollaboratorInfo> collaborators = documentCollaborators.get(documentId);
        if (collaborators != null) {
            collaborators.remove(userId);
            if (collaborators.isEmpty()) {
                documentCollaborators.remove(documentId);
            }
        }
        
        documentLockManager.removeCollaborator(documentId.toString(), userId);
    }

    private Object getDocumentOperationLock(Long documentId) {
        return documentOperationLocks.computeIfAbsent(documentId, key -> new Object());
    }

    private EditOperation normalizeOperation(EditOperation operation) {
        EditOperation normalized = copyOperation(operation);
        normalized.setType(normalizeOperationType(operation.getType()));
        normalized.setPosition(Math.max(operation.getPosition() == null ? 0 : operation.getPosition(), 0));
        normalized.setLength(Math.max(operation.getLength() == null ? 0 : operation.getLength(), 0));
        normalized.setContent(operation.getContent() == null ? "" : operation.getContent());
        return normalized;
    }

    private String normalizeOperationType(String type) {
        String normalizedType = type == null ? "" : type.trim().toLowerCase(Locale.ROOT);
        if (!"insert".equals(normalizedType) && !"delete".equals(normalizedType) && !"update".equals(normalizedType)) {
            throw new BusinessException("不支持的编辑操作类型: " + type);
        }
        return normalizedType;
    }

    private EditOperation copyOperation(EditOperation source) {
        EditOperation copy = new EditOperation();
        copy.setType(source.getType());
        copy.setContent(source.getContent());
        copy.setPosition(source.getPosition());
        copy.setLength(source.getLength());
        copy.setVersion(source.getVersion());
        copy.setTimestamp(source.getTimestamp());
        copy.setUserId(source.getUserId());
        copy.setDocumentId(source.getDocumentId());
        return copy;
    }

    private List<AppliedOperationRecord> getOperationsAfterVersion(Long documentId, long version) {
        List<AppliedOperationRecord> history = operationHistories.get(documentId);
        if (history == null || history.isEmpty()) {
            return Collections.emptyList();
        }

        List<AppliedOperationRecord> result = new ArrayList<>();
        for (AppliedOperationRecord record : history) {
            if (record.getAppliedVersion() != null && record.getAppliedVersion() > version) {
                result.add(record);
            }
        }
        result.sort(Comparator.comparingLong(record -> record.getAppliedVersion() == null ? 0L : record.getAppliedVersion()));
        return result;
    }

    private EditOperation rebaseOperation(EditOperation operation, List<AppliedOperationRecord> newerOperations) {
        EditOperation rebased = copyOperation(operation);
        for (AppliedOperationRecord appliedOperation : newerOperations) {
            rebased = transformOperationByAppliedOperation(rebased, appliedOperation);
        }
        return normalizeOperation(rebased);
    }

    private EditOperation transformOperationByAppliedOperation(EditOperation incoming, AppliedOperationRecord applied) {
        EditOperation transformed = copyOperation(incoming);

        String incomingType = normalizeOperationType(transformed.getType());
        int position = Math.max(transformed.getPosition() == null ? 0 : transformed.getPosition(), 0);
        int length = Math.max(transformed.getLength() == null ? 0 : transformed.getLength(), 0);

        if ("insert".equals(incomingType)) {
            transformed.setPosition(transformIndex(position, applied, true));
            return transformed;
        }

        int start = position;
        int end = position + length;
        int transformedStart = transformIndex(start, applied, false);
        int transformedEnd = transformIndex(end, applied, true);
        if (transformedEnd < transformedStart) {
            transformedEnd = transformedStart;
        }

        transformed.setPosition(transformedStart);
        transformed.setLength(transformedEnd - transformedStart);
        return transformed;
    }

    private int transformIndex(int index, AppliedOperationRecord applied, boolean stickAfterInsert) {
        int transformed = Math.max(index, 0);
        int operationPosition = Math.max(applied.getPosition() == null ? 0 : applied.getPosition(), 0);
        int deletedLength = getDeletedLength(applied);
        int insertedLength = getInsertedLength(applied);

        if (deletedLength > 0) {
            int deleteEnd = operationPosition + deletedLength;
            if (transformed > deleteEnd) {
                transformed -= deletedLength;
            } else if (transformed > operationPosition) {
                transformed = operationPosition;
            }
        }

        if (insertedLength > 0
                && (transformed > operationPosition || (stickAfterInsert && transformed == operationPosition))) {
            transformed += insertedLength;
        }

        return Math.max(transformed, 0);
    }

    private int getDeletedLength(AppliedOperationRecord record) {
        String type = normalizeOperationType(record.getType());
        if ("delete".equals(type) || "update".equals(type)) {
            return Math.max(record.getLength() == null ? 0 : record.getLength(), 0);
        }
        return 0;
    }

    private int getInsertedLength(AppliedOperationRecord record) {
        String type = normalizeOperationType(record.getType());
        if ("insert".equals(type) || "update".equals(type)) {
            return record.getContent() == null ? 0 : record.getContent().length();
        }
        return 0;
    }

    private boolean isNoOpOperation(EditOperation operation) {
        String type = normalizeOperationType(operation.getType());
        String content = operation.getContent() == null ? "" : operation.getContent();
        int length = Math.max(operation.getLength() == null ? 0 : operation.getLength(), 0);

        if ("insert".equals(type)) {
            return content.isEmpty();
        }
        if ("delete".equals(type)) {
            return length == 0;
        }
        return length == 0 && content.isEmpty();
    }

    private void recordAppliedOperation(
            Long documentId,
            EditOperation appliedOperation,
            Long baseVersion,
            Long appliedVersion) {
        AppliedOperationRecord record = new AppliedOperationRecord();
        record.setType(normalizeOperationType(appliedOperation.getType()));
        record.setPosition(Math.max(appliedOperation.getPosition() == null ? 0 : appliedOperation.getPosition(), 0));
        record.setLength(Math.max(appliedOperation.getLength() == null ? 0 : appliedOperation.getLength(), 0));
        record.setContent(appliedOperation.getContent() == null ? "" : appliedOperation.getContent());
        record.setBaseVersion(baseVersion);
        record.setAppliedVersion(appliedVersion);

        List<AppliedOperationRecord> history = operationHistories.computeIfAbsent(
                documentId,
                key -> new CopyOnWriteArrayList<>());
        history.add(record);

        int overflow = history.size() - MAX_OPERATION_HISTORY;
        for (int i = 0; i < overflow; i++) {
            history.remove(0);
        }
    }

    private ConflictResolution buildAutoResolvedConflict(
            EditOperation originalOperation,
            EditOperation rebasedOperation,
            long clientVersion,
            long serverVersion) {
        ConflictResolution conflict = new ConflictResolution();
        conflict.setType("auto-resolved");
        conflict.setDescription("检测到版本差异，已自动重放补丁并合并到最新文档");
        conflict.setResolution(String.format(
                Locale.ROOT,
                "客户端版本 %d -> 服务端版本 %d，位置 %d -> %d，长度 %d -> %d",
                clientVersion,
                serverVersion,
                Math.max(originalOperation.getPosition() == null ? 0 : originalOperation.getPosition(), 0),
                Math.max(rebasedOperation.getPosition() == null ? 0 : rebasedOperation.getPosition(), 0),
                Math.max(originalOperation.getLength() == null ? 0 : originalOperation.getLength(), 0),
                Math.max(rebasedOperation.getLength() == null ? 0 : rebasedOperation.getLength(), 0)));
        return conflict;
    }
    
    private DocumentState applyEditOperation(DocumentState currentState, EditOperation operation) {
        DocumentState newState = new DocumentState();
        newState.setDocumentId(currentState.getDocumentId());
        long nextVersion = (currentState.getVersion() == null ? 0L : currentState.getVersion()) + 1L;
        newState.setVersion(nextVersion);
        newState.setMetadata(new HashMap<>(currentState.getMetadata()));
        newState.setLastModified(System.currentTimeMillis());
        
        String currentContent = currentState.getContent() != null ? currentState.getContent() : "";
        StringBuilder newContent = new StringBuilder(currentContent);
        int safePosition = Math.max(0, Math.min(operation.getPosition() != null ? operation.getPosition() : 0, newContent.length()));
        
        switch (operation.getType().toLowerCase()) {
            case "insert":
                if (operation.getContent() != null) {
                    newContent.insert(safePosition, operation.getContent());
                }
                break;
                
            case "delete":
                if (operation.getPosition() != null && operation.getLength() != null) {
                    int start = safePosition;
                    int end = Math.max(start, Math.min(start + Math.max(operation.getLength(), 0), newContent.length()));
                    newContent.delete(start, end);
                }
                break;
                
            case "update":
                if (operation.getPosition() != null && operation.getLength() != null && operation.getContent() != null) {
                    int start = safePosition;
                    int end = Math.max(start, Math.min(start + Math.max(operation.getLength(), 0), newContent.length()));
                    newContent.replace(start, end, operation.getContent());
                }
                break;
                
            default:
                throw new BusinessException("不支持的编辑操作类型: " + operation.getType());
        }
        
        newState.setContent(newContent.toString());
        return newState;
    }
    
    private void updateCollaboratorActivity(Long documentId, String userId) {
        Map<String, CollaboratorInfo> collaborators = documentCollaborators.get(documentId);
        if (collaborators != null) {
            CollaboratorInfo collaborator = collaborators.get(userId);
            if (collaborator != null) {
                collaborator.setLastActiveTime(System.currentTimeMillis());
                collaborator.setStatus("online");
            }
        }
    }

    private static class AppliedOperationRecord {
        private String type;
        private Integer position;
        private Integer length;
        private String content;
        private Long baseVersion;
        private Long appliedVersion;

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
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

        public String getContent() {
            return content;
        }

        public void setContent(String content) {
            this.content = content;
        }

        @SuppressWarnings("unused")
        public Long getBaseVersion() {
            return baseVersion;
        }

        public void setBaseVersion(Long baseVersion) {
            this.baseVersion = baseVersion;
        }

        public Long getAppliedVersion() {
            return appliedVersion;
        }

        public void setAppliedVersion(Long appliedVersion) {
            this.appliedVersion = appliedVersion;
        }
    }
}