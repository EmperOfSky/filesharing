package com.filesharing.websocket;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.BinaryWebSocketHandler;

import java.net.URI;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Yjs 协作最简 WebSocket 处理器。
 *
 * 设计原则：后端只做房间消息转发，不做文本冲突处理。
 */
@Slf4j
@Component
public class YjsCollabWebSocketHandler extends BinaryWebSocketHandler {

    private static final String ATTR_DOC_ID = "docId";

    // docId -> sessions
    private final Map<String, Set<WebSocketSession>> docSessions = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String docId = resolveDocId(session.getUri());
        if (docId == null || docId.isBlank()) {
            session.close(CloseStatus.BAD_DATA.withReason("缺少文档ID"));
            return;
        }

        session.getAttributes().put(ATTR_DOC_ID, docId);
        docSessions.computeIfAbsent(docId, key -> ConcurrentHashMap.newKeySet()).add(session);
        log.debug("Yjs协作连接建立: docId={}, sessionId={}", docId, session.getId());
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        String docId = getDocId(session);
        if (docId == null) {
            return;
        }

        // 直接转发增量字符串（通常是 base64）
        broadcastText(docId, message, session);
    }

    @Override
    protected void handleBinaryMessage(WebSocketSession session, BinaryMessage message) {
        String docId = getDocId(session);
        if (docId == null) {
            return;
        }

        // 直接转发二进制增量
        broadcastBinary(docId, message, session);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        String docId = getDocId(session);
        if (docId != null) {
            Set<WebSocketSession> sessions = docSessions.get(docId);
            if (sessions != null) {
                sessions.remove(session);
                if (sessions.isEmpty()) {
                    docSessions.remove(docId);
                }
            }
        }

        log.debug("Yjs协作连接关闭: docId={}, sessionId={}, status={}", docId, session.getId(), status);
    }

    private void broadcastText(String docId, TextMessage message, WebSocketSession sender) {
        Set<WebSocketSession> sessions = docSessions.get(docId);
        if (sessions == null) {
            return;
        }

        for (WebSocketSession target : sessions) {
            if (target == null || !target.isOpen() || target.getId().equals(sender.getId())) {
                continue;
            }
            try {
                target.sendMessage(message);
            } catch (Exception ex) {
                log.warn("Yjs文本消息转发失败: docId={}, targetSessionId={}", docId, target.getId(), ex);
            }
        }
    }

    private void broadcastBinary(String docId, BinaryMessage message, WebSocketSession sender) {
        Set<WebSocketSession> sessions = docSessions.get(docId);
        if (sessions == null) {
            return;
        }

        for (WebSocketSession target : sessions) {
            if (target == null || !target.isOpen() || target.getId().equals(sender.getId())) {
                continue;
            }
            try {
                target.sendMessage(message);
            } catch (Exception ex) {
                log.warn("Yjs二进制消息转发失败: docId={}, targetSessionId={}", docId, target.getId(), ex);
            }
        }
    }

    private String getDocId(WebSocketSession session) {
        Object value = session.getAttributes().get(ATTR_DOC_ID);
        return value == null ? null : value.toString();
    }

    private String resolveDocId(URI uri) {
        if (uri == null || uri.getPath() == null) {
            return null;
        }

        String path = uri.getPath();
        String prefix = "/collab/";
        int index = path.indexOf(prefix);
        if (index < 0) {
            return null;
        }

        String docId = path.substring(index + prefix.length());
        int slash = docId.indexOf('/');
        if (slash >= 0) {
            docId = docId.substring(0, slash);
        }

        return docId.trim();
    }
}
