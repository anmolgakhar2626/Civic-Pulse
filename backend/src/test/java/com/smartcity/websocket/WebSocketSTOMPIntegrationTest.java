package com.smartcity.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartcity.security.JwtUtils;
import com.smartcity.user.Role;
import com.smartcity.user.RoleRepository;
import com.smartcity.user.User;
import com.smartcity.user.UserRepository;
import com.smartcity.websocket.dto.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.stomp.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;

import java.lang.reflect.Type;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DisplayName("WebSocket STOMP Full End-to-End Real-Time Integration Test")
class WebSocketSTOMPIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private WebSocketEventPublisher publisher;

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

    private WebSocketStompClient stompClient;
    private StompSession stompSession;
    private String jwtToken;

    @BeforeEach
    void setUp() throws Exception {
        String username = "ws_test_user_" + UUID.randomUUID().toString().substring(0, 6);
        Role operatorRole = roleRepository.findByName("COMMAND_OPERATOR")
                .orElseGet(() -> roleRepository.save(new Role("COMMAND_OPERATOR", "Operator")));

        User user = new User(
                username,
                username + "@smartcity.gov",
                passwordEncoder.encode("Pass123!"),
                "Test User",
                "9876543212",
                operatorRole,
                null,
                null
        );
        userRepository.save(user);

        jwtToken = jwtUtils.generateTokenFromUsername(username, java.util.List.of("ROLE_COMMAND_OPERATOR"));

        stompClient = new WebSocketStompClient(new StandardWebSocketClient());
        MappingJackson2MessageConverter converter = new MappingJackson2MessageConverter();
        converter.setObjectMapper(objectMapper);
        stompClient.setMessageConverter(converter);

        String wsUrl = "ws://localhost:" + port + "/ws";
        StompHeaders connectHeaders = new StompHeaders();
        connectHeaders.add("Authorization", "Bearer " + jwtToken);

        stompSession = stompClient.connectAsync(wsUrl, new WebSocketHttpHeaders(), connectHeaders, new StompSessionHandlerAdapter() {
            @Override
            public void handleTransportError(StompSession session, Throwable exception) {
                // handle transport error
            }
        }).get(5, TimeUnit.SECONDS);

        assertNotNull(stompSession, "STOMP session should be established");
        assertTrue(stompSession.isConnected(), "STOMP session should be connected");
    }

    @AfterEach
    void tearDown() {
        if (stompSession != null && stompSession.isConnected()) {
            stompSession.disconnect();
        }
        if (stompClient != null) {
            stompClient.stop();
        }
    }

    @Test
    @DisplayName("Should receive IncidentEvent on /topic/incidents when published")
    void shouldReceiveIncidentEvent() throws Exception {
        BlockingQueue<IncidentEvent> queue = new LinkedBlockingQueue<>();

        stompSession.subscribe("/topic/incidents", new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return IncidentEvent.class;
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                queue.offer((IncidentEvent) payload);
            }
        });

        Thread.sleep(500);

        String incidentId = UUID.randomUUID().toString();
        IncidentEvent testEvent = new IncidentEvent(
                incidentId,
                "INC-TEST-001",
                "ELECTRICAL_HAZARD",
                "Live wire snapped on Sector 22 road",
                "TIER_1",
                "CRITICAL",
                "DETECTED",
                30.7333,
                76.7794,
                "Emergency Services",
                Instant.now(),
                "CREATED"
        );

        publisher.publishIncidentEvent(testEvent);

        IncidentEvent received = queue.poll(5, TimeUnit.SECONDS);
        assertNotNull(received, "Expected IncidentEvent from STOMP subscription");
        assertEquals(incidentId, received.getIncidentId());
        assertEquals("INC-TEST-001", received.getIncidentNumber());
        assertEquals("ELECTRICAL_HAZARD", received.getIncidentType());
        assertEquals("CRITICAL", received.getPriorityLabel());
        assertEquals("CREATED", received.getEventType());
    }

    @Test
    @DisplayName("Should receive SensorEvent on /topic/sensors when telemetry is published")
    void shouldReceiveSensorEvent() throws Exception {
        BlockingQueue<SensorEvent> queue = new LinkedBlockingQueue<>();

        stompSession.subscribe("/topic/sensors", new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return SensorEvent.class;
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                queue.offer((SensorEvent) payload);
            }
        });

        Thread.sleep(500);

        SensorEvent testSensorEvent = new SensorEvent(
                "SMOKE-MONITOR-05",
                "SMOKE",
                88.5,
                "ppm",
                30.7100,
                76.7200,
                Instant.now(),
                "ALERT"
        );

        publisher.publishSensorEvent(testSensorEvent);

        SensorEvent received = queue.poll(5, TimeUnit.SECONDS);
        assertNotNull(received, "Expected SensorEvent from /topic/sensors");
        assertEquals("SMOKE-MONITOR-05", received.getSensorId());
        assertEquals("SMOKE", received.getSensorType());
        assertEquals(88.5, received.getValue());
        assertEquals("ppm", received.getUnit());
        assertEquals("ALERT", received.getStatus());
    }

    @Test
    @DisplayName("Should receive SLAEvent on /topic/sla when SLA state changes")
    void shouldReceiveSLAEvent() throws Exception {
        BlockingQueue<SLAEvent> queue = new LinkedBlockingQueue<>();

        stompSession.subscribe("/topic/sla", new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return SLAEvent.class;
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                queue.offer((SLAEvent) payload);
            }
        });

        Thread.sleep(500);

        String incId = UUID.randomUUID().toString();
        String slaId = UUID.randomUUID().toString();
        SLAEvent slaEvent = new SLAEvent(
                incId,
                slaId,
                "HIGH",
                Instant.now().plusSeconds(7200),
                120L,
                "AT_RISK",
                "LEVEL_1",
                Instant.now()
        );

        publisher.publishSLAEvent(slaEvent);

        SLAEvent received = queue.poll(5, TimeUnit.SECONDS);
        assertNotNull(received, "Expected SLAEvent from /topic/sla");
        assertEquals(incId, received.getIncidentId());
        assertEquals(slaId, received.getSlaId());
        assertEquals("AT_RISK", received.getSlaStatus());
        assertEquals("LEVEL_1", received.getEscalationLevel());
    }

    @Test
    @DisplayName("Should receive AlertEvent on /topic/alerts for threshold breaches and critical incidents")
    void shouldReceiveAlertEvent() throws Exception {
        BlockingQueue<AlertEvent> queue = new LinkedBlockingQueue<>();

        stompSession.subscribe("/topic/alerts", new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return AlertEvent.class;
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                queue.offer((AlertEvent) payload);
            }
        });

        Thread.sleep(500);

        AlertEvent alert = new AlertEvent(
                "SENSOR_THRESHOLD",
                "CRITICAL",
                "PM2.5 Severe Toxic Smog",
                "PM2.5 reading 380 µg/m³ exceeded critical safe boundary",
                null,
                30.7333,
                76.7794
        );

        publisher.publishAlert(alert);

        AlertEvent received = queue.poll(5, TimeUnit.SECONDS);
        assertNotNull(received, "Expected AlertEvent from /topic/alerts");
        assertEquals("SENSOR_THRESHOLD", received.getType());
        assertEquals("CRITICAL", received.getSeverity());
        assertEquals("PM2.5 Severe Toxic Smog", received.getTitle());
    }

    @Test
    @DisplayName("Should receive AssignmentEvent on /topic/assignments when incident is assigned")
    void shouldReceiveAssignmentEvent() throws Exception {
        BlockingQueue<AssignmentEvent> queue = new LinkedBlockingQueue<>();

        stompSession.subscribe("/topic/assignments", new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return AssignmentEvent.class;
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                queue.offer((AssignmentEvent) payload);
            }
        });

        Thread.sleep(500);

        String incidentId = UUID.randomUUID().toString();
        AssignmentEvent assignment = new AssignmentEvent(
                incidentId,
                "Traffic Department",
                "Traffic Signal Quick Response",
                "SYSTEM_AUTO_DISPATCH",
                Instant.now(),
                "Junction traffic signal non-functional"
        );

        publisher.publishAssignmentEvent(assignment);

        AssignmentEvent received = queue.poll(5, TimeUnit.SECONDS);
        assertNotNull(received, "Expected AssignmentEvent from /topic/assignments");
        assertEquals(incidentId, received.getIncidentId());
        assertEquals("Traffic Department", received.getDepartment());
        assertEquals("Traffic Signal Quick Response", received.getTeam());
        assertEquals("SYSTEM_AUTO_DISPATCH", received.getAssignedBy());
    }
}
