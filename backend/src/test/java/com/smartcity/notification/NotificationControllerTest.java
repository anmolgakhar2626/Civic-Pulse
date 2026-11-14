package com.smartcity.notification;

import com.smartcity.common.GlobalExceptionHandler;
import com.smartcity.common.ResourceNotFoundException;
import com.smartcity.user.User;
import com.smartcity.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.security.Principal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("NotificationController REST Endpoints Tests")
class NotificationControllerTest {

    private MockMvc mockMvc;

    @Mock
    private NotificationService notificationService;

    @Mock
    private UserRepository userRepository;

    private User testUser;
    private UsernamePasswordAuthenticationToken auth;

    @BeforeEach
    void setUp() {
        NotificationController controller = new NotificationController(notificationService, userRepository);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        testUser = new User();
        testUser.setId(UUID.randomUUID());
        testUser.setUsername("operator");
        testUser.setEmail("operator@civicpulse.gov");

        auth = new UsernamePasswordAuthenticationToken(
                "operator",
                "N/A",
                List.of(new SimpleGrantedAuthority("ROLE_COMMAND_OPERATOR"))
        );
    }

    @Test
    @DisplayName("GET /api/notifications returns 200 with list of notifications")
    void testGetNotifications() throws Exception {
        when(userRepository.findByUsername("operator")).thenReturn(Optional.of(testUser));

        NotificationDTO dto = new NotificationDTO();
        dto.setId(UUID.randomUUID());
        dto.setTitle("Flash Flood Warning");
        dto.setMessage("Water level rising");
        dto.setNotificationType("CRITICAL_ALERT");
        dto.setSeverity("CRITICAL");
        dto.setRead(false);

        when(notificationService.getNotificationsForUser(eq(testUser.getId()), eq(List.of("COMMAND_OPERATOR")), eq(false), any(), any(), eq(0), eq(20)))
                .thenReturn(List.of(dto));

        mockMvc.perform(get("/api/notifications")
                        .principal(auth)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].title").value("Flash Flood Warning"))
                .andExpect(jsonPath("$.data[0].severity").value("CRITICAL"));
    }

    @Test
    @DisplayName("GET /api/notifications/unread-count returns 200 with unread count")
    void testGetUnreadCount() throws Exception {
        when(userRepository.findByUsername("operator")).thenReturn(Optional.of(testUser));
        when(notificationService.getUnreadCountForUser(eq(testUser.getId()), eq(List.of("COMMAND_OPERATOR"))))
                .thenReturn(5L);

        mockMvc.perform(get("/api/notifications/unread-count")
                        .principal(auth)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.unreadCount").value(5));
    }

    @Test
    @DisplayName("PATCH /api/notifications/{id}/read marks notification as read")
    void testMarkAsRead() throws Exception {
        UUID notifId = UUID.randomUUID();
        when(userRepository.findByUsername("operator")).thenReturn(Optional.of(testUser));

        NotificationDTO dto = new NotificationDTO();
        dto.setId(notifId);
        dto.setTitle("SLA Breached");
        dto.setRead(true);
        dto.setStatus("READ");

        when(notificationService.markAsRead(eq(notifId), eq(testUser.getId()), any())).thenReturn(dto);

        mockMvc.perform(patch("/api/notifications/" + notifId + "/read")
                        .principal(auth)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.read").value(true));
    }

    @Test
    @DisplayName("PATCH /api/notifications/read-all marks all notifications as read")
    void testMarkAllAsRead() throws Exception {
        when(userRepository.findByUsername("operator")).thenReturn(Optional.of(testUser));
        when(notificationService.markAllAsRead(eq(testUser.getId()), eq(List.of("COMMAND_OPERATOR")))).thenReturn(4);

        mockMvc.perform(patch("/api/notifications/read-all")
                        .principal(auth)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.markedCount").value(4));
    }

    @Test
    @DisplayName("DELETE /api/notifications/{id} deletes notification when authorized")
    void testDeleteNotification() throws Exception {
        UUID notifId = UUID.randomUUID();
        when(userRepository.findByUsername("operator")).thenReturn(Optional.of(testUser));
        when(notificationService.deleteNotification(eq(notifId), eq(testUser.getId()), any())).thenReturn(true);

        mockMvc.perform(delete("/api/notifications/" + notifId)
                        .principal(auth)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("DELETE /api/notifications/{id} returns 403 when user does not own notification")
    void testDeleteNotificationForbidden() throws Exception {
        UUID notifId = UUID.randomUUID();
        when(userRepository.findByUsername("operator")).thenReturn(Optional.of(testUser));
        when(notificationService.deleteNotification(eq(notifId), eq(testUser.getId()), any()))
                .thenThrow(new AccessDeniedException("User not authorized"));

        mockMvc.perform(delete("/api/notifications/" + notifId)
                        .principal(auth)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }
}
