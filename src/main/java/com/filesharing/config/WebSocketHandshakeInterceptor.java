package com.filesharing.config;

import com.filesharing.websocket.DocumentLockManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import javax.servlet.http.HttpSession;
import java.util.Map;

/**
 * WebSocket握手拦截器
 */
@Slf4j
@RequiredArgsConstructor
public class WebSocketHandshakeInterceptor implements HandshakeInterceptor {
    
    private final DocumentLockManager documentLockManager;
    
    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                 WebSocketHandler wsHandler, Map<String, Object> attributes) throws Exception {
        
        if (request instanceof ServletServerHttpRequest) {
            ServletServerHttpRequest servletRequest = (ServletServerHttpRequest) request;
            HttpSession session = servletRequest.getServletRequest().getSession(false);
            
            if (session != null) {
                // 从HTTP Session中获取用户信息
                Object userId = session.getAttribute("userId");
                Object username = session.getAttribute("username");
                
                if (userId != null) {
                    attributes.put("userId", userId.toString());
                    attributes.put("username", username != null ? username.toString() : "Unknown");
                    log.debug("WebSocket握手成功: 用户ID={}", userId);
                    return true;
                }
            }
            
            // 尝试从URL参数中获取token
            String token = servletRequest.getServletRequest().getParameter("token");
            if (token != null && !token.isEmpty()) {
                // 验证JWT token并提取用户信息
                // String userId = jwtTokenUtil.getUserIdFromToken(token);
                // if (userId != null) {
                //     attributes.put("userId", userId);
                //     attributes.put("username", jwtTokenUtil.getUsernameFromToken(token));
                //     return true;
                // }
                attributes.put("userId", "temp_user_" + System.currentTimeMillis());
                attributes.put("username", "临时用户");
                log.debug("WebSocket握手成功: 临时用户");
                return true;
            }
        }
        
        log.warn("WebSocket握手失败: 无法验证用户身份");
        return false;
    }
    
    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                             WebSocketHandler wsHandler, Exception exception) {
        // 握手完成后执行的操作
        log.debug("WebSocket握手完成");
    }
}