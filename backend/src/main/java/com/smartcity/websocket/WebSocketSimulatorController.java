package com.smartcity.websocket;

import com.smartcity.websocket.dto.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.UUID;

/**
 * Development testing and verification endpoint for WebSocket STOMP event publishing.
 */
@RestController
@RequestMapping("/api/websocket")
@Tag(name = "WebSocket Testing", description = "Development triggers to verify real-time STOMP event broadcasts")
@SecurityRequirement(name = "BearerAuth")
public class WebSocketSimulatorController {

    private final WebSocketEventPublisher publisher;

    public WebSocketSimulatorController(WebSocketEventPublisher publisher) {
        this.publisher = publisher;
    }

    @PostMapping("/trigger/incident")
    @Operation(summary = "Publish test incident event", description = "Publishes simulated incident lifecycle event to /topic/incidents")
    public ResponseEntity<Map<String, Object>> triggerIncidentEvent(@RequestBody(required = false) Map<String, Object> payload) {
        String incidentId = payload != null && payload.containsKey("incidentId") ? String.valueOf(payload.get("incidentId")) : UUID.randomUUID().toString();
        String incidentNumber = payload != null && payload.containsKey("incidentNumber") ? String.valueOf(payload.get("incidentNumber")) : "INC-SIM-" + incidentId.substring(0, 8).toUpperCase();
        String incidentType = payload != null && payload.containsKey("incidentType") ? String.valueOf(payload.get("incidentType")) : "MAJOR_ROAD_ACCIDENT";
        String status = payload != null && payload.containsKey("status") ? String.valueOf(payload.get("status")) : "DETECTED";
        String priorityTier = payload != null && payload.containsKey("priorityTier") ? String.valueOf(payload.get("priorityTier")) : "CRITICAL";

        IncidentEvent event = new IncidentEvent(
                incidentId,
                incidentNumber,
                incidentType,
                "Simulated Test Incident: " + incidentType,
                priorityTier,
                priorityTier,
                status,
                30.7333,
                76.7794,
                "Emergency Services",
                Instant.now(),
                "CREATED"
        );

        publisher.publishIncidentEvent(event);
        return ResponseEntity.ok(Map.of("success", true, "topic", "/topic/incidents", "event", event));
    }

    @PostMapping("/trigger/pm25-spike")
    @Operation(summary = "Publish test sensor breach & alert", description = "Publishes sensor event to /topic/sensors and alert to /topic/alerts")
    public ResponseEntity<Map<String, Object>> triggerPm25Spike(@RequestBody(required = false) Map<String, Object> payload) {
        double value = payload != null && payload.containsKey("value") ? Double.parseDouble(String.valueOf(payload.get("value"))) : 345.0;
        String sensorId = payload != null && payload.containsKey("sensorId") ? String.valueOf(payload.get("sensorId")) : "PM25-TEST-SIM";

        SensorEvent sensorEvent = new SensorEvent(
                sensorId,
                "PM25",
                value,
                "µg/m³",
                30.7046,
                76.7179,
                Instant.now(),
                value > 300 ? "ALERT" : "NORMAL"
        );
        publisher.publishSensorEvent(sensorEvent);

        AlertEvent alertEvent = null;
        if (value > 300) {
            alertEvent = new AlertEvent(
                    "SENSOR_THRESHOLD",
                    "CRITICAL",
                    "Severe Smog Threshold Breach (PM2.5 > 300)",
                    String.format("Sensor %s recorded PM2.5 level at %.1f µg/m³. Severe public health hazard.", sensorId, value),
                    null,
                    30.7046,
                    76.7179
            );
            publisher.publishAlert(alertEvent);
        }

        return ResponseEntity.ok(Map.of(
                "success", true,
                "sensorTopic", "/topic/sensors",
                "alertTopic", value > 300 ? "/topic/alerts" : "none",
                "sensorEvent", sensorEvent,
                "alertEvent", alertEvent != null ? alertEvent : "No breach threshold crossed"
        ));
    }

    @PostMapping("/trigger/sla-at-risk")
    @Operation(summary = "Publish test SLA AT_RISK event", description = "Publishes SLA event to /topic/sla and alert to /topic/alerts")
    public ResponseEntity<Map<String, Object>> triggerSlaAtRisk(@RequestBody(required = false) Map<String, Object> payload) {
        String incidentId = payload != null && payload.containsKey("incidentId") ? String.valueOf(payload.get("incidentId")) : UUID.randomUUID().toString();
        String slaId = UUID.randomUUID().toString();

        SLAEvent slaEvent = new SLAEvent(
                incidentId,
                slaId,
                "HIGH",
                Instant.now().plus(15, ChronoUnit.MINUTES),
                15L,
                "AT_RISK",
                "LEVEL_1",
                Instant.now()
        );
        publisher.publishSLAEvent(slaEvent);

        AlertEvent alertEvent = new AlertEvent(
                "SLA_AT_RISK",
                "HIGH",
                "Incident SLA Approaching Expiry",
                String.format("Incident %s has 15 minutes remaining before target SLA breach.", incidentId),
                incidentId,
                30.7333,
                76.7794
        );
        publisher.publishAlert(alertEvent);

        return ResponseEntity.ok(Map.of(
                "success", true,
                "slaTopic", "/topic/sla",
                "alertTopic", "/topic/alerts",
                "slaEvent", slaEvent,
                "alertEvent", alertEvent
        ));
    }

    @PostMapping("/trigger/assignment")
    @Operation(summary = "Publish test assignment event", description = "Publishes assignment event to /topic/assignments and /topic/incidents")
    public ResponseEntity<Map<String, Object>> triggerAssignment(@RequestBody(required = false) Map<String, Object> payload) {
        String incidentId = payload != null && payload.containsKey("incidentId") ? String.valueOf(payload.get("incidentId")) : UUID.randomUUID().toString();
        String department = payload != null && payload.containsKey("department") ? String.valueOf(payload.get("department")) : "Emergency Services";
        String team = payload != null && payload.containsKey("team") ? String.valueOf(payload.get("team")) : "Emergency Rapid Response Team";

        AssignmentEvent assignmentEvent = new AssignmentEvent(
                incidentId,
                department,
                team,
                "AUTOMATIC_DISPATCHER",
                Instant.now(),
                "Critical road hazard requires immediate rapid containment crew"
        );
        publisher.publishAssignmentEvent(assignmentEvent);

        IncidentEvent incidentEvent = new IncidentEvent(
                incidentId,
                "INC-SIM-" + incidentId.substring(0, 8).toUpperCase(),
                "MAJOR_ROAD_ACCIDENT",
                "Hazardous roadway collision requiring immediate unit response",
                "CRITICAL",
                "CRITICAL",
                "ASSIGNED",
                30.7333,
                76.7794,
                department,
                Instant.now(),
                "ASSIGNED"
        );
        publisher.publishIncidentEvent(incidentEvent);

        return ResponseEntity.ok(Map.of(
                "success", true,
                "assignmentTopic", "/topic/assignments",
                "incidentTopic", "/topic/incidents",
                "assignmentEvent", assignmentEvent,
                "incidentEvent", incidentEvent
        ));
    }

    @PostMapping("/trigger/alert")
    @Operation(summary = "Publish arbitrary test alert", description = "Publishes alert to /topic/alerts")
    public ResponseEntity<Map<String, Object>> triggerAlert(@RequestBody(required = false) Map<String, Object> payload) {
        String type = payload != null && payload.containsKey("type") ? String.valueOf(payload.get("type")) : "CRITICAL_INCIDENT";
        String severity = payload != null && payload.containsKey("severity") ? String.valueOf(payload.get("severity")) : "CRITICAL";
        String title = payload != null && payload.containsKey("title") ? String.valueOf(payload.get("title")) : "Critical Incident Alert";
        String message = payload != null && payload.containsKey("message") ? String.valueOf(payload.get("message")) : "A critical municipal condition has been detected.";

        AlertEvent alert = new AlertEvent(type, severity, title, message, null, 30.7333, 76.7794);
        publisher.publishAlert(alert);

        return ResponseEntity.ok(Map.of("success", true, "topic", "/topic/alerts", "alert", alert));
    }
}
