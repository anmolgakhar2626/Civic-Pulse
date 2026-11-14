package com.smartcity.notification;

import com.smartcity.incident.Incident;
import com.smartcity.sla.SLARecord;
import com.smartcity.user.User;
import com.smartcity.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.time.Instant;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class NotificationServiceTest {

    private NotificationRepository notificationRepository;
    private NotificationSender inAppSender;
    private NotificationSender emailSender;
    private NotificationSender smsSender;
    private UserRepository userRepository;
    private NotificationServiceImpl notificationService;

    @BeforeEach
    void setUp() {
        notificationRepository = mock(NotificationRepository.class);
        inAppSender = mock(NotificationSender.class);
        emailSender = mock(NotificationSender.class);
        smsSender = mock(NotificationSender.class);
        userRepository = mock(UserRepository.class);

        when(inAppSender.supports(NotificationChannel.IN_APP)).thenReturn(true);
        when(emailSender.supports(NotificationChannel.EMAIL)).thenReturn(true);
        when(smsSender.supports(NotificationChannel.SMS)).thenReturn(true);

        when(notificationRepository.save(any(Notification.class))).thenAnswer(inv -> {
            Notification n = inv.getArgument(0);
            if (n.getId() == null) {
                n.setId(UUID.randomUUID());
            }
            return n;
        });

        notificationService = new NotificationServiceImpl(
                notificationRepository,
                List.of(inAppSender, emailSender, smsSender),
                userRepository
        );
    }

    @Test
    @DisplayName("CRITICAL incident notification notifies Command Centre and responsible department via multiple channels")
    void testCriticalIncidentNotificationRules() {
        Incident incident = new Incident();
        incident.setId(UUID.randomUUID());
        incident.setIncidentNumber("INC-TEST-001");
        incident.setIncidentType("MAJOR_ROAD_ACCIDENT");
        incident.setDescription("Multi-vehicle collision on highway");
        incident.setLatitude(30.7333);
        incident.setLongitude(76.7794);

        notificationService.notifyCriticalIncident(incident, "Traffic Police");

        // Should create notifications for COMMAND_OPERATOR (IN_APP, EMAIL, SMS) and DEPARTMENT_OFFICER (IN_APP, EMAIL)
        verify(notificationRepository, atLeast(5)).save(any(Notification.class));
        verify(inAppSender, atLeast(2)).send(any(Notification.class));
        verify(emailSender, atLeast(2)).send(any(Notification.class));
        verify(smsSender, atLeast(1)).send(any(Notification.class));
    }

    @Test
    @DisplayName("SLA AT_RISK notification notifies assigned department")
    void testSlaAtRiskNotificationRules() {
        SLARecord sla = new SLARecord();
        Incident incident = new Incident();
        incident.setIncidentNumber("INC-SLA-001");
        sla.setIncident(incident);
        sla.setDeadlineAt(Instant.now().plusSeconds(600));

        notificationService.notifySlaAtRisk(sla, "Public Works Department");

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository, atLeast(2)).save(captor.capture());

        List<Notification> saved = captor.getAllValues();
        assertThat(saved).anyMatch(n -> n.getNotificationType().equals(NotificationType.SLA_AT_RISK.name())
                && "DEPARTMENT_OFFICER".equals(n.getRecipientRole()));
    }

    @Test
    @DisplayName("SLA BREACHED notification notifies department officer and command operator")
    void testSlaBreachedNotificationRules() {
        SLARecord sla = new SLARecord();
        Incident incident = new Incident();
        incident.setIncidentNumber("INC-SLA-002");
        sla.setIncident(incident);
        sla.setDeadlineAt(Instant.now().minusSeconds(100));

        notificationService.notifySlaBreached(sla, "Fire & Rescue");

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository, atLeast(5)).save(captor.capture());

        List<Notification> saved = captor.getAllValues();
        assertThat(saved).anyMatch(n -> "DEPARTMENT_OFFICER".equals(n.getRecipientRole()));
        assertThat(saved).anyMatch(n -> "COMMAND_OPERATOR".equals(n.getRecipientRole()));
    }

    @Test
    @DisplayName("LEVEL_3 escalation notifies ADMIN and Command Centre")
    void testLevel3EscalationNotificationRules() {
        SLARecord sla = new SLARecord();
        Incident incident = new Incident();
        incident.setIncidentNumber("INC-SLA-003");
        sla.setIncident(incident);

        notificationService.notifyLevel3Escalation(sla, "Executive timeout reached");

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository, atLeast(4)).save(captor.capture());

        List<Notification> saved = captor.getAllValues();
        assertThat(saved).anyMatch(n -> "ADMIN".equals(n.getRecipientRole()));
        assertThat(saved).anyMatch(n -> "COMMAND_OPERATOR".equals(n.getRecipientRole()));
    }

    @Test
    @DisplayName("Mark notification as read updates status and timestamp")
    void testMarkAsRead() {
        UUID id = UUID.randomUUID();
        Notification notification = new Notification(null, "ADMIN", "Test", "Message", "CRITICAL_ALERT", "IN_APP");
        notification.setId(id);
        notification.setStatus("UNREAD");

        when(notificationRepository.findById(id)).thenReturn(Optional.of(notification));

        NotificationDTO dto = notificationService.markAsRead(id);

        assertThat(dto.getStatus()).isEqualTo("READ");
        assertThat(dto.getReadAt()).isNotNull();
    }
}
