package com.smartcity.iot;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartcity.assignment.AssignmentService;
import com.smartcity.department.Department;
import com.smartcity.department.DepartmentRepository;
import com.smartcity.department.Team;
import com.smartcity.department.TeamRepository;
import com.smartcity.incident.Incident;
import com.smartcity.incident.IncidentRepository;
import com.smartcity.incident.IncidentSource;
import com.smartcity.incident.IncidentStatus;
import com.smartcity.iot.dto.SensorDataMessage;
import com.smartcity.iot.dto.SensorStatusMessage;
import com.smartcity.priority.PriorityService;
import com.smartcity.sla.SLAService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@WithMockUser(username = "admin", roles = {"ADMIN", "COMMAND_OPERATOR"})
@DisplayName("IoT MQTT Sensor Ingestion, Threshold Rules, Deduplication, and Incident Creation Integration Tests")
class IoTIngestionIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private IoTProcessingService ioTProcessingService;

    @Autowired
    private SensorReadingRepository sensorReadingRepository;

    @Autowired
    private SensorDeviceRepository sensorDeviceRepository;

    @Autowired
    private IotEventRepository iotEventRepository;

    @Autowired
    private IncidentRepository incidentRepository;

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private TeamRepository teamRepository;

    @Autowired
    private PriorityService priorityService;

    @Autowired
    private SLAService slaService;

    @Autowired
    private AssignmentService assignmentService;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        // Core departments (ENVIRONMENT, DISASTER_MGMT, SANITATION, EMERGENCY)
        // are already seeded by Flyway migration V5.
    }

    @Test
    @DisplayName("Valid MQTT sensor data message is parsed, stored in MongoDB, and updates device status")
    void testProcessValidSensorDataMessageAndPersist() throws Exception {
        SensorDataMessage data = new SensorDataMessage(
                "TEMP-WARD5-01",
                "TEMPERATURE",
                24.5,
                "°C",
                30.7046,
                76.7179,
                "Ward 5 Municipal Office"
        );
        String payload = objectMapper.writeValueAsString(data);

        SensorReading reading = ioTProcessingService.processDataMessage("smartcity/sensors/TEMP-WARD5-01/data", payload);

        assertNotNull(reading);
        assertNotNull(reading.getId());
        assertEquals("TEMP-WARD5-01", reading.getSensorId());
        assertEquals("TEMPERATURE", reading.getSensorType());
        assertEquals(24.5, reading.getValue());
        assertEquals("NORMAL", reading.getStatus());

        // Verify SensorDevice upsert
        SensorDevice device = sensorDeviceRepository.findBySensorId("TEMP-WARD5-01").orElseThrow();
        assertEquals(SensorStatus.ONLINE, device.getStatus());
        assertEquals(24.5, device.getLastValue());
        assertNotNull(device.getLastSeenAt());
    }

    @Test
    @DisplayName("Invalid or malformed MQTT message is handled gracefully without crashing")
    void testInvalidMessageHandling() {
        // Malformed JSON payload
        SensorReading reading1 = ioTProcessingService.processDataMessage("smartcity/sensors/ERR-01/data", "{invalid json");
        assertNull(reading1);

        // Missing required value
        SensorReading reading2 = ioTProcessingService.processDataMessage("smartcity/sensors/ERR-02/data", "{\"sensorId\":\"ERR-02\"}");
        assertNull(reading2);

        // Verify invalid event was logged in iot_events
        List<IotEvent> events = iotEventRepository.findBySeverityOrderByTimestampDesc("WARNING");
        assertFalse(events.isEmpty());
    }

    @Test
    @DisplayName("PM2.5 > 300 creates new incident with source IOT, evaluates priority, calculates SLA, and auto-assigns")
    void testPm25SpikeCreatesCriticalIncidentWithFullWorkflow() throws Exception {
        SensorDataMessage data = new SensorDataMessage(
                "PM25-CORRIDOR-09",
                "PM25",
                345.0,
                "µg/m³",
                30.7300,
                76.7800,
                "Industrial Area Phase 2 Hub"
        );
        String payload = objectMapper.writeValueAsString(data);

        SensorReading reading = ioTProcessingService.processDataMessage("smartcity/sensors/PM25-CORRIDOR-09/data", payload);

        assertNotNull(reading);
        assertEquals("ALERT", reading.getStatus());

        // Verify Incident was created in PostgreSQL
        List<Incident> incidents = incidentRepository.findByStatus(IncidentStatus.ASSIGNED);
        Incident matched = null;
        for (Incident inc : incidents) {
            if (inc.getDescription() != null && inc.getDescription().contains("PM25-CORRIDOR-09")) {
                matched = inc;
                break;
            }
        }
        if (matched == null) {
            // Check other active statuses if status advanced
            for (Incident inc : incidentRepository.findAll()) {
                if (inc.getDescription() != null && inc.getDescription().contains("PM25-CORRIDOR-09")) {
                    matched = inc;
                    break;
                }
            }
        }

        assertNotNull(matched, "Incident should have been created for PM2.5 > 300");
        assertEquals(IncidentSource.IOT, matched.getSource());
        assertEquals("AIR_QUALITY_HAZARD", matched.getIncidentType());
        assertNotNull(matched.getPriorityLabel());
        assertNotNull(matched.getAssignedDepartment());

        // Verify IoT Event logged
        List<IotEvent> events = iotEventRepository.findByDeviceIdOrderByTimestampDesc("PM25-CORRIDOR-09");
        assertFalse(events.isEmpty());
        assertEquals("THRESHOLD_BREACH_NEW_INCIDENT", events.getFirst().getEventType());
        assertEquals(matched.getId().toString(), events.getFirst().getIncidentId());
    }

    @Test
    @DisplayName("Water level spike >= 95% creates FLASH_FLOOD incident with full workflow")
    void testWaterLevelSpikeCreatesFlashFloodIncident() throws Exception {
        SensorDataMessage data = new SensorDataMessage(
                "WATER-UNDERPASS-02",
                "WATER_LEVEL",
                96.0,
                "%",
                30.7200,
                76.7400,
                "Railway Underpass Corridor"
        );
        String payload = objectMapper.writeValueAsString(data);

        SensorReading reading = ioTProcessingService.processDataMessage("smartcity/sensors/WATER-UNDERPASS-02/data", payload);

        assertNotNull(reading);
        assertEquals("ALERT", reading.getStatus());

        // Verify Incident created
        boolean found = incidentRepository.findAll().stream()
                .anyMatch(inc -> inc.getDescription() != null &&
                        inc.getDescription().contains("WATER-UNDERPASS-02") &&
                        "FLASH_FLOOD".equals(inc.getIncidentType()));
        assertTrue(found, "Flash flood incident must be created");
    }

    @Test
    @DisplayName("Waste bin fill >= 90% creates WASTE_OVERFLOW incident")
    void testWasteBinOverflowCreatesIncident() throws Exception {
        SensorDataMessage data = new SensorDataMessage(
                "BIN-MARKET-05",
                "WASTE_LEVEL",
                93.5,
                "%",
                30.7100,
                76.7300,
                "Sector 22 Market Alley"
        );
        String payload = objectMapper.writeValueAsString(data);

        SensorReading reading = ioTProcessingService.processDataMessage("smartcity/sensors/BIN-MARKET-05/data", payload);

        assertNotNull(reading);
        assertEquals("ALERT", reading.getStatus());

        boolean found = incidentRepository.findAll().stream()
                .anyMatch(inc -> inc.getDescription() != null &&
                        inc.getDescription().contains("BIN-MARKET-05") &&
                        "WASTE_OVERFLOW".equals(inc.getIncidentType()));
        assertTrue(found, "Waste overflow incident must be created");
    }

    @Test
    @DisplayName("Gas leak >= 100 ppm creates GAS_LEAK incident")
    void testGasLeakCreatesIncident() throws Exception {
        SensorDataMessage data = new SensorDataMessage(
                "GAS-FEEDER-11",
                "GAS",
                185.0,
                "ppm",
                30.7150,
                76.7250,
                "Industrial Area Sector 3"
        );
        String payload = objectMapper.writeValueAsString(data);

        SensorReading reading = ioTProcessingService.processDataMessage("smartcity/sensors/GAS-FEEDER-11/data", payload);

        assertNotNull(reading);
        assertEquals("ALERT", reading.getStatus());

        boolean found = incidentRepository.findAll().stream()
                .anyMatch(inc -> inc.getDescription() != null &&
                        inc.getDescription().contains("GAS-FEEDER-11") &&
                        "GAS_LEAK".equals(inc.getIncidentType()));
        assertTrue(found, "Gas leak incident must be created");
    }

    @Test
    @DisplayName("Electrical hazard indicator >= 1.0 creates ELECTRICAL_HAZARD incident")
    void testElectricalHazardCreatesIncident() throws Exception {
        SensorDataMessage data = new SensorDataMessage(
                "ELEC-SUBSTATION-03",
                "ELECTRICAL",
                1.0,
                "fault",
                30.7050,
                76.7150,
                "Substation 4 Feeder Live Junction"
        );
        String payload = objectMapper.writeValueAsString(data);

        SensorReading reading = ioTProcessingService.processDataMessage("smartcity/sensors/ELEC-SUBSTATION-03/data", payload);

        assertNotNull(reading);
        assertEquals("ALERT", reading.getStatus());

        boolean found = incidentRepository.findAll().stream()
                .anyMatch(inc -> inc.getDescription() != null &&
                        inc.getDescription().contains("ELEC-SUBSTATION-03") &&
                        "ELECTRICAL_HAZARD".equals(inc.getIncidentType()));
        assertTrue(found, "Electrical hazard incident must be created");
    }

    @Test
    @DisplayName("Duplicate prevention: multiple consecutive readings from same sensor condition update existing incident rather than duplicating")
    void testDuplicateIncidentPrevention() throws Exception {
        String sensorId = "PM25-DEDUP-TEST";
        SensorDataMessage firstBreach = new SensorDataMessage(
                sensorId,
                "PM25",
                325.0,
                "µg/m³",
                30.7046,
                76.7179,
                "Downtown Monitoring Station"
        );

        // First message -> should create 1 incident
        ioTProcessingService.processDataMessage("smartcity/sensors/" + sensorId + "/data", objectMapper.writeValueAsString(firstBreach));

        long initialCount = incidentRepository.findAll().stream()
                .filter(inc -> inc.getDescription() != null && inc.getDescription().contains(sensorId))
                .count();
        assertEquals(1, initialCount, "First message must create exactly 1 incident");

        // Second message from same sensor -> threshold still breached
        SensorDataMessage secondBreach = new SensorDataMessage(
                sensorId,
                "PM25",
                335.0,
                "µg/m³",
                30.7046,
                76.7179,
                "Downtown Monitoring Station"
        );
        ioTProcessingService.processDataMessage("smartcity/sensors/" + sensorId + "/data", objectMapper.writeValueAsString(secondBreach));

        // Third message from same sensor
        SensorDataMessage thirdBreach = new SensorDataMessage(
                sensorId,
                "PM25",
                340.0,
                "µg/m³",
                30.7046,
                76.7179,
                "Downtown Monitoring Station"
        );
        ioTProcessingService.processDataMessage("smartcity/sensors/" + sensorId + "/data", objectMapper.writeValueAsString(thirdBreach));

        // Verify still exactly 1 incident in PostgreSQL!
        long finalCount = incidentRepository.findAll().stream()
                .filter(inc -> inc.getDescription() != null && inc.getDescription().contains(sensorId))
                .count();
        assertEquals(1, finalCount, "Deduplication must prevent creating duplicate incidents for ongoing condition");

        // Verify continuing events logged
        List<IotEvent> events = iotEventRepository.findByDeviceIdOrderByTimestampDesc(sensorId);
        assertTrue(events.size() >= 3, "All readings must be logged as IoT events");
        assertTrue(events.stream().anyMatch(e -> "THRESHOLD_BREACH_CONTINUED".equals(e.getEventType())));
    }

    @Test
    @DisplayName("Sensor status heartbeat updates device status to OFFLINE and ONLINE")
    void testSensorStatusUpdate() throws Exception {
        String sensorId = "STATUS-DEVICE-01";

        // Report OFFLINE
        SensorStatusMessage offlineMsg = new SensorStatusMessage(sensorId, "OFFLINE");
        ioTProcessingService.processStatusMessage("smartcity/sensors/" + sensorId + "/status", objectMapper.writeValueAsString(offlineMsg));

        SensorDevice offlineDev = sensorDeviceRepository.findBySensorId(sensorId).orElseThrow();
        assertEquals(SensorStatus.OFFLINE, offlineDev.getStatus());

        // Report ONLINE
        SensorStatusMessage onlineMsg = new SensorStatusMessage(sensorId, "ONLINE");
        ioTProcessingService.processStatusMessage("smartcity/sensors/" + sensorId + "/status", objectMapper.writeValueAsString(onlineMsg));

        SensorDevice onlineDev = sensorDeviceRepository.findBySensorId(sensorId).orElseThrow();
        assertEquals(SensorStatus.ONLINE, onlineDev.getStatus());
    }

    @Test
    @DisplayName("REST APIs /api/iot/sensors, /api/iot/readings, and /api/iot/events return paginated responses")
    void testIoTControllerEndpoints() throws Exception {
        // Seed reading & device
        SensorDataMessage data = new SensorDataMessage("API-TEST-01", "PM25", 45.0, "µg/m³", 30.7, 76.7, "Station 1");
        ioTProcessingService.processDataMessage("smartcity/sensors/API-TEST-01/data", objectMapper.writeValueAsString(data));

        // 1. GET /api/iot/sensors
        mockMvc.perform(get("/api/iot/sensors?page=0&size=10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page", is(0)))
                .andExpect(jsonPath("$.content", notNullValue()));

        // 2. GET /api/iot/sensors/{sensorId}
        mockMvc.perform(get("/api/iot/sensors/API-TEST-01"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sensorId", is("API-TEST-01")))
                .andExpect(jsonPath("$.status", is("ONLINE")));

        // 3. GET /api/iot/sensors/{sensorId}/readings
        mockMvc.perform(get("/api/iot/sensors/API-TEST-01/readings?page=0&size=5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", notNullValue()));

        // 4. GET /api/iot/readings
        mockMvc.perform(get("/api/iot/readings?page=0&size=10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", notNullValue()));

        // 5. GET /api/iot/events
        mockMvc.perform(get("/api/iot/events?page=0&size=10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", notNullValue()));
    }
}
