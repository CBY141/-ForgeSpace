package com.webwar.webwar.modules.message.websocket;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class ChatWebSocketHandler extends TextWebSocketHandler {

    private static final Map<Long, WebSocketSession> onlineUsers = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        Long userId = getUserId(session);
        if (userId != null) {
            onlineUsers.put(userId, session);
            System.out.println("✅ WebSocket 连接成功：用户 " + userId + "，当前在线人数：" + onlineUsers.size());
        } else {
            System.out.println("❌ WebSocket 连接失败：无法获取 userId，URL：" + session.getUri());
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        // 不需要处理客户端发来的消息
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        Long userId = getUserId(session);
        if (userId != null) {
            onlineUsers.remove(userId);
            System.out.println("🔴 WebSocket 断开：用户 " + userId + "，当前在线人数：" + onlineUsers.size());
        }
    }

    public void sendMessage(Long userId, String json) {
        WebSocketSession session = onlineUsers.get(userId);
        if (session != null && session.isOpen()) {
            try {
                session.sendMessage(new TextMessage(json));
                System.out.println("📤 消息已推送给用户 " + userId);
            } catch (IOException e) {
                System.out.println("❌ 推送失败给用户 " + userId + "：" + e.getMessage());
                e.printStackTrace();
            }
        } else {
            System.out.println("⚠️ 用户 " + userId + " 不在线，无法推送。当前在线用户：" + onlineUsers.keySet());
        }
    }

    private Long getUserId(WebSocketSession session) {
        String query = session.getUri().getQuery();
        if (query != null && query.startsWith("userId=")) {
            try {
                return Long.parseLong(query.substring(7));
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }
}