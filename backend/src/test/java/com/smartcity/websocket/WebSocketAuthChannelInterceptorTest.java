package com.smartcity.websocket;

import com.smartcity.security.JwtUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("WebSocketAuthChannelInterceptor Unit Tests")
class WebSocketAuthChannelInterceptorTest {

    private JwtUtils jwtUtils;
    private UserDetailsService userDetailsService;
    private MessageChannel messageChannel;
    private WebSocketAuthChannelInterceptor interceptor;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
        jwtUtils = new JwtUtils();
        ReflectionTestUtils.setField(jwtUtils, "jwtSecret", "c3VwZXJzZWNyZXRqd3RzZWNyZXRrZXlmb3JzbWFydGNpdHljb21tYW5kY2VudHJlY2l2aWNwdWxzZQ==");
        ReflectionTestUtils.setField(jwtUtils, "jwtExpirationMs", 86400000L);

        userDetailsService = mock(UserDetailsService.class);
        messageChannel = mock(MessageChannel.class);
        interceptor = new WebSocketAuthChannelInterceptor(jwtUtils, userDetailsService);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Should authenticate CONNECT frame with valid Authorization Bearer token")
    void shouldAuthenticateValidBearerToken() {
        String username = "operator_alex";
        String token = jwtUtils.generateTokenFromUsername(username, List.of("ROLE_COMMAND_OPERATOR"));

        UserDetails userDetails = new User(username, "password",
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_COMMAND_OPERATOR")));
        when(userDetailsService.loadUserByUsername(username)).thenReturn(userDetails);

        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        accessor.setLeaveMutable(true);
        accessor.setNativeHeader("Authorization", "Bearer " + token);
        Message<?> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        Message<?> result = interceptor.preSend(message, messageChannel);

        assertNotNull(result);
        StompHeaderAccessor resultAccessor = StompHeaderAccessor.wrap(result);
        assertNotNull(resultAccessor.getUser());
        assertEquals(username, resultAccessor.getUser().getName());
    }

    @Test
    @DisplayName("Should authenticate CONNECT frame with valid token in 'token' header")
    void shouldAuthenticateValidCustomTokenHeader() {
        String username = "admin_sarah";
        String token = jwtUtils.generateTokenFromUsername(username, List.of("ROLE_ADMIN"));

        UserDetails userDetails = new User(username, "password",
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_ADMIN")));
        when(userDetailsService.loadUserByUsername(username)).thenReturn(userDetails);

        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        accessor.setLeaveMutable(true);
        accessor.setNativeHeader("token", token);
        Message<?> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        Message<?> result = interceptor.preSend(message, messageChannel);

        assertNotNull(result);
        StompHeaderAccessor resultAccessor = StompHeaderAccessor.wrap(result);
        assertNotNull(resultAccessor.getUser());
        assertEquals(username, resultAccessor.getUser().getName());
    }

    @Test
    @DisplayName("Should reject CONNECT frame with invalid JWT token")
    void shouldRejectInvalidToken() {
        String token = "invalid.token.structure";

        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        accessor.setNativeHeader("Authorization", "Bearer " + token);
        Message<?> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        assertThrows(IllegalArgumentException.class, () -> interceptor.preSend(message, messageChannel));
    }

    @Test
    @DisplayName("Should allow CONNECT frame without token as anonymous connection")
    void shouldAllowAnonymousConnection() {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        Message<?> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        Message<?> result = interceptor.preSend(message, messageChannel);

        assertNotNull(result);
        StompHeaderAccessor resultAccessor = StompHeaderAccessor.wrap(result);
        assertNull(resultAccessor.getUser());
    }

    @Test
    @DisplayName("Should ignore non-CONNECT frames like SUBSCRIBE or SEND")
    void shouldIgnoreNonConnectFrames() {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        accessor.setDestination("/topic/incidents");
        Message<?> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        Message<?> result = interceptor.preSend(message, messageChannel);

        assertNotNull(result);
        verifyNoInteractions(userDetailsService);
    }
}
