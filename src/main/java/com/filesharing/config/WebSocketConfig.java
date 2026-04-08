package com.filesharing.config;

import com.filesharing.websocket.CollaborationWebSocketHandler;
import com.filesharing.websocket.YjsCollabWebSocketHandler;
import com.filesharing.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

import java.util.List;

/**
 * WebSocket配置类
 */
@Configuration
@EnableWebSocket
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketConfigurer {

    private final CollaborationWebSocketHandler collaborationWebSocketHandler;
    private final YjsCollabWebSocketHandler yjsCollabWebSocketHandler;
    private final JwtUtil jwtUtil;
    private final SecurityHardeningProperties securityHardeningProperties;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        List<String> allowedOrigins = securityHardeningProperties.getCors().getAllowedOrigins();
        List<String> allowedOriginPatterns = securityHardeningProperties.getCors().getAllowedOriginPatterns();

        // 注册协作WebSocket处理器
        var collaborationHandlerRegistration = registry.addHandler(collaborationWebSocketHandler, "/ws/collaboration")
                .setAllowedOrigins(allowedOrigins.toArray(new String[0]));
        if (!allowedOriginPatterns.isEmpty()) {
            collaborationHandlerRegistration.setAllowedOriginPatterns(allowedOriginPatterns.toArray(new String[0]));
        }
        collaborationHandlerRegistration.addInterceptors(new WebSocketHandshakeInterceptor(jwtUtil));

        // 最简Yjs协作通道：后端仅负责按文档房间转发增量消息
        var yjsHandlerRegistration = registry.addHandler(yjsCollabWebSocketHandler, "/collab/{docId}")
                .setAllowedOrigins(allowedOrigins.toArray(new String[0]));
        if (!allowedOriginPatterns.isEmpty()) {
            yjsHandlerRegistration.setAllowedOriginPatterns(allowedOriginPatterns.toArray(new String[0]));
        }
    }
}