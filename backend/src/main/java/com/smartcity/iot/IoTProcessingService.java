package com.smartcity.iot;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartcity.assignment.AssignmentService;
import com.smartcity.incident.Incident;
import com.smartcity.incident.IncidentRepository;
import com.smartcity.incident.IncidentSource;
import com.smartcity.incident.IncidentStatus;
import com.smartcity.iot.dto.SensorDataMessage;
import com.smartcity.iot.dto.SensorStatusMessage;
import com.smartcity.priority.PriorityService;
import com.smartcity.sla.SLAService;
import com.smartcity.websocket.WebSocketEventPublisher;
import com.smartcity.websocket.dto.AlertEvent;
import com.smartcity.websocket.dto.SensorEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
public class IoTProcessingService {

    private static final Logger log = LoggerFactory.getLogger(IoTProcessingService.class);

    private static final Set<IncidentStatus> ACTIVE_STATUSES = Set.of(
            IncidentStatus.DETECTED,
            IncidentStatus.CLASSIFIED,
            IncidentStatus.PRIORITIZED,
            IncidentStatus.ASSIGNED,
            IncidentStatus.ACKNOWLEDGED,
            IncidentStatus.IN_PROGRESS,
            IncidentStatus.ESCALATED
    );

    private final SensorReadingRepository sensorReadingRepository;
    private final SensorDeviceRepository sensorDeviceRepository;
    private final IotEventRepository iotEventRepository;
    private final SensorThresholdService thresholdService;
    private final IncidentRepository incidentRepository;
    private final PriorityService priorityService;
    private final SLAService slaService;
    private final AssignmentService assignmentService;
    private final ObjectMapper objectMapper;

    @Autowired(required = false)
    @Lazy
    private WebSocketEventPublisher webSocketEventPublisher;

    public IoTProcessingService(
            SensorReadingRepository sensorReadingRepository,
            SensorDeviceRepository sensorDeviceRepository,
            IotEventRepository iotEventRepository,
            SensorThresholdService thresholdService,
            IncidentRepository incidentRepository,
            PriorityService priorityService,
            SLAService slaService,
            AssignmentService assignmentService,
            ObjectMapper objectMapper
    ) {
        this.sensorReadingRepository = sensorReadingRepository;
        this.sensorDeviceRepository = sensorDeviceRepository;
        this.iotEventRepository = iotEventRepository;
        this.thresholdService = thresholdService;
        this.incidentRepository = incidentRepository;
        this.priorityService = priorityService;
        this.slaService = slaService;
        this.assignmentService = assignmentService;
        this.objectMapper = objectMapper;
    }

    public void setWebSocketEventPublisher(WebSocketEventPublisher webSocketEventPublisher) {
        this.webSocketEventPublisher = webSocketEventPublisher;
    }

    /**
     * Ingestion pipeline:
     * MQTT message -> Validation -> SensorReading creation -> MongoDB persistence ->
     * Threshold/rule evaluation -> Potential Incident creation with deduplication
     */
    @Transactional
    public SensorReading processDataMessage(String topic, String payload) {
        SensorDataMessage data;
        try {
            data = objectMapper.readValue(payload, SensorDataMessage.class);
        } catch (Exception e) {
            log.warn("Invalid MQTT sensor data message on topic '{}': {} (payload: {})", topic, e.getMessage(), payload);
            recordInvalidMessageEvent(topic, payload, "DATA_PARSE_ERROR", e.getMessage());
            return null;
        }

        if (data.getSensorId() == null || data.getSensorId().isBlank() || data.getValue() == null) {
            log.warn("Sensor data message missing required sensorId or value on topic '{}'", topic);
            recordInvalidMessageEvent(topic, payload, "DATA_VALIDATION_ERROR", "Missing required sensorId or value");
            return null;
        }

        Instant now = data.getTimestamp() != null ? data.getTimestamp() : Instant.now();
        SensorType sensorType = SensorType.fromString(data.getSensorType());

        // 1. Create and persist SensorReading in MongoDB
        SensorReading reading = new SensorReading();
        reading.setSensorId(data.getSensorId().trim());
        reading.setSensorType(sensorType.name());
        reading.setValue(data.getValue());
        reading.setUnit(data.getUnit());
        reading.setLatitude(data.getLatitude());
        reading.setLongitude(data.getLongitude());
        reading.setLocation(data.getLocation());
        reading.setTimestamp(now);
        reading.setMetadata(data.getMetadata());
        reading.setStatus("NORMAL");

        // 2. Update/Upsert SensorDevice record in MongoDB
        updateSensorDeviceOnReading(data, sensorType, now);

        // 3. Threshold evaluation
        ThresholdEvaluationResult evalResult = thresholdService.evaluate(reading);

        if (evalResult.isBreachDetected()) {
            reading.setStatus("ALERT");
            log.warn("IoT Threshold breach detected for sensor '{}': rule={}, type={}, val={}{}",
                    data.getSensorId(), evalResult.getRuleName(), evalResult.getIncidentType(), data.getValue(), data.getUnit());

            // 4. Duplicate prevention: check for active incident
            Optional<Incident> activeIncidentOpt = findActiveIncidentForSensor(data.getSensorId(), evalResult.getIncidentType());

            if (activeIncidentOpt.isPresent()) {
                Incident activeIncident = activeIncidentOpt.get();
                log.info("Deduplication matched active incident {} for sensor {}. Skipping new incident creation.",
                        activeIncident.getIncidentNumber(), data.getSensorId());

                // Update existing incident
                activeIncident.setUpdatedAt(Instant.now());
                incidentRepository.saveAndFlush(activeIncident);

                // Record IotEvent linked to existing incident
                IotEvent event = new IotEvent();
                event.setSensorId(data.getSensorId());
                event.setSensorType(sensorType.name());
                event.setEventType("THRESHOLD_BREACH_CONTINUED");
                event.setSeverity(evalResult.getSeverity() != null ? evalResult.getSeverity().name() : "WARNING");
                event.setMessage(String.format("Ongoing alert: %s (value=%.1f %s) on active incident %s",
                        evalResult.getTitle(), data.getValue(), data.getUnit(), activeIncident.getIncidentNumber()));
                event.setIncidentId(activeIncident.getId().toString());
                event.setPayloadJson(payload);
                event.setProcessed(true);
                iotEventRepository.save(event);
                if (webSocketEventPublisher != null) {
                    AlertEvent alert = new AlertEvent(
                            "SENSOR_THRESHOLD",
                            evalResult.getSeverity() != null ? evalResult.getSeverity().name() : "HIGH",
                            evalResult.getTitle() != null ? evalResult.getTitle() : "Sensor Threshold Breach",
                            String.format("Ongoing alert: %s (value=%.1f %s) on active incident %s",
                                    evalResult.getTitle(), data.getValue(), data.getUnit(), activeIncident.getIncidentNumber()),
                            activeIncident.getId().toString(),
                            data.getLatitude(),
                            data.getLongitude()
                    );
                    webSocketEventPublisher.publishAlert(alert);
                }
            } else {
                // Create new incident
                Incident newIncident = createIncidentFromBreach(data, evalResult, now);
                log.info("Created new incident {} from IoT sensor {}", newIncident.getIncidentNumber(), data.getSensorId());

                // Record IotEvent linked to new incident
                IotEvent event = new IotEvent();
                event.setSensorId(data.getSensorId());
                event.setSensorType(sensorType.name());
                event.setEventType("THRESHOLD_BREACH_NEW_INCIDENT");
                event.setSeverity(evalResult.getSeverity() != null ? evalResult.getSeverity().name() : "CRITICAL");
                event.setMessage(String.format("New incident created: %s (value=%.1f %s)",
                        evalResult.getTitle(), data.getValue(), data.getUnit()));
                event.setIncidentId(newIncident.getId().toString());
                event.setPayloadJson(payload);
                event.setProcessed(true);
                iotEventRepository.save(event);

                if (webSocketEventPublisher != null) {
                    AlertEvent alert = new AlertEvent(
                            "SENSOR_THRESHOLD",
                            evalResult.getSeverity() != null ? evalResult.getSeverity().name() : "HIGH",
                            evalResult.getTitle() != null ? evalResult.getTitle() : "Sensor Threshold Breach",
                            String.format("New incident created: %s (value=%.1f %s)",
                                    evalResult.getTitle(), data.getValue(), data.getUnit()),
                            newIncident.getId() != null ? newIncident.getId().toString() : null,
                            data.getLatitude(),
                            data.getLongitude()
                    );
                    webSocketEventPublisher.publishAlert(alert);
                }
            }
        }

        SensorReading savedReading = sensorReadingRepository.save(reading);

        // Real-time sensor reading broadcast
        if (webSocketEventPublisher != null) {
            SensorEvent sensorEvent = new SensorEvent(
                    savedReading.getSensorId(),
                    savedReading.getSensorType(),
                    savedReading.getValue(),
                    savedReading.getUnit(),
                    savedReading.getLatitude(),
                    savedReading.getLongitude(),
                    savedReading.getTimestamp(),
                    savedReading.getStatus()
            );
            webSocketEventPublisher.publishSensorEvent(sensorEvent);
        }

        return savedReading;
    }

    /**
     * Process sensor status heartbeat/status topic message
     */
    public SensorDevice processStatusMessage(String topic, String payload) {
        SensorStatusMessage statusMsg;
        try {
            statusMsg = objectMapper.readValue(payload, SensorStatusMessage.class);
        } catch (Exception e) {
            log.warn("Invalid MQTT sensor status message on topic '{}': {}", topic, e.getMessage());
            recordInvalidMessageEvent(topic, payload, "STATUS_PARSE_ERROR", e.getMessage());
            return null;
        }

        if (statusMsg.getSensorId() == null || statusMsg.getSensorId().isBlank()) {
            return null;
        }

        String sensorId = statusMsg.getSensorId().trim();
        SensorStatus status = SensorStatus.fromString(statusMsg.getStatus());
        Instant now = statusMsg.getTimestamp() != null ? statusMsg.getTimestamp() : Instant.now();

        SensorDevice device = sensorDeviceRepository.findBySensorId(sensorId)
                .orElseGet(() -> new SensorDevice(sensorId, SensorType.AIR_QUALITY, "Municipal Sector", null, null));

        device.setStatus(status);
        device.setLastSeenAt(now);
        SensorDevice saved = sensorDeviceRepository.save(device);

        IotEvent event = new IotEvent();
        event.setSensorId(sensorId);
        event.setSensorType(device.getSensorType() != null ? device.getSensorType().name() : "UNKNOWN");
        event.setEventType("DEVICE_" + status.name());
        event.setSeverity(status == SensorStatus.ONLINE ? "INFO" : "WARNING");
        event.setMessage(String.format("Sensor %s reported status %s", sensorId, status.name()));
        event.setPayloadJson(payload);
        event.setProcessed(true);
        iotEventRepository.save(event);

        log.info("Sensor '{}' status updated to {}", sensorId, status.name());
        return saved;
    }

    private void updateSensorDeviceOnReading(SensorDataMessage data, SensorType sensorType, Instant timestamp) {
        SensorDevice device = sensorDeviceRepository.findBySensorId(data.getSensorId().trim())
                .orElseGet(() -> new SensorDevice(data.getSensorId().trim(), sensorType, data.getLocation(), data.getLatitude(), data.getLongitude()));

        device.setSensorType(sensorType);
        device.setStatus(SensorStatus.ONLINE);
        device.setLastSeenAt(timestamp);
        device.setLastValue(data.getValue());
        device.setUnit(data.getUnit());
        if (data.getLocation() != null && !data.getLocation().isBlank()) {
            device.setLocation(data.getLocation());
        }
        if (data.getLatitude() != null) {
            device.setLatitude(data.getLatitude());
        }
        if (data.getLongitude() != null) {
            device.setLongitude(data.getLongitude());
        }
        sensorDeviceRepository.save(device);
    }

    private Optional<Incident> findActiveIncidentForSensor(String sensorId, String incidentType) {
        List<Incident> activeIncidents = incidentRepository.findByIncidentTypeAndStatusIn(incidentType, ACTIVE_STATUSES);
        String sensorTag = "[Sensor: " + sensorId + "]";
        for (Incident inc : activeIncidents) {
            if (inc.getDescription() != null && inc.getDescription().contains(sensorTag)) {
                return Optional.of(inc);
            }
        }
        return Optional.empty();
    }

    private Incident createIncidentFromBreach(SensorDataMessage data, ThresholdEvaluationResult result, Instant timestamp) {
        Incident incident = new Incident();
        incident.setIncidentNumber("INC-IOT-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        incident.setIncidentType(result.getIncidentType());
        incident.setDescription(result.getDescription());
        incident.setSource(IncidentSource.IOT);
        incident.setLatitude(data.getLatitude() != null ? data.getLatitude() : 0.0);
        incident.setLongitude(data.getLongitude() != null ? data.getLongitude() : 0.0);
        incident.setAddress(data.getLocation() != null ? data.getLocation() : "IoT Sensor Telemetry Location");
        incident.setStatus(IncidentStatus.DETECTED);
        incident.setReportedAt(timestamp);
        incident.setDetectedAt(timestamp);

        if (result.getSeverity() != null) {
            incident.setSeverity(result.getSeverity());
            incident.setSeverityIndicator(result.getSeverity().name());
        }

        // Save new incident initially and flush immediately so downstream JDBC services can reference it
        Incident savedIncident = incidentRepository.saveAndFlush(incident);

        if (webSocketEventPublisher != null) {
            com.smartcity.websocket.dto.IncidentEvent incEvent = new com.smartcity.websocket.dto.IncidentEvent(
                    savedIncident.getId() != null ? savedIncident.getId().toString() : null,
                    savedIncident.getIncidentNumber(),
                    savedIncident.getIncidentType(),
                    savedIncident.getDescription(),
                    savedIncident.getPriorityTier() != null ? savedIncident.getPriorityTier().name() : null,
                    savedIncident.getPriorityLabel(),
                    savedIncident.getStatus() != null ? savedIncident.getStatus().name() : null,
                    savedIncident.getLatitude(),
                    savedIncident.getLongitude(),
                    savedIncident.getAssignedDepartment() != null ? savedIncident.getAssignedDepartment().getName() : null,
                    savedIncident.getCreatedAt() != null ? savedIncident.getCreatedAt() : Instant.now(),
                    "CREATED"
            );
            webSocketEventPublisher.publishIncidentEvent(incEvent);
        }

        // 1. Trigger Priority Engine
        try {
            priorityService.evaluateIncidentPriority(savedIncident.getId());
        } catch (Exception e) {
            log.error("Failed to evaluate priority for IoT incident {}: {}", savedIncident.getIncidentNumber(), e.getMessage());
        }

        // 2. Trigger SLA Engine
        try {
            slaService.createSLAForIncident(savedIncident.getId());
        } catch (Exception e) {
            log.error("Failed to establish SLA for IoT incident {}: {}", savedIncident.getIncidentNumber(), e.getMessage());
        }

        // 3. Trigger Department Assignment Workflow
        try {
            assignmentService.autoAssign(savedIncident.getId().toString());
        } catch (Exception e) {
            log.error("Failed to auto-assign department for IoT incident {}: {}", savedIncident.getIncidentNumber(), e.getMessage());
        }

        return incidentRepository.findById(savedIncident.getId()).orElse(savedIncident);
    }

    private void recordInvalidMessageEvent(String topic, String payload, String eventType, String error) {
        IotEvent event = new IotEvent();
        event.setSensorId("UNKNOWN");
        event.setSensorType("UNKNOWN");
        event.setEventType(eventType);
        event.setSeverity("WARNING");
        event.setMessage("Invalid message on topic '" + topic + "': " + error);
        event.setPayloadJson(payload);
        event.setProcessed(false);
        iotEventRepository.save(event);
    }
}
