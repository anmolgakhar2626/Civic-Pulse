package com.smartcity.iot.simulator;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartcity.iot.MqttClientManager;
import com.smartcity.iot.dto.SensorDataMessage;
import com.smartcity.iot.dto.SensorStatusMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;

/**
 * Development-only sensor simulator allowing testing without physical ESP32/IoT hardware.
 */
@Component
public class IoTSensorSimulator {

    private static final Logger log = LoggerFactory.getLogger(IoTSensorSimulator.class);

    private final MqttClientManager mqttClientManager;
    private final ObjectMapper objectMapper;

    public IoTSensorSimulator(MqttClientManager mqttClientManager, ObjectMapper objectMapper) {
        this.mqttClientManager = mqttClientManager;
        this.objectMapper = objectMapper;
    }

    public void publishData(SensorDataMessage message) throws Exception {
        String topic = String.format("smartcity/sensors/%s/data", message.getSensorId());
        String payload = objectMapper.writeValueAsString(message);
        mqttClientManager.publish(topic, payload);
        log.info("Simulator published data to '{}': {}", topic, payload);
    }

    public void publishStatus(String sensorId, String status) throws Exception {
        SensorStatusMessage message = new SensorStatusMessage(sensorId, status);
        String topic = String.format("smartcity/sensors/%s/status", sensorId);
        String payload = objectMapper.writeValueAsString(message);
        mqttClientManager.publish(topic, payload);
        log.info("Simulator published status to '{}': {}", topic, payload);
    }

    public void publishPm25Spike(String sensorId, double value) throws Exception {
        SensorDataMessage msg = new SensorDataMessage(
                sensorId != null ? sensorId : "PM25-SECTOR17-001",
                "PM25",
                value > 0 ? value : 345.0,
                "µg/m³",
                30.7046,
                76.7179,
                "Sector 17 Central Commercial Plaza"
        );
        msg.setMetadata(Map.of("sensorModel", "Nova-SDS011", "simulated", true));
        publishData(msg);
    }

    public void publishWaterLevelSpike(String sensorId, double value) throws Exception {
        SensorDataMessage msg = new SensorDataMessage(
                sensorId != null ? sensorId : "WATER-UNDERPASS-004",
                "WATER_LEVEL",
                value > 0 ? value : 92.5,
                "%",
                30.7210,
                76.7415,
                "Railway Road Underpass Corridor"
        );
        msg.setMetadata(Map.of("sensorModel", "Ultrasonic-JSN-SR04T", "simulated", true));
        publishData(msg);
    }

    public void publishWasteOverflow(String sensorId, double value) throws Exception {
        SensorDataMessage msg = new SensorDataMessage(
                sensorId != null ? sensorId : "BIN-SECTOR22-042",
                "WASTE_LEVEL",
                value > 0 ? value : 94.0,
                "%",
                30.7125,
                76.7290,
                "Sector 22 Market Commercial Alley"
        );
        msg.setMetadata(Map.of("sensorModel", "LiDAR-Waste-Pro", "simulated", true));
        publishData(msg);
    }

    public void publishGasLeak(String sensorId, double value) throws Exception {
        SensorDataMessage msg = new SensorDataMessage(
                sensorId != null ? sensorId : "GAS-INDUSTRIAL-012",
                "GAS",
                value > 0 ? value : 185.0,
                "ppm",
                30.7155,
                76.7250,
                "Industrial Area Phase 1 Gas Distribution Pipeline"
        );
        msg.setMetadata(Map.of("sensorModel", "MQ-4-NaturalGas", "simulated", true));
        publishData(msg);
    }

    public void publishElectricalHazard(String sensorId, double value) throws Exception {
        SensorDataMessage msg = new SensorDataMessage(
                sensorId != null ? sensorId : "ELEC-GRID-007",
                "ELECTRICAL",
                value > 0 ? value : 1.0,
                "fault",
                30.7055,
                76.7155,
                "Substation 4 Feeder Live Junction"
        );
        msg.setMetadata(Map.of("sensorModel", "CurrentTransformer-CT100", "simulated", true));
        publishData(msg);
    }

    public void publishNormalReading(String sensorId, String sensorType, double value, String unit) throws Exception {
        SensorDataMessage msg = new SensorDataMessage(
                sensorId,
                sensorType,
                value,
                unit,
                30.7000,
                76.7100,
                "Ward 5 Municipal Station"
        );
        msg.setMetadata(Map.of("simulated", true));
        publishData(msg);
    }
}
