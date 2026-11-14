package com.smartcity.security;

import com.smartcity.common.AuditLog;
import com.smartcity.common.AuditLogRepository;
import com.smartcity.common.DuplicateResourceException;
import com.smartcity.department.DepartmentRepository;
import com.smartcity.department.TeamRepository;
import com.smartcity.security.dto.AuthResponse;
import com.smartcity.security.dto.LoginRequest;
import com.smartcity.security.dto.RegisterRequest;
import com.smartcity.security.dto.UserSummaryResponse;
import com.smartcity.user.Role;
import com.smartcity.user.RoleRepository;
import com.smartcity.user.User;
import com.smartcity.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService Unit Tests")
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private DepartmentRepository departmentRepository;

    @Mock
    private TeamRepository teamRepository;

    @Mock
    private AuditLogRepository auditLogRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuthenticationManager authenticationManager;

    private JwtUtils jwtUtils;
    private AuthServiceImpl authService;

    private Role citizenRole;
    private User testUser;

    @BeforeEach
    void setUp() {
        jwtUtils = new JwtUtils();
        ReflectionTestUtils.setField(jwtUtils, "jwtSecret", "c3VwZXJzZWNyZXRqd3RzZWNyZXRrZXlmb3JzbWFydGNpdHljb21tYW5kY2VudHJlY2l2aWNwdWxzZQ==");
        ReflectionTestUtils.setField(jwtUtils, "jwtExpirationMs", 86400000L);

        authService = new AuthServiceImpl(
                userRepository,
                roleRepository,
                departmentRepository,
                teamRepository,
                auditLogRepository,
                passwordEncoder,
                authenticationManager,
                jwtUtils
        );

        citizenRole = new Role("CITIZEN", "Citizen user role");
        citizenRole.setId(UUID.randomUUID());

        testUser = new User(
                "john_doe",
                "john@example.com",
                "$2a$10$hashedpassword",
                "John Doe",
                "1234567890",
                citizenRole,
                null,
                null
        );
        testUser.setId(UUID.randomUUID());
    }

    @Test
    @DisplayName("register successfully encodes password, creates citizen user, and creates audit log")
    void testRegisterSuccess() {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("john_doe");
        request.setEmail("john@example.com");
        request.setPassword("SecretPass@2026");
        request.setFullName("John Doe");
        request.setPhone("1234567890");

        when(userRepository.existsByUsername("john_doe")).thenReturn(false);
        when(userRepository.existsByEmail("john@example.com")).thenReturn(false);
        when(roleRepository.findByName("CITIZEN")).thenReturn(Optional.of(citizenRole));
        when(passwordEncoder.encode("SecretPass@2026")).thenReturn("$2a$10$hashedpassword");
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        UserSummaryResponse response = authService.register(request);

        assertNotNull(response);
        assertEquals("john_doe", response.getUsername());
        assertEquals("john@example.com", response.getEmail());
        assertEquals("CITIZEN", response.getRole());

        verify(passwordEncoder).encode("SecretPass@2026");
        verify(userRepository).save(any(User.class));
        verify(auditLogRepository).save(any(AuditLog.class));
    }

    @Test
    @DisplayName("register throws DuplicateResourceException when username exists")
    void testRegisterDuplicateUsername() {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("john_doe");
        request.setEmail("john@example.com");

        when(userRepository.existsByUsername("john_doe")).thenReturn(true);

        assertThrows(DuplicateResourceException.class, () -> authService.register(request));
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("register throws DuplicateResourceException when email exists")
    void testRegisterDuplicateEmail() {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("john_doe");
        request.setEmail("john@example.com");

        when(userRepository.existsByUsername("john_doe")).thenReturn(false);
        when(userRepository.existsByEmail("john@example.com")).thenReturn(true);

        assertThrows(DuplicateResourceException.class, () -> authService.register(request));
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("login successfully authenticates, generates token, and logs success")
    void testLoginSuccess() {
        LoginRequest request = new LoginRequest("john_doe", "SecretPass@2026");
        Authentication auth = new UsernamePasswordAuthenticationToken(testUser, "SecretPass@2026", testUser.getAuthorities());

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(auth);

        AuthResponse response = authService.login(request, "127.0.0.1");

        assertNotNull(response);
        assertNotNull(response.getToken());
        assertEquals("Bearer", response.getTokenType());
        assertEquals(86400000L, response.getExpiresIn());
        assertEquals("john_doe", response.getUser().getUsername());

        verify(auditLogRepository).save(argThat(log -> "LOGIN_SUCCESS".equals(log.getAction())));
    }

    @Test
    @DisplayName("login with invalid credentials logs failure and throws BadCredentialsException")
    void testLoginFailure() {
        LoginRequest request = new LoginRequest("john_doe", "WrongPass");

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        assertThrows(BadCredentialsException.class, () -> authService.login(request, "127.0.0.1"));
        verify(auditLogRepository).save(argThat(log -> "LOGIN_FAILURE".equals(log.getAction())));
    }

    @Test
    @DisplayName("getCurrentUser throws BadCredentialsException if unauthenticated")
    void testGetCurrentUserUnauthenticated() {
        assertThrows(BadCredentialsException.class, () -> authService.getCurrentUser(null));

        Authentication unauth = new UsernamePasswordAuthenticationToken("anon", "anon");
        assertThrows(BadCredentialsException.class, () -> authService.getCurrentUser(unauth));
    }

    @Test
    @DisplayName("getCurrentUser returns user summary without sensitive fields")
    void testGetCurrentUserSuccess() {
        Authentication auth = new UsernamePasswordAuthenticationToken(testUser, null, testUser.getAuthorities());
        when(userRepository.findByUsername("john_doe")).thenReturn(Optional.of(testUser));

        UserSummaryResponse response = authService.getCurrentUser(auth);

        assertNotNull(response);
        assertEquals("john_doe", response.getUsername());
        assertEquals("CITIZEN", response.getRole());
    }
}
