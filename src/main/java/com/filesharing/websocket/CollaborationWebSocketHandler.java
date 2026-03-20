package com.filesharing.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.filesharing.entity.User;
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
            
            if (documentId == null || documentId.isEmpty()) {
                sendMessage(session, createErrorMessage("文档ID不能为空"));
                return;
            }
            
            // 更新用户文档映射
            String previousDocument = userDocumentMapping.put(userId, documentId);
            if (previousDocument != null && !previousDocument.equals(documentId)) {
                // 如果用户之前在其他文档中，先离开那个文档
                broadcastToDocument(previousDocument, createLeaveMessage(userId), session);
            }
            
            // 广播用户加入消息
            broadcastToDocument(documentId, createJoinMessage(userId, message.getUserName()), session);
            
            // 发送文档当前状态
            sendMessage(session, createDocumentStateMessage(documentId));
            
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
            
            if (documentId != null) {
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
            
            if (documentId == null) {
                sendMessage(session, createErrorMessage("用户未加入任何文档"));
                return;
            }
            
            // 转发编辑操作给其他协作者
            CollaborationMessage forwardMessage = createEditOperationMessage(
                userId, message.getUserName(), message.getOperation(), message.getPosition());
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
            
            if (documentId != null) {
                sendMessage(session, createDocumentStateMessage(documentId));
            }
            
        } catch (Exception e) {
            log.error("处理同步请求失败", e);
        }
    }
    
    // ==================== 辅助方法 ====================
    
    private String getUserIdFromSession(WebSocketSession session) {
        // 从session属性中获取用户ID
        Object userIdObj = session.getAttributes().get("userId");
        return userIdObj != null ? userIdObj.toString() : null;
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
    
    private CollaborationMessage createDocumentStateMessage(String documentId) {
        CollaborationMessage message = new CollaborationMessage();
        message.setType(MessageType.DOCUMENT_STATE);
        message.setDocumentId(documentId);
        message.setContent("{\"version\": 1, \"content\": \"文档初始内容\"}");
        message.setTimestamp(System.currentTimeMillis());
        return message;
    }
    
    // ==================== 内部类 ====================
    
    public enum MessageType {
        JOIN_DOCUMENT,      // 加入文档
        LEAVE_DOCUMENT,     // 离开文档
        EDIT_OPERATION,     // 编辑操作
        CURSOR_POSITION,    // 光标位置
        CHAT_MESSAGE,       // 聊天消息
        REQUEST_SYNC,       // 请求同步
        USER_JOIN,          // 用户加入
        USER_LEAVE,         // 用户离开
        DOCUMENT_STATE,     // 文档状态
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
        
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        
        public Long getTimestamp() { return timestamp; }
        public void setTimestamp(Long timestamp) { this.timestamp = timestamp; }
    }
}