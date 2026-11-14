package com.smartcity.notification;

import com.smartcity.notification.event.*;
import com.smartcity.user.User;
import com.smartcity.user.UserRepository;
import com.smartcity.websocket.WebSocketEventPublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("NotificationEventListener Unit Tests")
class NotificationEventListenerTest {

    @Mock
    private NotificationDao notificationDao;

    @Mock
    private UserRepository userRepository;

    @Mock
    private WebSocketEventPublisher webSocketEventPublisher;

    private NotificationEventListener listener;

    @BeforeEach
    void setUp() {
        listener = new NotificationEventListener(notificationDao, userRepository, webSocketEventPublisher);
        lenient().when(notificationDao.save(any(Notification.class))).thenAnswer(inv -> {
            Notification n = inv.getArgument(0);
            return NotificationDTO.fromEntity(n);
        });
    }

    @Test
    @DisplayName("handleIncidentCreated for CRITICAL incident notifies COMMAND_OPERATOR and ADMIN")
    void testHandleCriticalIncidentCreated() {
        UUID incId = UUID.randomUUID();
        IncidentCreatedNotificationEvent event = new IncidentCreatedNotificationEvent(
                incId, "INC-2026-001", "Chemical Spill", "Pollution", "CRITICAL",
                UUID.randomUUID(), null, "Environment Dept"
        );

        when(notificationDao.existsByDedupKey("INC_CREATED_" + incId)).thenReturn(false);

        listener.handleIncidentCreated(event);

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationDao, times(2)).save(captor.capture());

        assertThat(captor.getAllValues()).extracting(Notification::getRecipientRole)
                .containsExactlyInAnyOrder("COMMAND_OPERATOR", "ADMIN");
        assertThat(captor.getAllValues()).allMatch(n -> "CRITICAL".equals(n.getSeverity()));
    }

    @Test
    @DisplayName("handleIncidentCreated deduplicates when dedup key exists")
    void testHandleIncidentCreatedDuplicate() {
        UUID incId = UUID.randomUUID();
        IncidentCreatedNotificationEvent event = new IncidentCreatedNotificationEvent(
                incId, "INC-2026-001", "Chemical Spill", "Pollution", "CRITICAL",
                UUID.randomUUID(), null, "Environment Dept"
        );

        when(notificationDao.existsByDedupKey("INC_CREATED_" + incId)).thenReturn(true);

        listener.handleIncidentCreated(event);

        verify(notificationDao, never()).save(any(Notification.class));
    }

    @Test
    @DisplayName("handleIncidentAssigned notifies the assigned operator")
    void testHandleIncidentAssigned() {
        UUID incId = UUID.randomUUID();
        UUID operatorId = UUID.randomUUID();
        User operator = new User();
        operator.setId(operatorId);
        operator.setUsername("traffic_operator");

        when(userRepository.findById(operatorId)).thenReturn(Optional.of(operator));

        IncidentAssignedNotificationEvent event = new IncidentAssignedNotificationEvent(
                incId, "INC-2026-002", "Traffic Jam", "HIGH", operatorId, "admin"
        );

        listener.handleIncidentAssigned(event);

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationDao, times(1)).save(captor.capture());

        Notification saved = captor.getValue();
        assertThat(saved.getRecipientUser().getId()).isEqualTo(operatorId);
        assertThat(saved.getNotificationType()).isEqualTo("INCIDENT_ASSIGNED");
        assertThat(saved.getSeverity()).isEqualTo("HIGH");
    }

    @Test
    @DisplayName("handleIncidentStatusChanged to RESOLVED notifies reporting citizen")
    void testHandleIncidentResolvedNotifiesCitizen() {
        UUID incId = UUID.randomUUID();
        UUID citizenId = UUID.randomUUID();
        User citizen = new User();
        citizen.setId(citizenId);
        citizen.setUsername("citizen_john");

        when(userRepository.findById(citizenId)).thenReturn(Optional.of(citizen));

        IncidentStatusChangedNotificationEvent event = new IncidentStatusChangedNotificationEvent(
                incId, "INC-2026-003", "Pothole on Main St", "IN_PROGRESS", "RESOLVED",
                "operator", citizenId, null
        );

        listener.handleIncidentStatusChanged(event);

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationDao, times(1)).save(captor.capture());

        Notification saved = captor.getValue();
        assertThat(saved.getRecipientUser().getId()).isEqualTo(citizenId);
        assertThat(saved.getNotificationType()).isEqualTo("INCIDENT_RESOLVED");
    }

    @Test
    @DisplayName("handleSlaBreached notifies COMMAND_OPERATOR and ADMIN")
    void testHandleSlaBreached() {
        UUID slaId = UUID.randomUUID();
        UUID incId = UUID.randomUUID();
        SlaBreachedNotificationEvent event = new SlaBreachedNotificationEvent(
                slaId, incId, "INC-2026-004", Instant.now(), null, "Public Works"
        );

        when(notificationDao.existsByDedupKey("SLA_BREACH_" + slaId)).thenReturn(false);

        listener.handleSlaBreached(event);

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationDao, times(2)).save(captor.capture());

        assertThat(captor.getAllValues()).extracting(Notification::getRecipientRole)
                .containsExactlyInAnyOrder("COMMAND_OPERATOR", "ADMIN");
        assertThat(captor.getAllValues()).allMatch(n -> "CRITICAL".equals(n.getSeverity()));
    }
}
