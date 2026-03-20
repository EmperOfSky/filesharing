package com.filesharing.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.filesharing.service.DocumentBlockService;
import com.filesharing.service.RealTimeCollaborationService;
import com.filesharing.entity.User;
import com.filesharing.exception.BusinessException;
import com.filesharing.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * WebSocket处理器 - 处理实时协作连接
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CollaborationWebSocketHandler extends TextWebSocketHandler {
    
    private final UserService userService;
    private final DocumentBlockService documentBlockService;
    private final RealTimeCollaborationService realTimeCollaborationService;
    private final ObjectMapper objectMapper;
    
    // 存储活跃的WebSocket会话
    private final Map<String, WebSocketSession> activeSessions = new ConcurrentHashMap<>();
    // 存储用户和文档的映射关系
    private final Map<String, String> userDocumentMapping = new ConcurrentHashMap<>();
    
    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String sessionId = session.getId();
        String userId = getUserIdFromSession(session);
        
        if (userId != null) {
            activeSessions.put(sessionId, session);
            log.info("WebSocket连接建立: 会话ID={}, 用户ID={}", sessionId, userId);
            
            // 发送连接成功的消息
            sendMessage(session, createSuccessMessage("连接成功", "connected"));
        } else {
            log.warn("WebSocket连接建立失败: 无法识别用户身份");
            session.close(CloseStatus.NOT_ACCEPTABLE.withReason("用户身份验证失败"));
        }
    }
    
    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        try {
            String payload = message.getPayload();
            CollaborationMessage collabMessage = objectMapper.readValue(payload, CollaborationMessage.class);
            
            String sessionId = session.getId();
            String userId = getUserIdFromSession(session);
            
            log.debug("收到协作消息: 会话ID={}, 用户ID={}, 消息类型={}", 
                sessionId, userId, collabMessage.getType());
            
            switch (collabMessage.getType()) {
                case JOIN_DOCUMENT:
                    handleJoinDocument(session, collabMessage);
                    break;
                case LEAVE_DOCUMENT:
                    handleLeaveDocument(session, collabMessage);
                    break;
                case EDIT_OPERATION:
                    handleEditOperation(session, collabMessage);
                    break;
                case CURSOR_POSITION:
                    handleCursorPosition(session, collabMessage);
                    break;
                case CHAT_MESSAGE:
                    handleChatMessage(session, collabMessage);
                    break;
                case REQUEST_SYNC:
                    handleSyncRequest(session, collabMessage);
                    break;
                case LOCK_BLOCK:
                    handleLockBlock(session, collabMessage);
                    break;
                case UNLOCK_BLOCK:
                    handleUnlockBlock(session, collabMessage);
                    break;
                case UPDATE_BLOCK:
                    handleUpdateBlock(session, collabMessage);
                    break;
                case CREATE_BLOCK:
                    handleCreateBlock(session, collabMessage);
                    break;
                default:
                    log.warn("未知的消息类型: {}", collabMessage.getType());
                    sendMessage(session, createErrorMessage("未知的消息类型"));
            }
            
        } catch (Exception e) {
            log.error("处理WebSocket消息失败", e);
            sendMessage(session, createErrorMessage("消息处理失败: " + e.getMessage()));
        }
    }
    
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        String sessionId = session.getId();
        String userId = getUserIdFromSession(session);
        
        // 清理用户文档映射
        String documentId = userDocumentMapping.remove(userId);
        if (documentId != null) {
            try {
                documentBlockService.unlockBlocksByUser(parseDocumentId(documentId), parseUserId(userId));
            } catch (Exception ex) {
                log.warn("连接断开时释放块锁失败: 用户ID={}, 文档ID={}", userId, documentId, ex);
            }
            broadcastToDocument(documentId, createLeaveMessage(userId), session);
        }
        
        // 移除会话
        activeSessions.remove(sessionId);
        
        log.info("WebSocket连接关闭: 会话ID={}, 用户ID={}, 状态={}", sessionId, userId, status);
    }
    
    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        log.error("WebSocket传输错误: 会话ID={}", session.getId(), exception);
        super.handleTransportError(session, exception);
    }
    
    // ==================== 消息处理方法 ====================
    
    private void handleJoinDocument(WebSocketSession session, CollaborationMessage message) {
        try {
            String userId = getUserIdFromSession(session);
            String documentId = message.getDocumentId();
            User currentUser = requireCurrentUser(session);
            
            if (documentId == null || documentId.isEmpty()) {
                sendMessage(session, createErrorMessage("文档ID不能为空"));
                return;
            }

            Long parsedDocumentId = parseDocumentId(documentId);
            realTimeCollaborationService.joinCollaboration(parsedDocumentId, currentUser);
            
            // 更新用户文档映射
            String previousDocument = userDocumentMapping.put(userId, documentId);
            if (previousDocument != null && !previousDocument.equals(documentId)) {
                // 如果用户之前在其他文档中，先离开那个文档
                broadcastToDocument(previousDocument, createLeaveMessage(userId), session);
            }
            
            // 广播用户加入消息
            String displayName = message.getUserName() != null && !message.getUserName().isEmpty()
                    ? message.getUserName()
                    : currentUser.getUsername();
            broadcastToDocument(documentId, createJoinMessage(userId, displayName), session);
            
            // 发送文档当前真实状态
            RealTimeCollaborationService.DocumentState state =
                    realTimeCollaborationService.syncDocumentState(parsedDocumentId, currentUser);
            sendMessage(session, createDocumentStateMessage(parsedDocumentId, state));
            
            log.info("用户加入文档协作: 用户ID={}, 文档ID={}", userId, documentId);
            
        } catch (Exception e) {
            log.error("处理加入文档请求失败", e);
            sendMessage(session, createErrorMessage("加入文档失败: " + e.getMessage()));
        }
    }
    
    private void handleLeaveDocument(WebSocketSession session, CollaborationMessage message) {
        try {
            String userId = getUserIdFromSession(session);
            String documentId = userDocumentMapping.remove(userId);
            User currentUser = requireCurrentUser(session);
            
            if (documentId != null) {
                realTimeCollaborationService.leaveCollaboration(parseDocumentId(documentId), currentUser);
                documentBlockService.unlockBlocksByUser(parseDocumentId(documentId), currentUser.getId());
                broadcastToDocument(documentId, createLeaveMessage(userId), session);
                log.info("用户离开文档协作: 用户ID={}, 文档ID={}", userId, documentId);
            }
            
        } catch (Exception e) {
            log.error("处理离开文档请求失败", e);
        }
    }
    
    private void handleEditOperation(WebSocketSession session, CollaborationMessage message) {
        try {
            String userId = getUserIdFromSession(session);
            String documentId = userDocumentMapping.get(userId);
            User currentUser = requireCurrentUser(session);
            
            if (documentId == null) {
                sendMessage(session, createErrorMessage("用户未加入任何文档"));
                return;
            }
            
            // 以实时服务中的最新状态广播，避免客户端再额外拉取一次。
            Long parsedDocumentId = parseDocumentId(documentId);
            RealTimeCollaborationService.DocumentState state =
                    realTimeCollaborationService.syncDocumentState(parsedDocumentId, currentUser);
            CollaborationMessage forwardMessage = createDocumentUpdatedMessage(
                    parsedDocumentId,
                    state,
                    userId,
                    message.getUserName() != null ? message.getUserName() : currentUser.getUsername());
            broadcastToDocument(documentId, forwardMessage, session);
            
            log.debug("转发编辑操作: 用户ID={}, 文档ID={}, 操作={}", 
                userId, documentId, message.getOperation());
                
        } catch (Exception e) {
            log.error("处理编辑操作失败", e);
        }
    }
    
    private void handleCursorPosition(WebSocketSession session, CollaborationMessage message) {
        try {
            String userId = getUserIdFromSession(session);
            String documentId = userDocumentMapping.get(userId);
            
            if (documentId != null) {
                CollaborationMessage cursorMessage = createCursorPositionMessage(
                    userId, message.getUserName(), message.getPosition());
                broadcastToDocument(documentId, cursorMessage, session);
            }
            
        } catch (Exception e) {
            log.error("处理光标位置更新失败", e);
        }
    }
    
    private void handleChatMessage(WebSocketSession session, CollaborationMessage message) {
        try {
            String userId = getUserIdFromSession(session);
            String documentId = userDocumentMapping.get(userId);
            
            if (documentId == null) {
                sendMessage(session, createErrorMessage("用户未加入任何文档"));
                return;
            }
            
            if (message.getContent() == null || message.getContent().trim().isEmpty()) {
                sendMessage(session, createErrorMessage("聊天消息不能为空"));
                return;
            }
            
            CollaborationMessage chatMessage = createChatMessage(
                userId, message.getUserName(), message.getContent());
            broadcastToDocument(documentId, chatMessage, null); // 广播给所有人包括发送者
            
            log.debug("转发聊天消息: 用户ID={}, 文档ID={}, 内容={}", 
                userId, documentId, message.getContent());
                
        } catch (Exception e) {
            log.error("处理聊天消息失败", e);
        }
    }
    
    private void handleSyncRequest(WebSocketSession session, CollaborationMessage message) {
        try {
            String userId = getUserIdFromSession(session);
            String documentId = userDocumentMapping.get(userId);
            User currentUser = requireCurrentUser(session);
            
            if (documentId != null) {
                Long parsedDocumentId = parseDocumentId(documentId);
                RealTimeCollaborationService.DocumentState state =
                        realTimeCollaborationService.syncDocumentState(parsedDocumentId, currentUser);
                sendMessage(session, createDocumentStateMessage(parsedDocumentId, state));
            }
            
        } catch (Exception e) {
            log.error("处理同步请求失败", e);
        }
    }

    private void handleLockBlock(WebSocketSession session, CollaborationMessage message) {
        try {
            String userId = getUserIdFromSession(session);
            String documentId = userDocumentMapping.get(userId);
            User currentUser = requireCurrentUser(session);

            if (documentId == null) {
                sendMessage(session, createErrorMessage("用户未加入任何文档"));
                return;
            }
            if (message.getBlockId() == null) {
                sendMessage(session, createErrorMessage("缺少 blockId"));
                return;
            }

            Long parsedDocumentId = parseDocumentId(documentId);
            DocumentBlockService.BlockView block = documentBlockService.lockBlock(parsedDocumentId, message.getBlockId(), currentUser);
            broadcastToDocument(documentId, createBlockMessage(MessageType.BLOCK_LOCKED, block, currentUser, null), null);
        } catch (Exception e) {
            log.error("处理 LOCK_BLOCK 失败", e);
            sendMessage(session, createErrorMessage("锁定段落失败: " + e.getMessage()));
        }
    }

    private void handleUnlockBlock(WebSocketSession session, CollaborationMessage message) {
        try {
            String userId = getUserIdFromSession(session);
            String documentId = userDocumentMapping.get(userId);
            User currentUser = requireCurrentUser(session);

            if (documentId == null) {
                sendMessage(session, createErrorMessage("用户未加入任何文档"));
                return;
            }
            if (message.getBlockId() == null) {
                sendMessage(session, createErrorMessage("缺少 blockId"));
                return;
            }

            Long parsedDocumentId = parseDocumentId(documentId);
            DocumentBlockService.BlockView block = documentBlockService.unlockBlock(parsedDocumentId, message.getBlockId(), currentUser);
            broadcastToDocument(documentId, createBlockMessage(MessageType.BLOCK_UNLOCKED, block, currentUser, null), null);
        } catch (Exception e) {
            log.error("处理 UNLOCK_BLOCK 失败", e);
            sendMessage(session, createErrorMessage("解锁段落失败: " + e.getMessage()));
        }
    }

    private void handleUpdateBlock(WebSocketSession session, CollaborationMessage message) {
        try {
            String userId = getUserIdFromSession(session);
            String documentId = userDocumentMapping.get(userId);
            User currentUser = requireCurrentUser(session);

            if (documentId == null) {
                sendMessage(session, createErrorMessage("用户未加入任何文档"));
                return;
            }
            if (message.getBlockId() == null) {
                sendMessage(session, createErrorMessage("缺少 blockId"));
                return;
            }

            Long parsedDocumentId = parseDocumentId(documentId);
            DocumentBlockService.BlockView block = documentBlockService.updateBlock(
                    parsedDocumentId,
                    message.getBlockId(),
                    message.getContent(),
                    message.getVersion(),
                    currentUser);
            broadcastToDocument(documentId, createBlockMessage(MessageType.BLOCK_UPDATED, block, currentUser, null), null);
        } catch (Exception e) {
            log.error("处理 UPDATE_BLOCK 失败", e);
            sendMessage(session, createErrorMessage("更新段落失败: " + e.getMessage()));
        }
    }

    private void handleCreateBlock(WebSocketSession session, CollaborationMessage message) {
        try {
            String userId = getUserIdFromSession(session);
            String documentId = userDocumentMapping.get(userId);
            User currentUser = requireCurrentUser(session);

            if (documentId == null) {
                sendMessage(session, createErrorMessage("用户未加入任何文档"));
                return;
            }
            if (message.getAfterBlockId() == null) {
                sendMessage(session, createErrorMessage("缺少 afterBlockId"));
                return;
            }

            Long parsedDocumentId = parseDocumentId(documentId);
            DocumentBlockService.BlockView block = documentBlockService.createBlockAfter(
                    parsedDocumentId,
                    message.getAfterBlockId(),
                    currentUser);
            broadcastToDocument(
                    documentId,
                    createBlockMessage(MessageType.BLOCK_CREATED, block, currentUser, message.getAfterBlockId()),
                    null);
        } catch (Exception e) {
            log.error("处理 CREATE_BLOCK 失败", e);
            sendMessage(session, createErrorMessage("新增段落失败: " + e.getMessage()));
        }
    }

    public void broadcastDocumentUpdated(
            Long documentId,
            RealTimeCollaborationService.DocumentState state,
            String operatorUserId,
            String operatorName) {
        if (documentId == null || state == null) {
            return;
        }

        CollaborationMessage message = createDocumentUpdatedMessage(documentId, state, operatorUserId, operatorName);
        broadcastToDocument(String.valueOf(documentId), message, null);
    }

    public void broadcastBlockLocked(Long documentId, DocumentBlockService.BlockView block, User operator) {
        if (documentId == null || block == null) {
            return;
        }
        broadcastToDocument(String.valueOf(documentId), createBlockMessage(MessageType.BLOCK_LOCKED, block, operator, null), null);
    }

    public void broadcastBlockUnlocked(Long documentId, DocumentBlockService.BlockView block, User operator) {
        if (documentId == null || block == null) {
            return;
        }
        broadcastToDocument(String.valueOf(documentId), createBlockMessage(MessageType.BLOCK_UNLOCKED, block, operator, null), null);
    }

    public void broadcastBlockUpdated(Long documentId, DocumentBlockService.BlockView block, User operator) {
        if (documentId == null || block == null) {
            return;
        }
        broadcastToDocument(String.valueOf(documentId), createBlockMessage(MessageType.BLOCK_UPDATED, block, operator, null), null);
    }

    public void broadcastBlockCreated(Long documentId, Long afterBlockId, DocumentBlockService.BlockView block, User operator) {
        if (documentId == null || block == null) {
            return;
        }
        broadcastToDocument(
                String.valueOf(documentId),
                createBlockMessage(MessageType.BLOCK_CREATED, block, operator, afterBlockId),
                null);
    }
    
    // ==================== 辅助方法 ====================
    
    private String getUserIdFromSession(WebSocketSession session) {
        // 从session属性中获取用户ID
        Object userIdObj = session.getAttributes().get("userId");
        return userIdObj != null ? userIdObj.toString() : null;
    }

    private Long parseDocumentId(String documentId) {
        try {
            return Long.parseLong(documentId);
        } catch (NumberFormatException ex) {
            throw new BusinessException("无效文档ID: " + documentId);
        }
    }

    private Long parseUserId(String userId) {
        try {
            return Long.parseLong(userId);
        } catch (NumberFormatException ex) {
            throw new BusinessException("无效用户ID: " + userId);
        }
    }

    private User requireCurrentUser(WebSocketSession session) {
        String userId = getUserIdFromSession(session);
        if (userId == null || userId.isEmpty()) {
            throw new BusinessException("WebSocket会话缺少用户信息");
        }
        try {
            return userService.findUserById(Long.parseLong(userId));
        } catch (NumberFormatException ex) {
            throw new BusinessException("WebSocket会话用户ID无效: " + userId);
        }
    }
    
    private void broadcastToDocument(String documentId, CollaborationMessage message, WebSocketSession excludeSession) {
        try {
            String jsonMessage = objectMapper.writeValueAsString(message);
            TextMessage textMessage = new TextMessage(jsonMessage);
            
            for (WebSocketSession session : activeSessions.values()) {
                try {
                    String userId = getUserIdFromSession(session);
                    String userDocId = userDocumentMapping.get(userId);
                    
                    if (userDocId != null && userDocId.equals(documentId) && 
                        (excludeSession == null || !session.getId().equals(excludeSession.getId()))) {
                        session.sendMessage(textMessage);
                    }
                } catch (IOException e) {
                    log.warn("向会话发送消息失败: 会话ID={}", session.getId(), e);
                }
            }
        } catch (Exception e) {
            log.error("广播消息到文档失败: 文档ID={}", documentId, e);
        }
    }
    
    private void sendMessage(WebSocketSession session, CollaborationMessage message) {
        try {
            String jsonMessage = objectMapper.writeValueAsString(message);
            session.sendMessage(new TextMessage(jsonMessage));
        } catch (IOException e) {
            log.error("发送消息失败: 会话ID={}", session.getId(), e);
        }
    }
    
    // ==================== 消息创建方法 ====================
    
    private CollaborationMessage createSuccessMessage(String content, String status) {
        CollaborationMessage message = new CollaborationMessage();
        message.setType(MessageType.SYSTEM);
        message.setContent(content);
        message.setStatus(status);
        message.setTimestamp(System.currentTimeMillis());
        return message;
    }
    
    private CollaborationMessage createErrorMessage(String content) {
        CollaborationMessage message = new CollaborationMessage();
        message.setType(MessageType.ERROR);
        message.setContent(content);
        message.setTimestamp(System.currentTimeMillis());
        return message;
    }
    
    private CollaborationMessage createJoinMessage(String userId, String userName) {
        CollaborationMessage message = new CollaborationMessage();
        message.setType(MessageType.USER_JOIN);
        message.setUserId(userId);
        message.setUserName(userName);
        message.setTimestamp(System.currentTimeMillis());
        return message;
    }
    
    private CollaborationMessage createLeaveMessage(String userId) {
        CollaborationMessage message = new CollaborationMessage();
        message.setType(MessageType.USER_LEAVE);
        message.setUserId(userId);
        message.setTimestamp(System.currentTimeMillis());
        return message;
    }
    
    @SuppressWarnings("unused")
    private CollaborationMessage createEditOperationMessage(String userId, String userName, String operation, Integer position) {
        CollaborationMessage message = new CollaborationMessage();
        message.setType(MessageType.EDIT_OPERATION);
        message.setUserId(userId);
        message.setUserName(userName);
        message.setOperation(operation);
        message.setPosition(position);
        message.setTimestamp(System.currentTimeMillis());
        return message;
    }
    
    private CollaborationMessage createCursorPositionMessage(String userId, String userName, Integer position) {
        CollaborationMessage message = new CollaborationMessage();
        message.setType(MessageType.CURSOR_POSITION);
        message.setUserId(userId);
        message.setUserName(userName);
        message.setPosition(position);
        message.setTimestamp(System.currentTimeMillis());
        return message;
    }
    
    private CollaborationMessage createChatMessage(String userId, String userName, String content) {
        CollaborationMessage message = new CollaborationMessage();
        message.setType(MessageType.CHAT_MESSAGE);
        message.setUserId(userId);
        message.setUserName(userName);
        message.setContent(content);
        message.setTimestamp(System.currentTimeMillis());
        return message;
    }
    
    private CollaborationMessage createDocumentStateMessage(Long documentId, RealTimeCollaborationService.DocumentState state) {
        CollaborationMessage message = new CollaborationMessage();
        message.setType(MessageType.DOCUMENT_STATE);
        message.setDocumentId(String.valueOf(documentId));
        message.setContent(serializeState(state));
        message.setTimestamp(System.currentTimeMillis());
        return message;
    }

    private CollaborationMessage createDocumentUpdatedMessage(
            Long documentId,
            RealTimeCollaborationService.DocumentState state,
            String userId,
            String userName) {
        CollaborationMessage message = new CollaborationMessage();
        message.setType(MessageType.DOCUMENT_UPDATED);
        message.setDocumentId(String.valueOf(documentId));
        message.setUserId(userId);
        message.setUserName(userName);
        message.setContent(serializeState(state));
        message.setTimestamp(System.currentTimeMillis());
        return message;
    }

    private String serializeState(RealTimeCollaborationService.DocumentState state) {
        try {
            return objectMapper.writeValueAsString(state);
        } catch (Exception ex) {
            throw new BusinessException("序列化文档状态失败");
        }
    }

    private CollaborationMessage createBlockMessage(
            MessageType type,
            DocumentBlockService.BlockView block,
            User operator,
            Long afterBlockId) {
        CollaborationMessage message = new CollaborationMessage();
        message.setType(type);
        message.setDocumentId(block.getDocumentId() == null ? null : String.valueOf(block.getDocumentId()));
        message.setBlockId(block.getId());
        message.setAfterBlockId(afterBlockId);
        message.setVersion(block.getVersion());
        message.setUserId(operator == null ? null : String.valueOf(operator.getId()));
        message.setUserName(operator == null ? null : operator.getUsername());
        message.setContent(serializeBlock(block));
        message.setTimestamp(System.currentTimeMillis());
        return message;
    }

    private String serializeBlock(DocumentBlockService.BlockView block) {
        try {
            return objectMapper.writeValueAsString(block);
        } catch (Exception ex) {
            throw new BusinessException("序列化文档块失败");
        }
    }
    
    // ==================== 内部类 ====================
    
    public enum MessageType {
        JOIN_DOCUMENT,      // 加入文档
        LEAVE_DOCUMENT,     // 离开文档
        EDIT_OPERATION,     // 编辑操作
        CURSOR_POSITION,    // 光标位置
        CHAT_MESSAGE,       // 聊天消息
        REQUEST_SYNC,       // 请求同步
        LOCK_BLOCK,         // 锁定段落（客户端动作）
        UNLOCK_BLOCK,       // 解锁段落（客户端动作）
        UPDATE_BLOCK,       // 更新段落（客户端动作）
        CREATE_BLOCK,       // 新增段落（客户端动作）
        USER_JOIN,          // 用户加入
        USER_LEAVE,         // 用户离开
        DOCUMENT_STATE,     // 文档状态
        DOCUMENT_UPDATED,   // 文档更新广播
        BLOCK_LOCKED,       // 段落锁定广播
        BLOCK_UNLOCKED,     // 段落解锁广播
        BLOCK_UPDATED,      // 段落更新广播
        BLOCK_CREATED,      // 段落创建广播
        SYSTEM,             // 系统消息
        ERROR               // 错误消息
    }
    
    public static class CollaborationMessage {
        private MessageType type;
        private String userId;
        private String userName;
        private String documentId;
        private String content;
        private String operation; // insert, delete, update
        private Integer position;
        private Long blockId;
        private Long afterBlockId;
        private Integer version;
        private String status; // success, error
        private Long timestamp;
        
        // Getters and Setters
        public MessageType getType() { return type; }
        public void setType(MessageType type) { this.type = type; }
        
        public String getUserId() { return userId; }
        public void setUserId(String userId) { this.userId = userId; }
        
        public String getUserName() { return userName; }
        public void setUserName(String userName) { this.userName = userName; }
        
        public String getDocumentId() { return documentId; }
        public void setDocumentId(String documentId) { this.documentId = documentId; }
        
        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }
        
        public String getOperation() { return operation; }
        public void setOperation(String operation) { this.operation = operation; }
        
        public Integer getPosition() { return position; }
        public void setPosition(Integer position) { this.position = position; }

        public Long getBlockId() { return blockId; }
        public void setBlockId(Long blockId) { this.blockId = blockId; }

        public Long getAfterBlockId() { return afterBlockId; }
        public void setAfterBlockId(Long afterBlockId) { this.afterBlockId = afterBlockId; }

        public Integer getVersion() { return version; }
        public void setVersion(Integer version) { this.version = version; }
        
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        
        public Long getTimestamp() { return timestamp; }
        public void setTimestamp(Long timestamp) { this.timestamp = timestamp; }
    }
}