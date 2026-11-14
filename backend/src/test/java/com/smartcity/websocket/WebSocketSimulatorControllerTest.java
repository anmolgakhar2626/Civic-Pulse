package com.smartcity.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartcity.security.JwtUtils;
import com.smartcity.user.Role;
import com.smartcity.user.RoleRepository;
import com.smartcity.user.User;
import com.smartcity.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@DisplayName("WebSocket Simulator & Trigger Controller Security & Functionality Tests")
class WebSocketSimulatorControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    private ObjectMapper objectMapper;

    private String operatorToken;
    private String citizenToken;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
        Role operatorRole = roleRepository.findByName("COMMAND_OPERATOR")
                .orElseGet(() -> roleRepository.save(new Role("COMMAND_OPERATOR", "Command Operator")));
        Role citizenRole = roleRepository.findByName("CITIZEN")
                .orElseGet(() -> roleRepository.save(new Role("CITIZEN", "Citizen")));

        String opUsername = "sim_operator_" + UUID.randomUUID().toString().substring(0, 6);
        User operator = new User(
                opUsername,
                opUsername + "@smartcity.gov",
                passwordEncoder.encode("Secret123!"),
                "Officer Sim",
                "9876543210",
                operatorRole,
                null,
                null
        );
        userRepository.save(operator);
        operatorToken = jwtUtils.generateTokenFromUsername(opUsername, java.util.List.of("ROLE_COMMAND_OPERATOR"));

        String citUsername = "sim_citizen_" + UUID.randomUUID().toString().substring(0, 6);
        User citizen = new User(
                citUsername,
                citUsername + "@smartcity.gov",
                passwordEncoder.encode("Secret123!"),
                "Citizen Sim",
                "9876543211",
                citizenRole,
                null,
                null
        );
        userRepository.save(citizen);
        citizenToken = jwtUtils.generateTokenFromUsername(citUsername, java.util.List.of("ROLE_CITIZEN"));
    }

    @Test
    @DisplayName("Should return 401 Unauthorized when triggering simulator without auth token")
    void shouldReturn401WhenUnauthenticated() throws Exception {
        mockMvc.perform(post("/api/websocket/trigger/incident")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Should return 403 Forbidden when citizen attempts to trigger simulator")
    void shouldReturn403WhenCitizenAccessesSimulator() throws Exception {
        mockMvc.perform(post("/api/websocket/trigger/incident")
                        .header("Authorization", "Bearer " + citizenToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Should trigger incident event when operator makes authorized request")
    void shouldTriggerIncidentEventSuccessfully() throws Exception {
        Map<String, Object> req = Map.of(
                "incidentType", "MAJOR_ROAD_ACCIDENT",
                "priorityTier", "CRITICAL",
                "status", "DETECTED"
        );

        mockMvc.perform(post("/api/websocket/trigger/incident")
                        .header("Authorization", "Bearer " + operatorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.topic", is("/topic/incidents")))
                .andExpect(jsonPath("$.event.incidentType", is("MAJOR_ROAD_ACCIDENT")))
                .andExpect(jsonPath("$.event.priorityTier", is("CRITICAL")))
                .andExpect(jsonPath("$.event.eventType", is("CREATED")));
    }

    @Test
    @DisplayName("Should trigger PM2.5 spike and broadcast alert when threshold is crossed")
    void shouldTriggerPm25SpikeAlert() throws Exception {
        Map<String, Object> req = Map.of(
                "sensorId", "AIR-QUALITY-CHANDIGARH-01",
                "value", 345.5
        );

        mockMvc.perform(post("/api/websocket/trigger/pm25-spike")
                        .header("Authorization", "Bearer " + operatorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.sensorTopic", is("/topic/sensors")))
                .andExpect(jsonPath("$.alertTopic", is("/topic/alerts")))
                .andExpect(jsonPath("$.sensorEvent.value", is(345.5)))
                .andExpect(jsonPath("$.sensorEvent.status", is("ALERT")))
                .andExpect(jsonPath("$.alertEvent.type", is("SENSOR_THRESHOLD")))
                .andExpect(jsonPath("$.alertEvent.severity", is("CRITICAL")));
    }

    @Test
    @DisplayName("Should trigger SLA AT_RISK broadcast and alert")
    void shouldTriggerSlaAtRiskEvent() throws Exception {
        String incidentId = UUID.randomUUID().toString();
        Map<String, Object> req = Map.of("incidentId", incidentId);

        mockMvc.perform(post("/api/websocket/trigger/sla-at-risk")
                        .header("Authorization", "Bearer " + operatorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.slaTopic", is("/topic/sla")))
                .andExpect(jsonPath("$.alertTopic", is("/topic/alerts")))
                .andExpect(jsonPath("$.slaEvent.slaStatus", is("AT_RISK")))
                .andExpect(jsonPath("$.slaEvent.remainingMinutes", is(15)))
                .andExpect(jsonPath("$.alertEvent.type", is("SLA_AT_RISK")));
    }

    @Test
    @DisplayName("Should trigger assignment broadcast to /topic/assignments and /topic/incidents")
    void shouldTriggerAssignmentEvent() throws Exception {
        String incidentId = UUID.randomUUID().toString();
        Map<String, Object> req = Map.of(
                "incidentId", incidentId,
                "department", "Public Works",
                "team", "Road Maintenance Unit"
        );

        mockMvc.perform(post("/api/websocket/trigger/assignment")
                        .header("Authorization", "Bearer " + operatorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.assignmentTopic", is("/topic/assignments")))
                .andExpect(jsonPath("$.incidentTopic", is("/topic/incidents")))
                .andExpect(jsonPath("$.assignmentEvent.department", is("Public Works")))
                .andExpect(jsonPath("$.assignmentEvent.team", is("Road Maintenance Unit")))
                .andExpect(jsonPath("$.incidentEvent.eventType", is("ASSIGNED")));
    }
}
