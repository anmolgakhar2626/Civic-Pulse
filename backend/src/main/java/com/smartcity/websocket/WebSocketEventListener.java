package com.smartcity.websocket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.*;

import java.security.Principal;

/**
 * Monitors WebSocket STOMP session lifecycle events (connect, disconnect, subscribe).
 */
@Component
public class WebSocketEventListener {

    private static final Logger log = LoggerFactory.getLogger(WebSocketEventListener.class);

    @EventListener
    public void handleSessionConnected(SessionConnectedEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        Principal user = accessor.getUser();
        String username = user != null ? user.getName() : "anonymous";
        log.info("WebSocket client connected [sessionId: {}, user: {}]", accessor.getSessionId(), username);
    }

    @EventListener
    public void handleSessionDisconnect(SessionDisconnectEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        Principal user = accessor.getUser();
        String username = user != null ? user.getName() : "anonymous";
        log.info("WebSocket client disconnected [sessionId: {}, user: {}]", accessor.getSessionId(), username);
    }

    @EventListener
    public void handleSessionSubscribe(SessionSubscribeEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        String destination = accessor.getDestination();
        Principal user = accessor.getUser();
        String username = user != null ? user.getName() : "anonymous";
        log.debug("WebSocket client subscribed [destination: {}, user: {}]", destination, username);
    }

    @EventListener
    public void handleSessionUnsubscribe(SessionUnsubscribeEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        log.debug("WebSocket client unsubscribed [sessionId: {}]", accessor.getSessionId());
    }
}
