package com.filesharing.config;

import com.filesharing.websocket.CollaborationWebSocketHandler;
import com.filesharing.websocket.DocumentLockManager;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/**
 * WebSocket配置类
 */
@Configuration
@EnableWebSocket
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketConfigurer {
    
    private final CollaborationWebSocketHandler collaborationWebSocketHandler;
    private final DocumentLockManager documentLockManager;
    
    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        // 注册协作WebSocket处理器
        registry.addHandler(collaborationWebSocketHandler, "/ws/collaboration")
                .setAllowedOrigins("*") // 生产环境中应该限制具体的域名
                .addInterceptors(new WebSocketHandshakeInterceptor(documentLockManager));
    }
}