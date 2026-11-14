package com.smartcity.security;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartcity.assignment.AssignmentRule;
import com.smartcity.assignment.AssignmentRuleRepository;
import com.smartcity.common.AuditLog;
import com.smartcity.common.AuditLogRepository;
import com.smartcity.department.Department;
import com.smartcity.department.DepartmentRepository;
import com.smartcity.department.Team;
import com.smartcity.department.TeamRepository;
import com.smartcity.incident.Incident;
import com.smartcity.incident.IncidentRepository;
import com.smartcity.incident.IncidentSource;
import com.smartcity.incident.IncidentStatus;
import com.smartcity.incident.dto.CreateIncidentRequest;
import com.smartcity.priority.PriorityTier;
import com.smartcity.security.dto.LoginRequest;
import com.smartcity.security.dto.RegisterRequest;
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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@DisplayName("Authentication, JWT Protection, and Role-Based Access Control Integration Tests")
class AuthenticationSecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private TeamRepository teamRepository;

    @Autowired
    private IncidentRepository incidentRepository;

    @Autowired
    private AssignmentRuleRepository assignmentRuleRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    private ObjectMapper objectMapper;

    private Incident testIncident;
    private Department testDept;
    private Team testTeam;

    @BeforeEach
    void setUp() {
        // Ensure default roles exist
        for (String rName : List.of("ADMIN", "COMMAND_OPERATOR", "DEPARTMENT_OFFICER", "FIELD_WORKER", "CITIZEN")) {
            if (roleRepository.findByName(rName).isEmpty()) {
                roleRepository.save(new Role(rName, "Test role: " + rName));
            }
        }

        testDept = departmentRepository.findByCode("TRAFFIC").orElseGet(() ->
                departmentRepository.saveAndFlush(new Department("TRAFFIC", "Traffic Department", "Traffic", "traffic@city.gov", "1095")));

        testTeam = teamRepository.findByCode("TRAFFIC_OPS").orElseGet(() ->
                teamRepository.saveAndFlush(new Team(testDept, "TRAFFIC_OPS", "Traffic Operations Response Team", "1095")));

        if (assignmentRuleRepository.findByIncidentTypeIgnoreCaseAndActiveTrue("TRAFFIC_SIGNAL_FAILURE").isEmpty()) {
            assignmentRuleRepository.saveAndFlush(new AssignmentRule(
                    "TRAFFIC_SIGNAL_FAILURE", "TRAFFIC", "Traffic Department", "TRAFFIC_OPS", "Auto assign traffic signal failure"
            ));
        }

        testIncident = new Incident();
        testIncident.setIncidentNumber("INC-SEC-" + UUID.randomUUID().toString().substring(0, 8));
        testIncident.setIncidentType("TRAFFIC_SIGNAL_FAILURE");
        testIncident.setDescription("Traffic signal outage at junction");
        testIncident.setSource(IncidentSource.CITIZEN);
        testIncident.setLatitude(12.9716);
        testIncident.setLongitude(77.5946);
        testIncident.setStatus(IncidentStatus.PRIORITIZED);
        testIncident.setTier(PriorityTier.HIGH);
        testIncident.setAssignedDepartment(testDept);
        testIncident.setAssignedTeam(testTeam);
        testIncident = incidentRepository.saveAndFlush(testIncident);
    }

    private User createTestUser(String username, String email, String rawPassword, String roleName) {
        Role role = roleRepository.findByName(roleName).orElseThrow();
        User user = new User(
                username,
                email,
                passwordEncoder.encode(rawPassword),
                "Test User " + username,
                "9876543210",
                role,
                testDept,
                testTeam
        );
        user.setIsMobileVerified(true);
        return userRepository.save(user);
    }

    private String generateTokenFor(User user) {
        return jwtUtils.generateTokenFromUsername(
                user.getUsername(),
                user.getRoles().stream()
                        .map(r -> r.getName().startsWith("ROLE_") ? r.getName() : "ROLE_" + r.getName())
                        .toList()
        );
    }

    // =========================================================================
    // 1. REGISTRATION TESTS
    // =========================================================================

    @Test
    @DisplayName("POST /api/auth/register successfully registers a citizen and hashes password")
    void testRegisterCitizenSuccess() throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("citizen_raj");
        request.setEmail("raj.citizen@smartcity.gov");
        request.setPassword("SecurePass@2026");
        request.setFullName("Raj Citizen");
        request.setPhone("9988776655");
        request.setRole("CITIZEN");

        MvcResult result = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.username", is("citizen_raj")))
                .andExpect(jsonPath("$.email", is("raj.citizen@smartcity.gov")))
                .andExpect(jsonPath("$.fullName", is("Raj Citizen")))
                .andExpect(jsonPath("$.roles", hasItem("CITIZEN")))
                // Ensure sensitive password fields are NEVER returned in response
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist())
                .andReturn();

        // Verify password is encrypted with BCrypt in the database
        User savedUser = userRepository.findByUsername("citizen_raj").orElseThrow();
        assertNotNull(savedUser.getPasswordHash());
        assertTrue(savedUser.getPasswordHash().startsWith("$2a$"), "Password hash must start with BCrypt identifier $2a$");
        assertNotEquals("SecurePass@2026", savedUser.getPasswordHash());
        assertTrue(passwordEncoder.matches("SecurePass@2026", savedUser.getPasswordHash()));

        // Verify audit log entry was created
        List<AuditLog> auditLogs = auditLogRepository.findByEntityTypeAndEntityIdOrderByCreatedAtDesc("USER", savedUser.getId().toString());
        assertFalse(auditLogs.isEmpty());
        assertEquals("USER_REGISTERED", auditLogs.getFirst().getAction());
    }

    @Test
    @DisplayName("POST /api/auth/register fails with 409 Conflict when username already exists")
    void testRegisterDuplicateUsername() throws Exception {
        createTestUser("existing_user", "existing@smartcity.gov", "Pass@123", "CITIZEN");

        RegisterRequest request = new RegisterRequest();
        request.setUsername("existing_user");
        request.setEmail("different@smartcity.gov");
        request.setPassword("SecurePass@2026");
        request.setFullName("Duplicate Attempt");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.status", is(409)))
                .andExpect(jsonPath("$.errorCode", is("DUPLICATE_RESOURCE")));
    }

    @Test
    @DisplayName("POST /api/auth/register fails with 409 Conflict when email already exists")
    void testRegisterDuplicateEmail() throws Exception {
        createTestUser("user_one", "shared@smartcity.gov", "Pass@123", "CITIZEN");

        RegisterRequest request = new RegisterRequest();
        request.setUsername("user_two");
        request.setEmail("shared@smartcity.gov");
        request.setPassword("SecurePass@2026");
        request.setFullName("Duplicate Email Attempt");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.status", is(409)))
                .andExpect(jsonPath("$.errorCode", is("DUPLICATE_RESOURCE")));
    }

    @Test
    @DisplayName("POST /api/auth/register fails with 400 Bad Request on validation failure")
    void testRegisterValidationFailure() throws Exception {
        RegisterRequest invalidRequest = new RegisterRequest();
        // Missing username, invalid email, short password

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.errorCode", is("VALIDATION_FAILED")))
                .andExpect(jsonPath("$.errors.username", notNullValue()))
                .andExpect(jsonPath("$.errors.email", notNullValue()))
                .andExpect(jsonPath("$.errors.password", notNullValue()));
    }

    // =========================================================================
    // 2. LOGIN AND JWT GENERATION TESTS
    // =========================================================================

    @Test
    @DisplayName("POST /api/auth/login returns 200 with JWT access token for valid credentials")
    void testLoginSuccess() throws Exception {
        createTestUser("operator_bob", "bob@smartcity.gov", "BobSecret@2026", "COMMAND_OPERATOR");

        LoginRequest request = new LoginRequest("operator_bob", "BobSecret@2026");

        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token", notNullValue()))
                .andExpect(jsonPath("$.tokenType", is("Bearer")))
                .andExpect(jsonPath("$.expiresInMs", notNullValue()))
                .andExpect(jsonPath("$.user.username", is("operator_bob")))
                .andExpect(jsonPath("$.user.email", is("bob@smartcity.gov")))
                .andExpect(jsonPath("$.user.roles", hasItem("COMMAND_OPERATOR")))
                .andExpect(jsonPath("$.user.password").doesNotExist())
                .andExpect(jsonPath("$.user.passwordHash").doesNotExist())
                .andReturn();

        // Verify generated token is valid and contains user claims
        JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString());
        String token = root.get("token").asText();
        assertTrue(jwtUtils.validateJwtToken(token));
        assertEquals("operator_bob", jwtUtils.getUsernameFromJwtToken(token));

        // Verify audit log
        List<AuditLog> auditLogs = auditLogRepository.findByEntityTypeAndEntityIdOrderByCreatedAtDesc("AUTH", userRepository.findByUsername("operator_bob").get().getId().toString());
        assertFalse(auditLogs.isEmpty());
        assertEquals("LOGIN_SUCCESS", auditLogs.getFirst().getAction());
    }

    @Test
    @DisplayName("POST /api/auth/login fails with 401 Unauthorized for incorrect password")
    void testLoginInvalidPassword() throws Exception {
        createTestUser("officer_sarah", "sarah@smartcity.gov", "CorrectPassword123", "DEPARTMENT_OFFICER");

        LoginRequest request = new LoginRequest("officer_sarah", "WrongPassword999");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.status", is(401)))
                .andExpect(jsonPath("$.errorCode", is("UNAUTHORIZED")))
                .andExpect(jsonPath("$.message", containsString("Invalid username or password")));
    }

    @Test
    @DisplayName("POST /api/auth/login fails with 401 Unauthorized for non-existent user")
    void testLoginNonExistentUser() throws Exception {
        LoginRequest request = new LoginRequest("ghost_user", "SomePassword123");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.status", is(401)))
                .andExpect(jsonPath("$.errorCode", is("UNAUTHORIZED")));
    }

    // =========================================================================
    // 3. GET /api/auth/me AND SENSITIVE FIELD EXCLUSION TESTS
    // =========================================================================

    @Test
    @DisplayName("GET /api/auth/me with valid Bearer token returns 200 with user profile")
    void testGetCurrentUserWithValidToken() throws Exception {
        User user = createTestUser("field_tech", "tech@smartcity.gov", "TechPass123", "FIELD_WORKER");
        String token = generateTokenFor(user);

        mockMvc.perform(get("/api/auth/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username", is("field_tech")))
                .andExpect(jsonPath("$.email", is("tech@smartcity.gov")))
                .andExpect(jsonPath("$.fullName", is("Test User field_tech")))
                .andExpect(jsonPath("$.roles", hasItem("FIELD_WORKER")))
                .andExpect(jsonPath("$.departmentName", is("Traffic Department")))
                // Ensure no password hashes or credentials exposed
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
    }

    @Test
    @DisplayName("GET /api/auth/me without Bearer token returns 401 Unauthorized")
    void testGetCurrentUserUnauthenticated() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.status", is(401)))
                .andExpect(jsonPath("$.errorCode", is("UNAUTHORIZED")));
    }

    @Test
    @DisplayName("GET /api/auth/me with invalid / malformed token returns 401 Unauthorized")
    void testGetCurrentUserWithInvalidToken() throws Exception {
        mockMvc.perform(get("/api/auth/me")
                        .header("Authorization", "Bearer invalid-tampered-token-123"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.status", is(401)))
                .andExpect(jsonPath("$.errorCode", is("UNAUTHORIZED")));
    }

    // =========================================================================
    // 4. UNPROTECTED VS PROTECTED ENDPOINT 401 UNAUTHORIZED CHECKS
    // =========================================================================

    @Test
    @DisplayName("Unauthenticated request to protected endpoint returns 401 Unauthorized")
    void testUnauthenticatedAccessReturns401() throws Exception {
        mockMvc.perform(get("/api/incidents"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.status", is(401)))
                .andExpect(jsonPath("$.errorCode", is("UNAUTHORIZED")));
    }

    // =========================================================================
    // 5. ROLE-BASED ACCESS CONTROL (RBAC) 403 FORBIDDEN & PERMISSION TESTS
    // =========================================================================

    @Test
    @DisplayName("CITIZEN can create incidents and query incidents, but cannot access command centre operations")
    void testCitizenRolePermissions() throws Exception {
        User citizen = createTestUser("citizen_alok", "alok@citizen.gov", "Pass12345", "CITIZEN");
        String token = generateTokenFor(citizen);

        // 1. Citizen CAN create incidents
        CreateIncidentRequest createReq = new CreateIncidentRequest();
        createReq.setIncidentType("OPEN_MANHOLE");
        createReq.setDescription("Hazardous open manhole near park");
        createReq.setSource(IncidentSource.CITIZEN);
        createReq.setLatitude(12.95);
        createReq.setLongitude(77.60);

        mockMvc.perform(post("/api/incidents")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.incidentType", is("OPEN_MANHOLE")));

        // 2. Citizen CAN view incidents
        mockMvc.perform(get("/api/incidents")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        // 3. Citizen CANNOT auto-assign incidents -> 403 Forbidden
        mockMvc.perform(post("/api/incidents/{id}/auto-assign", testIncident.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.status", is(403)))
                .andExpect(jsonPath("$.errorCode", is("FORBIDDEN")));

        // 4. Citizen CANNOT evaluate priority -> 403 Forbidden
        mockMvc.perform(post("/api/incidents/{id}/evaluate-priority", testIncident.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status", is(403)));

        // 5. Citizen CANNOT delete incident -> 403 Forbidden
        mockMvc.perform(delete("/api/incidents/{id}", testIncident.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status", is(403)));

        // 6. Citizen CANNOT view SLA active records -> 403 Forbidden
        mockMvc.perform(get("/api/sla/active")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status", is(403)));
    }

    @Test
    @DisplayName("COMMAND_OPERATOR can auto-assign, evaluate priority, and view SLA, but cannot DELETE incident")
    void testCommandOperatorRolePermissions() throws Exception {
        User operator = createTestUser("cop_john", "john@command.gov", "Pass12345", "COMMAND_OPERATOR");
        String token = generateTokenFor(operator);

        // 1. Operator CAN evaluate priority
        mockMvc.perform(post("/api/incidents/{id}/evaluate-priority", testIncident.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        // 2. Operator CAN auto-assign
        mockMvc.perform(post("/api/incidents/{id}/auto-assign", testIncident.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        // 3. Operator CAN view active SLA records
        mockMvc.perform(get("/api/sla/active")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        // 4. Operator CANNOT delete incidents (Admin only) -> 403 Forbidden
        mockMvc.perform(delete("/api/incidents/{id}", testIncident.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status", is(403)))
                .andExpect(jsonPath("$.errorCode", is("FORBIDDEN")));
    }

    @Test
    @DisplayName("DEPARTMENT_OFFICER can view SLA and departments, but cannot auto-assign or DELETE incident")
    void testDepartmentOfficerRolePermissions() throws Exception {
        User officer = createTestUser("officer_dave", "dave@dept.gov", "Pass12345", "DEPARTMENT_OFFICER");
        String token = generateTokenFor(officer);

        // 1. Officer CAN view departments
        mockMvc.perform(get("/api/departments")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        // 2. Officer CAN view active SLA records
        mockMvc.perform(get("/api/sla/active")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        // 3. Officer CANNOT auto-assign (Command Operator / Admin only) -> 403 Forbidden
        mockMvc.perform(post("/api/incidents/{id}/auto-assign", testIncident.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status", is(403)));

        // 4. Officer CANNOT delete incident -> 403 Forbidden
        mockMvc.perform(delete("/api/incidents/{id}", testIncident.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status", is(403)));
    }

    @Test
    @DisplayName("FIELD_WORKER can view actions, but cannot access SLA metrics or DELETE incidents")
    void testFieldWorkerRolePermissions() throws Exception {
        User worker = createTestUser("worker_maya", "maya@field.gov", "Pass12345", "FIELD_WORKER");
        String token = generateTokenFor(worker);

        // 1. Field worker CAN view incident response actions
        mockMvc.perform(get("/api/incidents/{id}/actions", testIncident.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        // 2. Field worker CANNOT access active SLA records -> 403 Forbidden
        mockMvc.perform(get("/api/sla/active")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status", is(403)));

        // 3. Field worker CANNOT delete incidents -> 403 Forbidden
        mockMvc.perform(delete("/api/incidents/{id}", testIncident.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status", is(403)));
    }

    @Test
    @DisplayName("ADMIN has full system access including deleting incidents")
    void testAdminRolePermissions() throws Exception {
        User admin = createTestUser("super_admin", "admin@smartcity.gov", "AdminPass@2026", "ADMIN");
        String token = generateTokenFor(admin);

        // 1. Admin CAN evaluate priority
        mockMvc.perform(post("/api/incidents/{id}/evaluate-priority", testIncident.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        // 2. Admin CAN auto-assign
        mockMvc.perform(post("/api/incidents/{id}/auto-assign", testIncident.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        // 3. Admin CAN delete incidents
        mockMvc.perform(delete("/api/incidents/{id}", testIncident.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());

        // 4. Admin CAN access user management APIs
        mockMvc.perform(get("/api/users")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", isA(List.class)));

        mockMvc.perform(get("/api/users/stats")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalUsers", notNullValue()));
    }

    @Test
    @DisplayName("Non-admin roles cannot access /api/users endpoints (returns 403 Forbidden)")
    void testNonAdminCannotAccessUserManagement() throws Exception {
        User citizen = createTestUser("cit_user1", "cit_user1@city.gov", "Pass12345", "CITIZEN");
        String citToken = generateTokenFor(citizen);

        mockMvc.perform(get("/api/users")
                        .header("Authorization", "Bearer " + citToken))
                .andExpect(status().isForbidden());

        User operator = createTestUser("op_user1", "op_user1@city.gov", "Pass12345", "COMMAND_OPERATOR");
        String opToken = generateTokenFor(operator);

        mockMvc.perform(get("/api/users")
                        .header("Authorization", "Bearer " + opToken))
                .andExpect(status().isForbidden());

        User worker = createTestUser("fw_user1", "fw_user1@city.gov", "Pass12345", "FIELD_WORKER");
        String fwToken = generateTokenFor(worker);

        mockMvc.perform(get("/api/users/stats")
                        .header("Authorization", "Bearer " + fwToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Registration with ADMIN role is rejected")
    void testRegistrationWithAdminRoleIsForbidden() throws Exception {
        RegisterRequest adminReq = new RegisterRequest();
        adminReq.setUsername("malicious_admin");
        adminReq.setEmail("hacker@city.gov");
        adminReq.setPassword("SecretPass123");
        adminReq.setFullName("Malicious Admin");
        adminReq.setRole("ADMIN");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(adminReq)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Demo accounts for all 5 roles can authenticate and receive correct role claims")
    void testDemoAccountsAuthenticationAndClaims() throws Exception {
        String[] roles = {"ADMIN", "COMMAND_OPERATOR", "DEPARTMENT_OFFICER", "FIELD_WORKER", "CITIZEN"};
        for (String roleName : roles) {
            String uname = "demo_" + roleName.toLowerCase();
            String email = uname + "@testcity.gov";
            User demoUser = createTestUser(uname, email, "Admin@CivicPulse2026", roleName);

            LoginRequest loginReq = new LoginRequest(uname, "Admin@CivicPulse2026");
            MvcResult res = mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(loginReq)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.token", notNullValue()))
                    .andExpect(jsonPath("$.user.role", is(roleName)))
                    .andExpect(jsonPath("$.user.roles", hasItem(roleName)))
                    .andReturn();

            JsonNode node = objectMapper.readTree(res.getResponse().getContentAsString());
            String token = node.get("token").asText();

            // Verify /api/auth/me returns the same role
            mockMvc.perform(get("/api/auth/me")
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.role", is(roleName)))
                    .andExpect(jsonPath("$.username", is(uname)));
        }
    }
}
