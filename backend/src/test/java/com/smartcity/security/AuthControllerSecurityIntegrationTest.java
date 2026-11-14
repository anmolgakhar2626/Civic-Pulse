package com.smartcity.security;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartcity.common.GlobalExceptionHandler;
import com.smartcity.security.dto.*;
import com.smartcity.user.Role;
import com.smartcity.user.RoleRepository;
import com.smartcity.user.User;
import com.smartcity.user.UserJdbcRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Optional;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Authentication & JDBC Security Integration Tests")
class AuthControllerSecurityIntegrationTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    private UserJdbcRepository userJdbcRepository;
    private PasswordEncoder passwordEncoder;
    private JwtUtils jwtUtils;
    private AuthServiceImpl authService;
    private AuthController authController;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private AuthenticationManager authenticationManager;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        passwordEncoder = new BCryptPasswordEncoder();

        // 1. Setup isolated in-memory test database for JDBC repository
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName("org.h2.Driver");
        dataSource.setUrl("jdbc:h2:mem:auth_sec_test_" + UUID.randomUUID().toString().replace("-", "")
                + ";MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1");
        dataSource.setUsername("sa");
        dataSource.setPassword("");

        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
        jdbcTemplate.execute("""
            CREATE TABLE departments (
                id UUID PRIMARY KEY,
                name VARCHAR(100),
                code VARCHAR(50)
            );
            CREATE TABLE teams (
                id UUID PRIMARY KEY,
                name VARCHAR(100),
                code VARCHAR(50)
            );
            CREATE TABLE roles (
                id UUID PRIMARY KEY,
                name VARCHAR(100)
            );
            CREATE TABLE users (
                id UUID PRIMARY KEY,
                full_name VARCHAR(150) NOT NULL,
                email VARCHAR(150) NOT NULL UNIQUE,
                username VARCHAR(100) UNIQUE,
                password_hash VARCHAR(255) NOT NULL,
                role VARCHAR(50) NOT NULL DEFAULT 'CITIZEN',
                enabled BOOLEAN NOT NULL DEFAULT TRUE,
                phone VARCHAR(50),
                mobile_number VARCHAR(50),
                is_mobile_verified BOOLEAN DEFAULT FALSE,
                genuine_complaint_count INT DEFAULT 0,
                is_verified_citizen BOOLEAN DEFAULT FALSE,
                department_id UUID,
                team_id UUID,
                role_id UUID,
                active BOOLEAN DEFAULT TRUE,
                created_at TIMESTAMP NOT NULL,
                updated_at TIMESTAMP NOT NULL
            );
        """);

        userJdbcRepository = new UserJdbcRepository(jdbcTemplate);

        // 2. Setup JWT Utilities
        jwtUtils = new JwtUtils();
        ReflectionTestUtils.setField(jwtUtils, "jwtSecret", "c3VwZXJzZWNyZXRqd3RzZWNyZXRrZXlmb3JzbWFydGNpdHljb21tYW5kY2VudHJlY2l2aWNwdWxzZQ==");
        ReflectionTestUtils.setField(jwtUtils, "jwtExpirationMs", 86400000L);

        // 3. Setup real AuthService with JDBC repository
        authService = new AuthServiceImpl(
                userJdbcRepository,
                roleRepository,
                null,
                null,
                null,
                passwordEncoder,
                authenticationManager,
                jwtUtils
        );

        authController = new AuthController(authService);

        mockMvc = MockMvcBuilders.standaloneSetup(authController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("1. Successful user registration: inserts user via JDBC, hashes password with BCrypt, never returns password hash")
    void testSuccessfulRegistration() throws Exception {
        RegisterRequest request = new RegisterRequest(
                "citizen_jane",
                "jane.doe@smartcity.gov",
                "SecureP@ssw0rd2026",
                "Jane Doe",
                "+1-555-0199",
                "CITIZEN"
        );

        MvcResult result = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.username", is("citizen_jane")))
                .andExpect(jsonPath("$.email", is("jane.doe@smartcity.gov")))
                .andExpect(jsonPath("$.fullName", is("Jane Doe")))
                .andExpect(jsonPath("$.role", is("CITIZEN")))
                .andExpect(jsonPath("$.active", is(true)))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist())
                .andReturn();

        // Verify database state via JDBC lookup
        User dbUser = userJdbcRepository.findByEmail("jane.doe@smartcity.gov").orElseThrow();
        assertNotNull(dbUser);
        assertNotEquals("SecureP@ssw0rd2026", dbUser.getPasswordHash());
        assertTrue(passwordEncoder.matches("SecureP@ssw0rd2026", dbUser.getPasswordHash()));
        assertTrue(dbUser.isEnabled());
    }

    @Test
    @DisplayName("2. Duplicate email rejection: returns 409 Conflict when registering existing email")
    void testDuplicateEmailRejection() throws Exception {
        RegisterRequest req1 = new RegisterRequest(
                "user_one",
                "unique.citizen@city.gov",
                "Passw0rd123!",
                "Citizen One",
                "111",
                "CITIZEN"
        );
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req1)))
                .andExpect(status().isCreated());

        RegisterRequest req2 = new RegisterRequest(
                "user_two",
                "unique.citizen@city.gov",
                "AnotherP@ss123",
                "Citizen Two",
                "222",
                "CITIZEN"
        );

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req2)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", containsString("already exists")));
    }

    @Test
    @DisplayName("3. DTO Validation: rejects invalid registration requests with 400 Bad Request")
    void testDtoValidationFailure() throws Exception {
        RegisterRequest invalidReq = new RegisterRequest(
                "", // Blank username
                "not-a-valid-email", // Invalid email
                "123", // Too short password
                "", // Blank full name
                null,
                "CITIZEN"
        );

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidReq)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("Validation failed")));
    }

    @Test
    @DisplayName("4. Successful login: returns valid JWT access token and user summary without password hash")
    void testSuccessfulLogin() throws Exception {
        // Pre-create user in database
        String rawPassword = "ValidPassword@2026";
        User user = new User(
                "command_operator_1",
                "operator1@civicpulse.gov",
                passwordEncoder.encode(rawPassword),
                "Command Operator One",
                "555-1234",
                new Role("COMMAND_OPERATOR", "Operator"),
                null,
                null
        );
        user.setRoleName("COMMAND_OPERATOR");
        userJdbcRepository.save(user);

        // Mock authentication manager for valid credentials
        Authentication auth = new UsernamePasswordAuthenticationToken(user, rawPassword, user.getAuthorities());
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(auth);

        LoginRequest loginRequest = new LoginRequest("command_operator_1", rawPassword);

        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.tokenType", is("Bearer")))
                .andExpect(jsonPath("$.user.username", is("command_operator_1")))
                .andExpect(jsonPath("$.user.role", is("COMMAND_OPERATOR")))
                .andExpect(jsonPath("$.user.password").doesNotExist())
                .andExpect(jsonPath("$.user.passwordHash").doesNotExist())
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        JsonNode jsonNode = objectMapper.readTree(responseBody);
        String token = jsonNode.get("token").asText();

        assertTrue(jwtUtils.validateJwtToken(token));
        assertEquals("command_operator_1", jwtUtils.getUsernameFromJwtToken(token));
    }

    @Test
    @DisplayName("5. Successful login using Email: resolves user by email address")
    void testSuccessfulLoginWithEmail() throws Exception {
        String rawPassword = "ValidPassword@2026";
        User user = new User(
                "operator_email_user",
                "operator.email@civicpulse.gov",
                passwordEncoder.encode(rawPassword),
                "Email Operator",
                "555-5678",
                new Role("COMMAND_OPERATOR", "Operator"),
                null,
                null
        );
        user.setRoleName("COMMAND_OPERATOR");
        userJdbcRepository.save(user);

        Authentication auth = new UsernamePasswordAuthenticationToken(user, rawPassword, user.getAuthorities());
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(auth);

        LoginRequest loginRequest = new LoginRequest("operator.email@civicpulse.gov", rawPassword);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.user.email", is("operator.email@civicpulse.gov")));
    }

    @Test
    @DisplayName("6. Invalid password rejection: returns 401 Unauthorized")
    void testInvalidPasswordRejection() throws Exception {
        User user = new User(
                "citizen_mark",
                "mark@city.gov",
                passwordEncoder.encode("RealPassword123"),
                "Mark Spencer",
                "123",
                new Role("CITIZEN", "Citizen"),
                null,
                null
        );
        userJdbcRepository.save(user);

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        LoginRequest loginRequest = new LoginRequest("citizen_mark", "WrongPassword!");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message", is("Invalid username or password")));
    }

    @Test
    @DisplayName("7. Disabled account rejection: returns 401 Unauthorized when account status is disabled")
    void testDisabledAccountRejection() throws Exception {
        User user = new User(
                "disabled_user",
                "disabled@city.gov",
                passwordEncoder.encode("Password123!"),
                "Disabled User",
                "123",
                new Role("CITIZEN", "Citizen"),
                null,
                null
        );
        user.setEnabled(false);
        user.setActive(false);
        userJdbcRepository.save(user);

        // Explicitly set enabled = false in database
        userJdbcRepository.updateAccountStatus(user.getId(), false);

        LoginRequest loginRequest = new LoginRequest("disabled_user", "Password123!");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message", containsString("disabled")));
    }

    @Test
    @DisplayName("8. Unauthorized dashboard/me access: returns 401 when unauthenticated")
    void testUnauthorizedAccess() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("9. Authenticated profile lookup GET /api/auth/me")
    void testAuthenticatedGetMe() throws Exception {
        User user = new User(
                "authenticated_user",
                "auth@city.gov",
                passwordEncoder.encode("SecureSecret1!"),
                "Authenticated User",
                "444-5555",
                new Role("COMMAND_OPERATOR", "Operator"),
                null,
                null
        );
        user.setRoleName("COMMAND_OPERATOR");
        userJdbcRepository.save(user);

        Authentication auth = new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());

        mockMvc.perform(get("/api/auth/me")
                        .principal(auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username", is("authenticated_user")))
                .andExpect(jsonPath("$.role", is("COMMAND_OPERATOR")))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
    }

    @Test
    @DisplayName("10. Safe Logout endpoint POST /api/auth/logout")
    void testLogout() throws Exception {
        User user = new User(
                "logout_user",
                "logout@city.gov",
                passwordEncoder.encode("Pass123!"),
                "Logout User",
                "123",
                new Role("CITIZEN", "Citizen"),
                null,
                null
        );
        userJdbcRepository.save(user);

        Authentication auth = new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());

        mockMvc.perform(post("/api/auth/logout")
                        .principal(auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message", containsString("Logged out successfully")));
    }

    @Test
    @DisplayName("11. Update Profile via JDBC: parameterized UPDATE query executes successfully")
    void testUpdateProfile() throws Exception {
        User user = new User(
                "profile_user",
                "profile@city.gov",
                passwordEncoder.encode("Pass123!"),
                "Old FullName",
                "000-0000",
                new Role("CITIZEN", "Citizen"),
                null,
                null
        );
        userJdbcRepository.save(user);

        Authentication auth = new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());

        UpdateProfileRequest updateReq = new UpdateProfileRequest("New FullName", "+1-800-CITY-OPS");

        mockMvc.perform(put("/api/auth/profile")
                        .principal(auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fullName", is("New FullName")))
                .andExpect(jsonPath("$.phone", is("+1-800-CITY-OPS")));

        // Verify database persistence via JDBC
        User updatedDb = userJdbcRepository.findById(user.getId()).orElseThrow();
        assertEquals("New FullName", updatedDb.getFullName());
        assertEquals("+1-800-CITY-OPS", updatedDb.getPhone());
    }

    @Test
    @DisplayName("12. Change Password via JDBC: verifies current password, hashes new password with BCrypt")
    void testChangePassword() throws Exception {
        String oldPw = "OldPassword@123";
        String newPw = "NewSecurePassword@456";

        User user = new User(
                "pw_user",
                "pw@city.gov",
                passwordEncoder.encode(oldPw),
                "Password User",
                "111",
                new Role("CITIZEN", "Citizen"),
                null,
                null
        );
        userJdbcRepository.save(user);

        Authentication auth = new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());

        ChangePasswordRequest changeReq = new ChangePasswordRequest(oldPw, newPw);

        mockMvc.perform(put("/api/auth/change-password")
                        .principal(auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(changeReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message", containsString("successfully")));

        // Verify database has new BCrypt hash
        User updatedDb = userJdbcRepository.findById(user.getId()).orElseThrow();
        assertTrue(passwordEncoder.matches(newPw, updatedDb.getPasswordHash()));
        assertFalse(passwordEncoder.matches(oldPw, updatedDb.getPasswordHash()));
    }
}

