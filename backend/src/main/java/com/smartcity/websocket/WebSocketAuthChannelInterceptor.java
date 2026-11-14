package com.smartcity.websocket;

import com.smartcity.security.JwtUtils;
import com.smartcity.security.UserDetailsServiceImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Intercepts STOMP messages to authenticate WebSocket connections using JWT tokens
 * transmitted in the CONNECT frame native headers.
 */
import org.springframework.security.core.userdetails.UserDetailsService;

@Component
public class WebSocketAuthChannelInterceptor implements ChannelInterceptor {

    private static final Logger log = LoggerFactory.getLogger(WebSocketAuthChannelInterceptor.class);

    private final JwtUtils jwtUtils;
    private final UserDetailsService userDetailsService;

    public WebSocketAuthChannelInterceptor(JwtUtils jwtUtils, UserDetailsService userDetailsService) {
        this.jwtUtils = jwtUtils;
        this.userDetailsService = userDetailsService;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
            String token = extractToken(accessor);

            if (token != null && !token.isBlank()) {
                try {
                    if (jwtUtils.validateJwtToken(token)) {
                        String username = jwtUtils.getUsernameFromJwtToken(token);
                        UserDetails userDetails = userDetailsService.loadUserByUsername(username);
                        UsernamePasswordAuthenticationToken authentication =
                                new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());

                        SecurityContextHolder.getContext().setAuthentication(authentication);

                        if (accessor.isMutable()) {
                            accessor.setUser(authentication);
                        } else {
                            StompHeaderAccessor mutableAccessor = StompHeaderAccessor.create(accessor.getCommand());
                            mutableAccessor.copyHeaders(accessor.getMessageHeaders());
                            mutableAccessor.setUser(authentication);
                            log.info("WebSocket STOMP connection authenticated for user: {}", username);
                            return MessageBuilder.createMessage(message.getPayload(), mutableAccessor.getMessageHeaders());
                        }
                        log.info("WebSocket STOMP connection authenticated for user: {}", username);
                    } else {
                        log.warn("WebSocket STOMP connection rejected: invalid JWT token");
                        throw new IllegalArgumentException("Invalid JWT token for WebSocket connection");
                    }
                } catch (IllegalArgumentException e) {
                    throw e;
                } catch (Exception e) {
                    log.warn("WebSocket STOMP authentication failed: {}", e.getMessage());
                    throw new IllegalArgumentException("Authentication failed: " + e.getMessage());
                }
            } else {
                // Anonymous connection permitted for public telemetry feeds
                log.debug("WebSocket STOMP connection established without authentication token (anonymous)");
            }
        }

        return message;
    }

    private String extractToken(StompHeaderAccessor accessor) {
        // 1. Check native header 'Authorization'
        List<String> authHeaders = accessor.getNativeHeader("Authorization");
        if (authHeaders != null && !authHeaders.isEmpty()) {
            String header = authHeaders.getFirst();
            if (header != null && header.startsWith("Bearer ")) {
                return header.substring(7).trim();
            }
            return header != null ? header.trim() : null;
        }

        // 2. Check native header 'X-Authorization'
        List<String> xAuthHeaders = accessor.getNativeHeader("X-Authorization");
        if (xAuthHeaders != null && !xAuthHeaders.isEmpty()) {
            String header = xAuthHeaders.getFirst();
            if (header != null && header.startsWith("Bearer ")) {
                return header.substring(7).trim();
            }
            return header != null ? header.trim() : null;
        }

        // 3. Check native header 'token' or 'passcode'
        List<String> tokenHeaders = accessor.getNativeHeader("token");
        if (tokenHeaders != null && !tokenHeaders.isEmpty()) {
            return tokenHeaders.getFirst().trim();
        }

        return null;
    }
}
