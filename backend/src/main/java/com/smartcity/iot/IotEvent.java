package com.smartcity.iot;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.UUID;

@Document(collection = "iot_events")
@CompoundIndex(name = "idx_iot_device_timestamp", def = "{'deviceId': 1, 'timestamp': -1}")
public class IotEvent {

    @Id
    private String id;

    @Indexed(unique = true)
    private String eventId;

    @Indexed
    private String deviceId;

    private String deviceType;

    @Indexed
    private String eventType; // THRESHOLD_BREACH, DEVICE_OFFLINE, DEVICE_ONLINE, SENSOR_FAULT

    private String message;
    private String payloadJson;
    private String severity; // INFO, WARNING, HIGH, CRITICAL

    @Indexed
    private String incidentId; // Associated Incident UUID if incident was created or updated

    private Boolean processed = false;

    @Indexed
    private Instant timestamp = Instant.now();

    public IotEvent() {
        this.eventId = "EVT-" + UUID.randomUUID().toString().substring(0, 12).toUpperCase();
        this.timestamp = Instant.now();
    }

    public IotEvent(String eventId, String deviceId, String deviceType, String eventType,
                    String payloadJson, String severity) {
        this.eventId = eventId != null ? eventId : "EVT-" + UUID.randomUUID().toString().substring(0, 12).toUpperCase();
        this.deviceId = deviceId;
        this.deviceType = deviceType;
        this.eventType = eventType;
        this.payloadJson = payloadJson;
        this.severity = severity;
        this.processed = false;
        this.timestamp = Instant.now();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getEventId() { return eventId; }
    public void setEventId(String eventId) { this.eventId = eventId; }

    @JsonIgnore
    public String getDeviceId() { return deviceId; }
    @JsonIgnore
    public void setDeviceId(String deviceId) { this.deviceId = deviceId; }

    public String getSensorId() { return deviceId; }
    public void setSensorId(String sensorId) { this.deviceId = sensorId; }

    @JsonIgnore
    public String getDeviceType() { return deviceType; }
    @JsonIgnore
    public void setDeviceType(String deviceType) { this.deviceType = deviceType; }

    public String getSensorType() { return deviceType; }
    public void setSensorType(String sensorType) { this.deviceType = sensorType; }

    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getPayloadJson() { return payloadJson; }
    public void setPayloadJson(String payloadJson) { this.payloadJson = payloadJson; }

    public String getSeverity() { return severity; }
    public void setSeverity(String severity) { this.severity = severity; }

    public String getIncidentId() { return incidentId; }
    public void setIncidentId(String incidentId) { this.incidentId = incidentId; }

    public Boolean getProcessed() { return processed; }
    public void setProcessed(Boolean processed) { this.processed = processed; }

    public Instant getTimestamp() { return timestamp; }
    public void setTimestamp(Instant timestamp) { this.timestamp = timestamp; }
}
