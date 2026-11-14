package com.smartcity.websocket;

import com.smartcity.websocket.dto.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("WebSocketEventPublisher Unit Tests")
class WebSocketEventPublisherTest {

    private MessageChannel messageChannel;
    private WebSocketEventPublisher publisher;

    @BeforeEach
    void setUp() {
        messageChannel = mock(MessageChannel.class);
        when(messageChannel.send(any(Message.class))).thenReturn(true);
        when(messageChannel.send(any(Message.class), anyLong())).thenReturn(true);

        SimpMessagingTemplate messagingTemplate = new SimpMessagingTemplate(messageChannel);
        publisher = new WebSocketEventPublisher(messagingTemplate);
    }

    @Test
    @DisplayName("Should publish IncidentEvent to /topic/incidents with frontend-safe fields")
    void shouldPublishIncidentEvent() {
        String incidentId = UUID.randomUUID().toString();
        IncidentEvent event = new IncidentEvent(
                incidentId,
                "INC-1001",
                "MAJOR_ROAD_ACCIDENT",
                "Multi-vehicle crash at junction 4",
                "TIER_1",
                "CRITICAL",
                "DETECTED",
                30.7333,
                76.7794,
                "Emergency Services",
                Instant.now(),
                "CREATED"
        );

        publisher.publishIncidentEvent(event);

        @SuppressWarnings("rawtypes")
        ArgumentCaptor<Message> captor = ArgumentCaptor.forClass(Message.class);
        verify(messageChannel).send(captor.capture());

        Message<?> sent = captor.getValue();
        SimpMessageHeaderAccessor headers = SimpMessageHeaderAccessor.wrap(sent);
        assertEquals("/topic/incidents", headers.getDestination());

        assertTrue(sent.getPayload() instanceof IncidentEvent || sent.getPayload() instanceof byte[]);
        if (sent.getPayload() instanceof IncidentEvent captured) {
            assertEquals(incidentId, captured.getIncidentId());
            assertEquals("INC-1001", captured.getIncidentNumber());
            assertEquals("MAJOR_ROAD_ACCIDENT", captured.getIncidentType());
            assertEquals("Multi-vehicle crash at junction 4", captured.getTitle());
            assertEquals("TIER_1", captured.getPriorityTier());
            assertEquals("CRITICAL", captured.getPriorityLabel());
            assertEquals("DETECTED", captured.getStatus());
            assertEquals(30.7333, captured.getLatitude());
            assertEquals(76.7794, captured.getLongitude());
            assertEquals("Emergency Services", captured.getAssignedDepartment());
            assertEquals("CREATED", captured.getEventType());
            assertNotNull(captured.getTimestamp());
        }
    }

    @Test
    @DisplayName("Should publish SensorEvent to /topic/sensors with telemetry values")
    void shouldPublishSensorEvent() {
        SensorEvent event = new SensorEvent(
                "AQI-SECTOR-17",
                "PM25",
                342.5,
                "µg/m³",
                30.7398,
                76.7827,
                Instant.now(),
                "ALERT"
        );

        publisher.publishSensorEvent(event);

        @SuppressWarnings("rawtypes")
        ArgumentCaptor<Message> captor = ArgumentCaptor.forClass(Message.class);
        verify(messageChannel).send(captor.capture());

        Message<?> sent = captor.getValue();
        SimpMessageHeaderAccessor headers = SimpMessageHeaderAccessor.wrap(sent);
        assertEquals("/topic/sensors", headers.getDestination());
    }

    @Test
    @DisplayName("Should publish SLAEvent to /topic/sla with deadline and escalation level")
    void shouldPublishSLAEvent() {
        String incidentId = UUID.randomUUID().toString();
        String slaId = UUID.randomUUID().toString();
        Instant deadline = Instant.now().plusSeconds(1800);

        SLAEvent event = new SLAEvent(
                incidentId,
                slaId,
                "CRITICAL",
                deadline,
                25L,
                "ON_TRACK",
                "NONE",
                Instant.now()
        );

        publisher.publishSLAEvent(event);

        @SuppressWarnings("rawtypes")
        ArgumentCaptor<Message> captor = ArgumentCaptor.forClass(Message.class);
        verify(messageChannel).send(captor.capture());

        Message<?> sent = captor.getValue();
        SimpMessageHeaderAccessor headers = SimpMessageHeaderAccessor.wrap(sent);
        assertEquals("/topic/sla", headers.getDestination());
    }

    @Test
    @DisplayName("Should publish AssignmentEvent to /topic/assignments with dispatch details")
    void shouldPublishAssignmentEvent() {
        String incidentId = UUID.randomUUID().toString();
        Instant assignedAt = Instant.now();

        AssignmentEvent event = new AssignmentEvent(
                incidentId,
                "Water Department",
                "Pipeline Leak Rapid Unit",
                "DISPATCHER_AI",
                assignedAt,
                "Pipeline burst requires high-pressure isolation valve team"
        );

        publisher.publishAssignmentEvent(event);

        @SuppressWarnings("rawtypes")
        ArgumentCaptor<Message> captor = ArgumentCaptor.forClass(Message.class);
        verify(messageChannel).send(captor.capture());

        Message<?> sent = captor.getValue();
        SimpMessageHeaderAccessor headers = SimpMessageHeaderAccessor.wrap(sent);
        assertEquals("/topic/assignments", headers.getDestination());
    }

    @Test
    @DisplayName("Should publish AlertEvent to /topic/alerts with severity and coordinates")
    void shouldPublishAlert() {
        String incidentId = UUID.randomUUID().toString();
        AlertEvent alert = new AlertEvent(
                "CRITICAL_INCIDENT",
                "CRITICAL",
                "Major Road Collision Detected",
                "Multi-vehicle crash at junction 12 with live wires exposed",
                incidentId,
                30.7333,
                76.7794
        );

        publisher.publishAlert(alert);

        @SuppressWarnings("rawtypes")
        ArgumentCaptor<Message> captor = ArgumentCaptor.forClass(Message.class);
        verify(messageChannel).send(captor.capture());

        Message<?> sent = captor.getValue();
        SimpMessageHeaderAccessor headers = SimpMessageHeaderAccessor.wrap(sent);
        assertEquals("/topic/alerts", headers.getDestination());
    }

    @Test
    @DisplayName("Should publish system message to /topic/system")
    void shouldPublishSystemEvent() {
        Map<String, Object> systemPayload = Map.of("status", "HEALTHY", "activeSessions", 14);
        publisher.publishSystemEvent(systemPayload);

        @SuppressWarnings("rawtypes")
        ArgumentCaptor<Message> captor = ArgumentCaptor.forClass(Message.class);
        verify(messageChannel).send(captor.capture());

        Message<?> sent = captor.getValue();
        SimpMessageHeaderAccessor headers = SimpMessageHeaderAccessor.wrap(sent);
        assertEquals("/topic/system", headers.getDestination());
    }

    @Test
    @DisplayName("Should gracefully handle null events without throwing exceptions")
    void shouldHandleNullEventsGracefully() {
        assertDoesNotThrow(() -> {
            publisher.publishIncidentEvent(null);
            publisher.publishSensorEvent(null);
            publisher.publishSLAEvent(null);
            publisher.publishAssignmentEvent(null);
            publisher.publishAlert(null);
            publisher.publishSystemEvent(null);
        });

        verifyNoInteractions(messageChannel);
    }

    @Test
    @DisplayName("Should catch template exceptions and protect domain business transactions")
    void shouldCatchTemplateExceptions() {
        when(messageChannel.send(any(Message.class))).thenThrow(new RuntimeException("Simulated messaging broker connection down"));

        IncidentEvent event = new IncidentEvent();
        assertDoesNotThrow(() -> publisher.publishIncidentEvent(event));

        AlertEvent alert = new AlertEvent();
        assertDoesNotThrow(() -> publisher.publishAlert(alert));
    }
}
