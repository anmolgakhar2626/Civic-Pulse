package com.smartcity.websocket;

import com.smartcity.websocket.dto.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;

/**
 * Central event publishing layer decoupling municipal domain services from WebSocket mechanics.
 * Broadcasts frontend-safe DTOs over configured STOMP topics.
 */
@Service
public class WebSocketEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(WebSocketEventPublisher.class);

    public static final String TOPIC_INCIDENTS = "/topic/incidents";
    public static final String TOPIC_SENSORS = "/topic/sensors";
    public static final String TOPIC_SLA = "/topic/sla";
    public static final String TOPIC_ALERTS = "/topic/alerts";
    public static final String TOPIC_ASSIGNMENTS = "/topic/assignments";
    public static final String TOPIC_SYSTEM = "/topic/system";
    public static final String TOPIC_NOTIFICATIONS = "/topic/notifications";

    private final SimpMessagingTemplate messagingTemplate;

    public WebSocketEventPublisher(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    /**
     * Publishes incident lifecycle events to /topic/incidents.
     */
    public void publishIncidentEvent(IncidentEvent event) {
        if (event == null) return;
        try {
            if (event.getTimestamp() == null) {
                event.setTimestamp(Instant.now());
            }
            messagingTemplate.convertAndSend(TOPIC_INCIDENTS, event);
            log.debug("Published IncidentEvent to {}: id={}, type={}, status={}",
                    TOPIC_INCIDENTS, event.getIncidentId(), event.getEventType(), event.getStatus());
        } catch (Exception e) {
            log.warn("Failed to publish IncidentEvent to {}: {}", TOPIC_INCIDENTS, e.getMessage());
        }
    }

    /**
     * Publishes telemetry sensor readings to /topic/sensors.
     */
    public void publishSensorEvent(SensorEvent event) {
        if (event == null) return;
        try {
            if (event.getTimestamp() == null) {
                event.setTimestamp(Instant.now());
            }
            messagingTemplate.convertAndSend(TOPIC_SENSORS, event);
            log.debug("Published SensorEvent to {}: sensorId={}, value={}{}",
                    TOPIC_SENSORS, event.getSensorId(), event.getValue(), event.getUnit());
        } catch (Exception e) {
            log.warn("Failed to publish SensorEvent to {}: {}", TOPIC_SENSORS, e.getMessage());
        }
    }

    /**
     * Publishes SLA transition and deadline events to /topic/sla.
     */
    public void publishSLAEvent(SLAEvent event) {
        if (event == null) return;
        try {
            if (event.getTimestamp() == null) {
                event.setTimestamp(Instant.now());
            }
            messagingTemplate.convertAndSend(TOPIC_SLA, event);
            log.debug("Published SLAEvent to {}: incidentId={}, slaStatus={}, remaining={}m",
                    TOPIC_SLA, event.getIncidentId(), event.getSlaStatus(), event.getRemainingMinutes());
        } catch (Exception e) {
            log.warn("Failed to publish SLAEvent to {}: {}", TOPIC_SLA, e.getMessage());
        }
    }

    /**
     * Publishes department/team assignment events to /topic/assignments.
     */
    public void publishAssignmentEvent(AssignmentEvent event) {
        if (event == null) return;
        try {
            if (event.getAssignedAt() == null) {
                event.setAssignedAt(Instant.now());
            }
            messagingTemplate.convertAndSend(TOPIC_ASSIGNMENTS, event);
            log.debug("Published AssignmentEvent to {}: incidentId={}, dept={}, team={}",
                    TOPIC_ASSIGNMENTS, event.getIncidentId(), event.getDepartment(), event.getTeam());
        } catch (Exception e) {
            log.warn("Failed to publish AssignmentEvent to {}: {}", TOPIC_ASSIGNMENTS, e.getMessage());
        }
    }

    /**
     * Publishes high-priority alerts to /topic/alerts.
     */
    public void publishAlert(AlertEvent alert) {
        if (alert == null) return;
        try {
            if (alert.getTimestamp() == null) {
                alert.setTimestamp(Instant.now());
            }
            messagingTemplate.convertAndSend(TOPIC_ALERTS, alert);
            log.info("Published AlertEvent to {}: type={}, severity={}, title='{}'",
                    TOPIC_ALERTS, alert.getType(), alert.getSeverity(), alert.getTitle());
        } catch (Exception e) {
            log.warn("Failed to publish AlertEvent to {}: {}", TOPIC_ALERTS, e.getMessage());
        }
    }

    /**
     * Publishes notification to general /topic/notifications.
     */
    public void publishNotification(Object payload) {
        if (payload == null) return;
        try {
            messagingTemplate.convertAndSend(TOPIC_NOTIFICATIONS, payload);
            log.debug("Published notification to {}", TOPIC_NOTIFICATIONS);
        } catch (Exception e) {
            log.warn("Failed to publish notification to {}: {}", TOPIC_NOTIFICATIONS, e.getMessage());
        }
    }

    /**
     * Publishes direct notification to specific user at /topic/notifications/{userId}.
     */
    public void publishNotificationToUser(String userId, Object payload) {
        if (userId == null || payload == null) return;
        String userTopic = TOPIC_NOTIFICATIONS + "/" + userId;
        try {
            messagingTemplate.convertAndSend(userTopic, payload);
            log.debug("Published user-specific notification to {}", userTopic);
        } catch (Exception e) {
            log.warn("Failed to publish user notification to {}: {}", userTopic, e.getMessage());
        }
    }

    /**
     * Publishes system-wide health and administrative events to /topic/system.
     */
    public void publishSystemEvent(Object payload) {
        if (payload == null) return;
        try {
            messagingTemplate.convertAndSend(TOPIC_SYSTEM, payload);
            log.debug("Published system event to {}", TOPIC_SYSTEM);
        } catch (Exception e) {
            log.warn("Failed to publish system event to {}: {}", TOPIC_SYSTEM, e.getMessage());
        }
    }
}
