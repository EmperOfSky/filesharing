package com.filesharing.websocket;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * 文档锁管理器 - 管理文档的并发访问和编辑锁
 */
@Slf4j
@Component
public class DocumentLockManager {
    
    // 文档锁持有者映射：文档ID -> 用户ID
    private final Map<String, String> documentLocks = new ConcurrentHashMap<>();
    
    // 文档协作者集合：文档ID -> 用户ID集合
    private final Map<String, Set<String>> documentCollaborators = new ConcurrentHashMap<>();
    
    // 用户当前编辑的文档：用户ID -> 文档ID
    private final Map<String, String> userEditingDocuments = new ConcurrentHashMap<>();
    
    /**
     * 尝试获取文档锁
     */
    public synchronized boolean acquireLock(String documentId, String userId) {
        String currentHolder = documentLocks.get(documentId);
        
        if (currentHolder == null) {
            // 文档未被锁定，可以获取锁
            documentLocks.put(documentId, userId);
            userEditingDocuments.put(userId, documentId);
            log.debug("用户 {} 成功获取文档 {} 的锁", userId, documentId);
            return true;
        } else if (currentHolder.equals(userId)) {
            // 当前用户已经是锁持有者
            log.debug("用户 {} 已经持有文档 {} 的锁", userId, documentId);
            return true;
        } else {
            // 文档已被其他用户锁定
            log.debug("用户 {} 无法获取文档 {} 的锁，当前持有者: {}", userId, documentId, currentHolder);
            return false;
        }
    }
    
    /**
     * 释放文档锁
     */
    public synchronized boolean releaseLock(String documentId, String userId) {
        String currentHolder = documentLocks.get(documentId);
        
        if (currentHolder != null && currentHolder.equals(userId)) {
            documentLocks.remove(documentId);
            userEditingDocuments.remove(userId);
            log.debug("用户 {} 释放了文档 {} 的锁", userId, documentId);
            return true;
        } else {
            log.warn("用户 {} 尝试释放不属于自己的文档 {} 锁", userId, documentId);
            return false;
        }
    }
    
    /**
     * 检查文档是否被锁定
     */
    public boolean isLocked(String documentId) {
        return documentLocks.containsKey(documentId);
    }
    
    /**
     * 获取文档锁持有者
     */
    public String getLockHolder(String documentId) {
        return documentLocks.get(documentId);
    }
    
    /**
     * 添加文档协作者
     */
    public void addCollaborator(String documentId, String userId) {
        documentCollaborators.computeIfAbsent(documentId, k -> new CopyOnWriteArraySet<>()).add(userId);
        log.debug("用户 {} 加入文档 {} 的协作者列表", userId, documentId);
    }
    
    /**
     * 移除文档协作者
     */
    public void removeCollaborator(String documentId, String userId) {
        Set<String> collaborators = documentCollaborators.get(documentId);
        if (collaborators != null) {
            collaborators.remove(userId);
            if (collaborators.isEmpty()) {
                documentCollaborators.remove(documentId);
            }
        }
        
        // 如果用户正在编辑此文档，释放锁
        String editingDoc = userEditingDocuments.get(userId);
        if (editingDoc != null && editingDoc.equals(documentId)) {
            releaseLock(documentId, userId);
        }
        
        log.debug("用户 {} 离开文档 {} 的协作者列表", userId, documentId);
    }
    
    /**
     * 获取文档协作者列表
     */
    public Set<String> getCollaborators(String documentId) {
        Set<String> collaborators = documentCollaborators.get(documentId);
        return collaborators != null ? new CopyOnWriteArraySet<>(collaborators) : new CopyOnWriteArraySet<>();
    }
    
    /**
     * 检查用户是否有文档访问权限
     */
    public boolean hasAccess(String documentId, String userId) {
        Set<String> collaborators = documentCollaborators.get(documentId);
        return collaborators != null && collaborators.contains(userId);
    }
    
    /**
     * 获取用户当前编辑的文档
     */
    public String getEditingDocument(String userId) {
        return userEditingDocuments.get(userId);
    }
    
    /**
     * 强制释放所有锁（用于系统维护或紧急情况）
     */
    public synchronized void forceReleaseAllLocks() {
        int lockCount = documentLocks.size();
        documentLocks.clear();
        userEditingDocuments.clear();
        log.info("强制释放了 {} 个文档锁", lockCount);
    }
    
    /**
     * 获取锁状态统计信息
     */
    public LockStatistics getLockStatistics() {
        LockStatistics stats = new LockStatistics();
        stats.setTotalLockedDocuments(documentLocks.size());
        stats.setTotalCollaborationSessions(documentCollaborators.size());
        stats.setTotalActiveEditors(userEditingDocuments.size());
        
        // 计算平均协作者数
        long totalCollaborators = documentCollaborators.values().stream()
            .mapToLong(Set::size)
            .sum();
        stats.setAverageCollaboratorsPerDocument(
            documentCollaborators.isEmpty() ? 0.0 : (double) totalCollaborators / documentCollaborators.size());
            
        return stats;
    }
    
    /**
     * 锁统计信息类
     */
    public static class LockStatistics {
        private int totalLockedDocuments;
        private int totalCollaborationSessions;
        private int totalActiveEditors;
        private double averageCollaboratorsPerDocument;
        
        // Getters and Setters
        public int getTotalLockedDocuments() { return totalLockedDocuments; }
        public void setTotalLockedDocuments(int totalLockedDocuments) { this.totalLockedDocuments = totalLockedDocuments; }
        
        public int getTotalCollaborationSessions() { return totalCollaborationSessions; }
        public void setTotalCollaborationSessions(int totalCollaborationSessions) { this.totalCollaborationSessions = totalCollaborationSessions; }
        
        public int getTotalActiveEditors() { return totalActiveEditors; }
        public void setTotalActiveEditors(int totalActiveEditors) { this.totalActiveEditors = totalActiveEditors; }
        
        public double getAverageCollaboratorsPerDocument() { return averageCollaboratorsPerDocument; }
        public void setAverageCollaboratorsPerDocument(double averageCollaboratorsPerDocument) { this.averageCollaboratorsPerDocument = averageCollaboratorsPerDocument; }
    }
}