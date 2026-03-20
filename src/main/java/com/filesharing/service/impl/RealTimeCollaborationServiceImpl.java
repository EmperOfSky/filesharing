package com.filesharing.service.impl;

import com.filesharing.entity.User;
import com.filesharing.exception.BusinessException;
import com.filesharing.service.RealTimeCollaborationService;
import com.filesharing.websocket.DocumentLockManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 实时协作服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RealTimeCollaborationServiceImpl implements RealTimeCollaborationService {
    
    private final DocumentLockManager documentLockManager;
    
    // 存储文档状态
    private final Map<Long, DocumentState> documentStates = new ConcurrentHashMap<>();
    
    // 存储协作者信息
    private final Map<Long, Map<String, CollaboratorInfo>> documentCollaborators = new ConcurrentHashMap<>();
    
    // 存储编辑操作历史
    private final Map<Long, List<EditOperation>> editHistories = new ConcurrentHashMap<>();
    
    // 版本号生成器
    private final AtomicLong versionGenerator = new AtomicLong(System.currentTimeMillis());
    
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
            // 验证文档是否存在
            if (!documentStates.containsKey(documentId)) {
                throw new BusinessException("文档不存在");
            }
            
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
            Long documentId = operation.getDocumentId();
            String userId = user.getId().toString();
            
            // 验证文档状态
            DocumentState currentState = documentStates.get(documentId);
            if (currentState == null) {
                throw new BusinessException("文档状态不存在");
            }
            
            // 检查版本冲突
            if (operation.getVersion() != null && !operation.getVersion().equals(currentState.getVersion())) {
                result.setSuccess(false);
                result.setMessage("文档版本冲突，请先同步最新版本");
                return result;
            }
            
            // 应用编辑操作
            DocumentState newState = applyEditOperation(currentState, operation);
            
            // 更新文档状态
            documentStates.put(documentId, newState);
            
            // 记录编辑历史
            editHistories.computeIfAbsent(documentId, k -> new ArrayList<>()).add(operation);
            
            // 更新协作者的最后活动时间
            updateCollaboratorActivity(documentId, userId);
            
            result.setSuccess(true);
            result.setMessage("编辑操作成功");
            result.setNewState(newState);
            
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
            DocumentState state = documentStates.get(documentId);
            if (state == null) {
                throw new BusinessException("文档状态不存在");
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
        DocumentState state = new DocumentState();
        state.setDocumentId(documentId);
        state.setContent(""); // 初始为空内容
        state.setVersion(versionGenerator.incrementAndGet());
        state.setMetadata(new HashMap<>());
        state.setLastModified(System.currentTimeMillis());
        return state;
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
    
    private DocumentState applyEditOperation(DocumentState currentState, EditOperation operation) {
        DocumentState newState = new DocumentState();
        newState.setDocumentId(currentState.getDocumentId());
        newState.setVersion(versionGenerator.incrementAndGet());
        newState.setMetadata(new HashMap<>(currentState.getMetadata()));
        newState.setLastModified(System.currentTimeMillis());
        
        String currentContent = currentState.getContent() != null ? currentState.getContent() : "";
        StringBuilder newContent = new StringBuilder(currentContent);
        
        switch (operation.getType().toLowerCase()) {
            case "insert":
                if (operation.getPosition() != null && operation.getContent() != null) {
                    newContent.insert(operation.getPosition(), operation.getContent());
                }
                break;
                
            case "delete":
                if (operation.getPosition() != null && operation.getLength() != null) {
                    int start = Math.min(operation.getPosition(), newContent.length());
                    int end = Math.min(start + operation.getLength(), newContent.length());
                    newContent.delete(start, end);
                }
                break;
                
            case "update":
                if (operation.getPosition() != null && operation.getLength() != null && operation.getContent() != null) {
                    int start = Math.min(operation.getPosition(), newContent.length());
                    int end = Math.min(start + operation.getLength(), newContent.length());
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
}