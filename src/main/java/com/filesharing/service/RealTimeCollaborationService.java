package com.filesharing.service;

import com.filesharing.entity.User;

import java.util.List;
import java.util.Map;

/**
 * 实时协作服务接口
 */
public interface RealTimeCollaborationService {
    
    /**
     * 初始化文档协作会话
     */
    CollaborationSession initializeCollaborationSession(Long documentId, User user);
    
    /**
     * 加入文档协作
     */
    boolean joinCollaboration(Long documentId, User user);
    
    /**
     * 离开文档协作
     */
    boolean leaveCollaboration(Long documentId, User user);
    
    /**
     * 处理编辑操作
     */
    EditOperationResult processEditOperation(EditOperation operation, User user);
    
    /**
     * 同步文档状态
     */
    DocumentState syncDocumentState(Long documentId, User user);
    
    /**
     * 获取文档协作者列表
     */
    List<CollaboratorInfo> getDocumentCollaborators(Long documentId);
    
    /**
     * 发送聊天消息
     */
    boolean sendChatMessage(ChatMessage message, User sender);
    
    /**
     * 获取协作统计信息
     */
    CollaborationStatistics getCollaborationStatistics();
    
    /**
     * 文档协作会话信息
     */
    class CollaborationSession {
        private Long documentId;
        private String sessionId;
        private List<CollaboratorInfo> collaborators;
        private DocumentState initialState;
        private Long createdAt;
        
        // Getters and Setters
        public Long getDocumentId() { return documentId; }
        public void setDocumentId(Long documentId) { this.documentId = documentId; }
        
        public String getSessionId() { return sessionId; }
        public void setSessionId(String sessionId) { this.sessionId = sessionId; }
        
        public List<CollaboratorInfo> getCollaborators() { return collaborators; }
        public void setCollaborators(List<CollaboratorInfo> collaborators) { this.collaborators = collaborators; }
        
        public DocumentState getInitialState() { return initialState; }
        public void setInitialState(DocumentState initialState) { this.initialState = initialState; }
        
        public Long getCreatedAt() { return createdAt; }
        public void setCreatedAt(Long createdAt) { this.createdAt = createdAt; }
    }
    
    /**
     * 协作者信息
     */
    class CollaboratorInfo {
        private String userId;
        private String username;
        private String avatar;
        private String cursorPosition;
        private Long lastActiveTime;
        private String status; // online, offline, away
        
        // Getters and Setters
        public String getUserId() { return userId; }
        public void setUserId(String userId) { this.userId = userId; }
        
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        
        public String getAvatar() { return avatar; }
        public void setAvatar(String avatar) { this.avatar = avatar; }
        
        public String getCursorPosition() { return cursorPosition; }
        public void setCursorPosition(String cursorPosition) { this.cursorPosition = cursorPosition; }
        
        public Long getLastActiveTime() { return lastActiveTime; }
        public void setLastActiveTime(Long lastActiveTime) { this.lastActiveTime = lastActiveTime; }
        
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
    }
    
    /**
     * 编辑操作
     */
    class EditOperation {
        private String type; // insert, delete, update
        private String content;
        private Integer position;
        private Integer length;
        private Long version; // 文档版本号
        private Long timestamp;
        private String userId;
        private Long documentId;
        
        // Getters and Setters
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        
        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }
        
        public Integer getPosition() { return position; }
        public void setPosition(Integer position) { this.position = position; }
        
        public Integer getLength() { return length; }
        public void setLength(Integer length) { this.length = length; }
        
        public Long getVersion() { return version; }
        public void setVersion(Long version) { this.version = version; }
        
        public Long getTimestamp() { return timestamp; }
        public void setTimestamp(Long timestamp) { this.timestamp = timestamp; }
        
        public String getUserId() { return userId; }
        public void setUserId(String userId) { this.userId = userId; }
        
        public Long getDocumentId() { return documentId; }
        public void setDocumentId(Long documentId) { this.documentId = documentId; }
    }
    
    /**
     * 编辑操作结果
     */
    class EditOperationResult {
        private boolean success;
        private String message;
        private DocumentState newState;
        private List<ConflictResolution> conflicts;
        
        // Getters and Setters
        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }
        
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        
        public DocumentState getNewState() { return newState; }
        public void setNewState(DocumentState newState) { this.newState = newState; }
        
        public List<ConflictResolution> getConflicts() { return conflicts; }
        public void setConflicts(List<ConflictResolution> conflicts) { this.conflicts = conflicts; }
    }
    
    /**
     * 冲突解决方案
     */
    class ConflictResolution {
        private String type; // auto-resolved, manual-required
        private String description;
        private String resolution;
        
        // Getters and Setters
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        
        public String getResolution() { return resolution; }
        public void setResolution(String resolution) { this.resolution = resolution; }
    }
    
    /**
     * 文档状态
     */
    class DocumentState {
        private Long documentId;
        private String content;
        private Long version;
        private Map<String, Object> metadata;
        private Long lastModified;
        
        // Getters and Setters
        public Long getDocumentId() { return documentId; }
        public void setDocumentId(Long documentId) { this.documentId = documentId; }
        
        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }
        
        public Long getVersion() { return version; }
        public void setVersion(Long version) { this.version = version; }
        
        public Map<String, Object> getMetadata() { return metadata; }
        public void setMetadata(Map<String, Object> metadata) { this.metadata = metadata; }
        
        public Long getLastModified() { return lastModified; }
        public void setLastModified(Long lastModified) { this.lastModified = lastModified; }
    }
    
    /**
     * 聊天消息
     */
    class ChatMessage {
        private Long documentId;
        private String userId;
        private String username;
        private String content;
        private Long timestamp;
        private String messageType; // text, system, emoji
        
        // Getters and Setters
        public Long getDocumentId() { return documentId; }
        public void setDocumentId(Long documentId) { this.documentId = documentId; }
        
        public String getUserId() { return userId; }
        public void setUserId(String userId) { this.userId = userId; }
        
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        
        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }
        
        public Long getTimestamp() { return timestamp; }
        public void setTimestamp(Long timestamp) { this.timestamp = timestamp; }
        
        public String getMessageType() { return messageType; }
        public void setMessageType(String messageType) { this.messageType = messageType; }
    }
    
    /**
     * 协作统计信息
     */
    class CollaborationStatistics {
        private int activeSessions;
        private int totalCollaborators;
        private int documentsBeingEdited;
        private double averageCollaboratorsPerDocument;
        private Map<String, Integer> editOperationStats;
        
        // Getters and Setters
        public int getActiveSessions() { return activeSessions; }
        public void setActiveSessions(int activeSessions) { this.activeSessions = activeSessions; }
        
        public int getTotalCollaborators() { return totalCollaborators; }
        public void setTotalCollaborators(int totalCollaborators) { this.totalCollaborators = totalCollaborators; }
        
        public int getDocumentsBeingEdited() { return documentsBeingEdited; }
        public void setDocumentsBeingEdited(int documentsBeingEdited) { this.documentsBeingEdited = documentsBeingEdited; }
        
        public double getAverageCollaboratorsPerDocument() { return averageCollaboratorsPerDocument; }
        public void setAverageCollaboratorsPerDocument(double averageCollaboratorsPerDocument) { this.averageCollaboratorsPerDocument = averageCollaboratorsPerDocument; }
        
        public Map<String, Integer> getEditOperationStats() { return editOperationStats; }
        public void setEditOperationStats(Map<String, Integer> editOperationStats) { this.editOperationStats = editOperationStats; }
    }
}