package com.smartcity.iot;

import com.smartcity.common.PagedResponse;
import com.smartcity.common.ResourceNotFoundException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/iot")
@Tag(name = "IoT Ingestion", description = "Municipal IoT sensor telemetry, health status, and event ingestion APIs")
@SecurityRequirement(name = "BearerAuth")
public class IoTController {

    private final SensorDeviceRepository sensorDeviceRepository;
    private final SensorReadingRepository sensorReadingRepository;
    private final IotEventRepository iotEventRepository;

    public IoTController(
            SensorDeviceRepository sensorDeviceRepository,
            SensorReadingRepository sensorReadingRepository,
            IotEventRepository iotEventRepository
    ) {
        this.sensorDeviceRepository = sensorDeviceRepository;
        this.sensorReadingRepository = sensorReadingRepository;
        this.iotEventRepository = iotEventRepository;
    }

    @GetMapping("/sensors")
    @Operation(summary = "List registered IoT sensor devices",
            description = "Returns paginated list of IoT sensor devices with optional filtering by sensorType and status.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Sensors retrieved successfully")
    })
    public ResponseEntity<PagedResponse<SensorDevice>> getSensors(
            @Parameter(description = "Filter by sensor type (optional)")
            @RequestParam(required = false) String sensorType,
            @Parameter(description = "Filter by sensor status: ONLINE, OFFLINE, etc. (optional)")
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Pageable pageable = PageRequest.of(Math.max(0, page), Math.max(1, size));
        Page<SensorDevice> resultPage;

        if (sensorType != null && !sensorType.isBlank()) {
            SensorType type = SensorType.fromString(sensorType);
            resultPage = sensorDeviceRepository.findBySensorType(type, pageable);
        } else if (status != null && !status.isBlank()) {
            SensorStatus s = SensorStatus.fromString(status);
            resultPage = sensorDeviceRepository.findByStatus(s, pageable);
        } else {
            resultPage = sensorDeviceRepository.findAll(pageable);
        }

        return ResponseEntity.ok(PagedResponse.of(resultPage));
    }

    @GetMapping("/sensors/{sensorId}")
    @Operation(summary = "Get sensor device details", description = "Retrieves sensor device metadata, coordinates, and status by sensorId.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Sensor device found"),
            @ApiResponse(responseCode = "404", description = "Sensor device not found")
    })
    public ResponseEntity<SensorDevice> getSensorById(@PathVariable String sensorId) {
        SensorDevice device = sensorDeviceRepository.findBySensorId(sensorId.trim())
                .orElseThrow(() -> new ResourceNotFoundException("Sensor device", "sensorId", sensorId));
        return ResponseEntity.ok(device);
    }

    @GetMapping("/sensors/{sensorId}/readings")
    @Operation(summary = "Get telemetry readings for a specific sensor", description = "Returns paginated time-series telemetry readings for the specified sensor.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Readings retrieved successfully")
    })
    public ResponseEntity<PagedResponse<SensorReading>> getSensorReadings(
            @PathVariable String sensorId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Pageable pageable = PageRequest.of(Math.max(0, page), Math.max(1, size));
        Page<SensorReading> readings = sensorReadingRepository.findBySensorIdOrderByTimestampDesc(sensorId.trim(), pageable);
        return ResponseEntity.ok(PagedResponse.of(readings));
    }

    @GetMapping("/readings")
    @Operation(summary = "List all sensor telemetry readings", description = "Returns paginated list of sensor telemetry readings with optional sensorType filter.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Readings retrieved successfully")
    })
    public ResponseEntity<PagedResponse<SensorReading>> getAllReadings(
            @Parameter(description = "Filter by sensor type (optional)")
            @RequestParam(required = false) String sensorType,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Pageable pageable = PageRequest.of(Math.max(0, page), Math.max(1, size));
        Page<SensorReading> readings;

        if (sensorType != null && !sensorType.isBlank()) {
            readings = sensorReadingRepository.findBySensorTypeOrderByTimestampDesc(sensorType.trim().toUpperCase(), pageable);
        } else {
            readings = sensorReadingRepository.findAllByOrderByTimestampDesc(pageable);
        }

        return ResponseEntity.ok(PagedResponse.of(readings));
    }

    @GetMapping("/events")
    @Operation(summary = "List IoT events and threshold breach logs", description = "Returns paginated list of IoT events with optional deviceId or severity filter.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Events retrieved successfully")
    })
    public ResponseEntity<PagedResponse<IotEvent>> getEvents(
            @Parameter(description = "Filter by sensor / device ID (optional)")
            @RequestParam(required = false) String sensorId,
            @Parameter(description = "Filter by severity: INFO, WARNING, CRITICAL (optional)")
            @RequestParam(required = false) String severity,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Pageable pageable = PageRequest.of(Math.max(0, page), Math.max(1, size));
        Page<IotEvent> events;

        if (sensorId != null && !sensorId.isBlank()) {
            events = iotEventRepository.findByDeviceIdOrderByTimestampDesc(sensorId.trim(), pageable);
        } else if (severity != null && !severity.isBlank()) {
            events = iotEventRepository.findBySeverityOrderByTimestampDesc(severity.trim().toUpperCase(), pageable);
        } else {
            events = iotEventRepository.findAllByOrderByTimestampDesc(pageable);
        }

        return ResponseEntity.ok(PagedResponse.of(events));
    }
}
