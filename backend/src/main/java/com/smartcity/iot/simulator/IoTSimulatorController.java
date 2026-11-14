package com.smartcity.iot.simulator;

import com.smartcity.iot.dto.SensorDataMessage;
import com.smartcity.iot.dto.SensorStatusMessage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/iot/simulator")
@Tag(name = "IoT Simulator", description = "Development simulator APIs to publish MQTT sensor telemetry without physical hardware")
@SecurityRequirement(name = "BearerAuth")
public class IoTSimulatorController {

    private final IoTSensorSimulator simulator;

    public IoTSimulatorController(IoTSensorSimulator simulator) {
        this.simulator = simulator;
    }

    @PostMapping("/publish")
    @Operation(summary = "Publish custom sensor telemetry via MQTT", description = "Publishes a custom JSON sensor data payload to its MQTT topic.")
    public ResponseEntity<Map<String, Object>> publishData(@Valid @RequestBody SensorDataMessage message) throws Exception {
        simulator.publishData(message);
        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Sensor data published to topic smartcity/sensors/" + message.getSensorId() + "/data",
                "sensorId", message.getSensorId()
        ));
    }

    @PostMapping("/status")
    @Operation(summary = "Publish sensor status heartbeat via MQTT", description = "Publishes an ONLINE / OFFLINE heartbeat to smartcity/sensors/{sensorId}/status.")
    public ResponseEntity<Map<String, Object>> publishStatus(@Valid @RequestBody SensorStatusMessage message) throws Exception {
        simulator.publishStatus(message.getSensorId(), message.getStatus());
        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Sensor status published to topic smartcity/sensors/" + message.getSensorId() + "/status",
                "status", message.getStatus()
        ));
    }

    @PostMapping("/trigger/{scenario}")
    @Operation(summary = "Trigger predefined sensor hazard scenario",
            description = "Triggers: 'pm25-spike', 'water-spike', 'waste-overflow', 'gas-leak', 'electrical-hazard', 'normal'")
    public ResponseEntity<Map<String, Object>> triggerScenario(
            @Parameter(description = "Scenario name") @PathVariable String scenario,
            @RequestParam(required = false) String sensorId,
            @RequestParam(required = false, defaultValue = "0") double value
    ) throws Exception {
        String cleanScenario = scenario.toLowerCase().trim();
        switch (cleanScenario) {
            case "pm25-spike", "pm25" -> simulator.publishPm25Spike(sensorId, value > 0 ? value : 345.0);
            case "water-spike", "water", "flood" -> simulator.publishWaterLevelSpike(sensorId, value > 0 ? value : 92.5);
            case "waste-overflow", "waste" -> simulator.publishWasteOverflow(sensorId, value > 0 ? value : 94.0);
            case "gas-leak", "gas" -> simulator.publishGasLeak(sensorId, value > 0 ? value : 185.0);
            case "electrical-hazard", "electrical" -> simulator.publishElectricalHazard(sensorId, value > 0 ? value : 1.0);
            case "normal" -> simulator.publishNormalReading(
                    sensorId != null ? sensorId : "NORM-001",
                    "PM25",
                    value > 0 ? value : 28.0,
                    "µg/m³"
            );
            default -> {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "error", "Unknown scenario: " + scenario + ". Valid options: pm25-spike, water-spike, waste-overflow, gas-leak, electrical-hazard, normal"
                ));
            }
        }

        return ResponseEntity.ok(Map.of(
                "success", true,
                "scenario", cleanScenario,
                "message", "Scenario triggered and MQTT message published"
        ));
    }
}
