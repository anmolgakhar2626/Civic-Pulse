package com.smartcity.security;

import com.smartcity.common.AuditLog;
import com.smartcity.common.AuditLogRepository;
import com.smartcity.department.Department;
import com.smartcity.department.DepartmentRepository;
import com.smartcity.department.Team;
import com.smartcity.department.TeamRepository;
import com.smartcity.security.dto.*;
import com.smartcity.security.otp.*;
import com.smartcity.security.otp.dto.OtpResponse;
import com.smartcity.security.otp.dto.SendOtpRequest;
import com.smartcity.security.otp.dto.VerifyOtpRequest;
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
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Phase 9: Citizen Mobile OTP Login & Security Tests")
class LoginOtpSecurityIntegrationTest {

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

    @Mock
    private MobileOtpRepository mobileOtpRepository;

    private MockDevSmsGatewayProvider smsGatewayProvider;
    private OtpServiceImpl otpService;
    private JwtUtils jwtUtils;
    private AuthServiceImpl authService;

    private User citizenUser;
    private User adminUser;
    private User officerUser;
    private User workerUser;
    private User unverifiedCitizen;
    private User disabledCitizen;

    private final String CITIZEN_MOBILE = "+919876543210";
    private final String UNKNOWN_MOBILE = "+919999999999";

    @BeforeEach
    void setUp() {
        jwtUtils = new JwtUtils();
        ReflectionTestUtils.setField(jwtUtils, "jwtSecret", "c3VwZXJzZWNyZXRqd3RzZWNyZXRrZXlmb3JzbWFydGNpdHljb21tYW5kY2VudHJlY2l2aWNwdWxzZQ==");
        ReflectionTestUtils.setField(jwtUtils, "jwtExpirationMs", 86400000L);

        smsGatewayProvider = new MockDevSmsGatewayProvider();
        otpService = new OtpServiceImpl(mobileOtpRepository, smsGatewayProvider, userRepository);

        authService = new AuthServiceImpl(
                userRepository,
                roleRepository,
                departmentRepository,
                teamRepository,
                auditLogRepository,
                passwordEncoder,
                authenticationManager,
                jwtUtils,
                otpService
        );

        // 1. Valid Active Verified Citizen
        Role roleCitizen = new Role("CITIZEN", "Citizen Role");
        citizenUser = new User("citizen_ramesh", "ramesh@example.com", "$2a$10$hash", "Ramesh Kumar", CITIZEN_MOBILE, roleCitizen, null, null);
        citizenUser.setId(UUID.randomUUID());
        citizenUser.setMobileNumber(CITIZEN_MOBILE);
        citizenUser.setIsMobileVerified(true);
        citizenUser.setEnabled(true);
        citizenUser.setActive(true);

        // 2. Municipal Admin
        Role roleAdmin = new Role("ADMIN", "Admin Role");
        adminUser = new User("admin_user", "admin@smartcity.gov", "$2a$10$hash", "System Admin", "+919876500001", roleAdmin, null, null);
        adminUser.setId(UUID.randomUUID());
        adminUser.setIsMobileVerified(true);
        adminUser.setEnabled(true);
        adminUser.setActive(true);

        // 3. Department Officer
        Role roleOfficer = new Role("DEPARTMENT_OFFICER", "Officer Role");
        officerUser = new User("officer_traffic", "traffic@smartcity.gov", "$2a$10$hash", "Traffic Officer", "+919876500002", roleOfficer, null, null);
        officerUser.setId(UUID.randomUUID());
        officerUser.setIsMobileVerified(true);
        officerUser.setEnabled(true);
        officerUser.setActive(true);

        // 4. Field Worker
        Role roleWorker = new Role("FIELD_WORKER", "Worker Role");
        workerUser = new User("field_worker_1", "worker@smartcity.gov", "$2a$10$hash", "Field Worker", "+919876500003", roleWorker, null, null);
        workerUser.setId(UUID.randomUUID());
        workerUser.setIsMobileVerified(true);
        workerUser.setEnabled(true);
        workerUser.setActive(true);

        // 5. Unverified Citizen
        unverifiedCitizen = new User("citizen_unverified", "unverified@example.com", "$2a$10$hash", "Unverified Citizen", "+919876500004", roleCitizen, null, null);
        unverifiedCitizen.setId(UUID.randomUUID());
        unverifiedCitizen.setIsMobileVerified(false);
        unverifiedCitizen.setEnabled(true);
        unverifiedCitizen.setActive(true);

        // 6. Disabled Citizen (active=false or enabled=false)
        disabledCitizen = new User("citizen_disabled", "disabled@example.com", "$2a$10$hash", "Disabled Citizen", "+919876500005", roleCitizen, null, null);
        disabledCitizen.setId(UUID.randomUUID());
        disabledCitizen.setIsMobileVerified(true);
        disabledCitizen.setEnabled(false);
        disabledCitizen.setActive(false);
    }

    @Test
    @DisplayName("1. Existing verified citizen can request LOGIN OTP successfully")
    void test1_sendLoginOtp_verifiedCitizen_success() {
        when(userRepository.findByMobileNumber(CITIZEN_MOBILE)).thenReturn(Optional.of(citizenUser));
        when(mobileOtpRepository.countRecentRequests(eq(CITIZEN_MOBILE), any(Instant.class))).thenReturn(0);
        when(mobileOtpRepository.findLatestByMobile(eq(CITIZEN_MOBILE), eq("LOGIN"))).thenReturn(Optional.empty());

        LoginOtpSendRequest request = new LoginOtpSendRequest(CITIZEN_MOBILE);
        OtpResponse response = authService.sendLoginOtp(request);

        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getCooldownSeconds()).isEqualTo(60);
        assertThat(response.getExpiresInSeconds()).isEqualTo(300);

        // Assert OTP was generated and saved with purpose 'LOGIN'
        verify(mobileOtpRepository).save(argThat(otp ->
                otp.getPurpose().equals("LOGIN") &&
                otp.getMobileNumber().equals(CITIZEN_MOBILE) &&
                otp.getUserId().equals(citizenUser.getId())
        ));

        // Assert local demo gateway dispatched OTP
        String lastOtp = smsGatewayProvider.getLastSentOtp(CITIZEN_MOBILE);
        assertThat(lastOtp).isNotEmpty().matches("^[0-9]{6}$");
    }

    @Test
    @DisplayName("2. Unknown mobile number returns anti-enumeration message without generating OTP")
    void test2_sendLoginOtp_unknownNumber_antiEnumeration() {
        when(userRepository.findByMobileNumber(UNKNOWN_MOBILE)).thenReturn(Optional.empty());

        LoginOtpSendRequest request = new LoginOtpSendRequest(UNKNOWN_MOBILE);
        OtpResponse response = authService.sendLoginOtp(request);

        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getMessage()).contains("If this mobile number is registered with an active citizen account");
        verify(mobileOtpRepository, never()).save(any(MobileOtp.class));
        assertThat(smsGatewayProvider.getLastSentOtp(UNKNOWN_MOBILE)).isNull();
    }

    @Test
    @DisplayName("3. Unverified citizen account cannot request LOGIN OTP")
    void test3_sendLoginOtp_unverifiedCitizen_rejected() {
        when(userRepository.findByMobileNumber(unverifiedCitizen.getMobileNumber())).thenReturn(Optional.of(unverifiedCitizen));

        LoginOtpSendRequest request = new LoginOtpSendRequest(unverifiedCitizen.getMobileNumber());
        OtpResponse response = authService.sendLoginOtp(request);

        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getMessage()).contains("If this mobile number is registered with an active citizen account");
        verify(mobileOtpRepository, never()).save(any(MobileOtp.class));
    }

    @Test
    @DisplayName("4. Disabled citizen account cannot request LOGIN OTP")
    void test4_sendLoginOtp_disabledAccount_rejected() {
        when(userRepository.findByMobileNumber(disabledCitizen.getMobileNumber())).thenReturn(Optional.of(disabledCitizen));

        LoginOtpSendRequest request = new LoginOtpSendRequest(disabledCitizen.getMobileNumber());
        OtpResponse response = authService.sendLoginOtp(request);

        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getMessage()).contains("If this mobile number is registered with an active citizen account");
        verify(mobileOtpRepository, never()).save(any(MobileOtp.class));
    }

    @Test
    @DisplayName("5. Admin account cannot request Citizen LOGIN OTP")
    void test5_sendLoginOtp_adminUser_rejected() {
        when(userRepository.findByMobileNumber(adminUser.getMobileNumber())).thenReturn(Optional.of(adminUser));

        LoginOtpSendRequest request = new LoginOtpSendRequest(adminUser.getMobileNumber());
        OtpResponse response = authService.sendLoginOtp(request);

        assertThat(response.isSuccess()).isTrue();
        verify(mobileOtpRepository, never()).save(any(MobileOtp.class));
    }

    @Test
    @DisplayName("6. Department officer account cannot request Citizen LOGIN OTP")
    void test6_sendLoginOtp_officerUser_rejected() {
        when(userRepository.findByMobileNumber(officerUser.getMobileNumber())).thenReturn(Optional.of(officerUser));

        LoginOtpSendRequest request = new LoginOtpSendRequest(officerUser.getMobileNumber());
        OtpResponse response = authService.sendLoginOtp(request);

        assertThat(response.isSuccess()).isTrue();
        verify(mobileOtpRepository, never()).save(any(MobileOtp.class));
    }

    @Test
    @DisplayName("7. Field worker account cannot request Citizen LOGIN OTP")
    void test7_sendLoginOtp_fieldWorkerUser_rejected() {
        when(userRepository.findByMobileNumber(workerUser.getMobileNumber())).thenReturn(Optional.of(workerUser));

        LoginOtpSendRequest request = new LoginOtpSendRequest(workerUser.getMobileNumber());
        OtpResponse response = authService.sendLoginOtp(request);

        assertThat(response.isSuccess()).isTrue();
        verify(mobileOtpRepository, never()).save(any(MobileOtp.class));
    }

    @Test
    @DisplayName("8. Correct LOGIN OTP returns valid signed JWT and AuthResponse")
    void test8_verifyLoginOtp_validOtp_returnsValidJwt() {
        when(userRepository.findByMobileNumber(CITIZEN_MOBILE)).thenReturn(Optional.of(citizenUser));

        String rawOtp = "654321";
        // Calculate hash using reflection on internal hash method or simulating setup
        String otpHash = ReflectionTestUtils.invokeMethod(otpService, "hashOtp", rawOtp, CITIZEN_MOBILE);

        MobileOtp activeOtp = new MobileOtp(
                CITIZEN_MOBILE,
                citizenUser.getId(),
                otpHash,
                "LOGIN",
                3,
                Instant.now().plusSeconds(60),
                Instant.now().plusSeconds(300)
        );
        activeOtp.setId(UUID.randomUUID());

        when(mobileOtpRepository.findLatestActiveOtp(CITIZEN_MOBILE, "LOGIN")).thenReturn(Optional.of(activeOtp));
        when(mobileOtpRepository.markUsedAtomic(activeOtp.getId())).thenReturn(true);

        LoginOtpVerifyRequest verifyReq = new LoginOtpVerifyRequest(CITIZEN_MOBILE, rawOtp);
        AuthResponse authResponse = authService.loginWithOtp(verifyReq, "127.0.0.1");

        assertThat(authResponse).isNotNull();
        assertThat(authResponse.getToken()).isNotEmpty();
        assertThat(authResponse.getUser()).isNotNull();
        assertThat(authResponse.getUser().getUsername()).isEqualTo("citizen_ramesh");
        assertThat(authResponse.getUser().getRole()).isEqualTo("CITIZEN");

        // Verify audit log LOGIN_SUCCESS recorded
        verify(auditLogRepository).save(argThat(log ->
                "AUTH".equals(log.getEntityType()) &&
                "LOGIN_SUCCESS".equals(log.getAction()) &&
                log.getDetailsJson().contains("MOBILE_OTP")
        ));
    }

    @Test
    @DisplayName("9. Incorrect LOGIN OTP is rejected and remaining attempts are reported")
    void test9_verifyLoginOtp_incorrectOtp_rejected() {
        when(userRepository.findByMobileNumber(CITIZEN_MOBILE)).thenReturn(Optional.of(citizenUser));

        String realOtp = "654321";
        String otpHash = ReflectionTestUtils.invokeMethod(otpService, "hashOtp", realOtp, CITIZEN_MOBILE);

        MobileOtp activeOtp = new MobileOtp(
                CITIZEN_MOBILE,
                citizenUser.getId(),
                otpHash,
                "LOGIN",
                3,
                Instant.now().plusSeconds(60),
                Instant.now().plusSeconds(300)
        );
        activeOtp.setId(UUID.randomUUID());

        when(mobileOtpRepository.findLatestActiveOtp(CITIZEN_MOBILE, "LOGIN")).thenReturn(Optional.of(activeOtp));

        LoginOtpVerifyRequest verifyReq = new LoginOtpVerifyRequest(CITIZEN_MOBILE, "000000");

        BadCredentialsException ex = assertThrows(BadCredentialsException.class, () ->
                authService.loginWithOtp(verifyReq, "127.0.0.1"));

        assertThat(ex.getMessage()).contains("Invalid verification code");
        verify(mobileOtpRepository).incrementAttemptsAtomic(activeOtp.getId());
        verify(mobileOtpRepository, never()).markUsedAtomic(any(UUID.class));
    }

    @Test
    @DisplayName("10. Expired LOGIN OTP is rejected")
    void test10_verifyLoginOtp_expiredOtp_rejected() {
        when(userRepository.findByMobileNumber(CITIZEN_MOBILE)).thenReturn(Optional.of(citizenUser));

        MobileOtp expiredOtp = new MobileOtp(
                CITIZEN_MOBILE,
                citizenUser.getId(),
                "ANY_HASH",
                "LOGIN",
                3,
                Instant.now().minusSeconds(120),
                Instant.now().minusSeconds(10) // Expired 10s ago
        );
        expiredOtp.setId(UUID.randomUUID());

        when(mobileOtpRepository.findLatestActiveOtp(CITIZEN_MOBILE, "LOGIN")).thenReturn(Optional.of(expiredOtp));

        LoginOtpVerifyRequest verifyReq = new LoginOtpVerifyRequest(CITIZEN_MOBILE, "123456");

        BadCredentialsException ex = assertThrows(BadCredentialsException.class, () ->
                authService.loginWithOtp(verifyReq, "127.0.0.1"));

        assertThat(ex.getMessage()).contains("expired");
    }

    @Test
    @DisplayName("11. Reused LOGIN OTP is rejected (single-use protection)")
    void test11_verifyLoginOtp_reusedOtp_rejected() {
        when(userRepository.findByMobileNumber(CITIZEN_MOBILE)).thenReturn(Optional.of(citizenUser));
        when(mobileOtpRepository.findLatestActiveOtp(CITIZEN_MOBILE, "LOGIN")).thenReturn(Optional.empty());

        LoginOtpVerifyRequest verifyReq = new LoginOtpVerifyRequest(CITIZEN_MOBILE, "123456");

        BadCredentialsException ex = assertThrows(BadCredentialsException.class, () ->
                authService.loginWithOtp(verifyReq, "127.0.0.1"));

        assertThat(ex.getMessage()).contains("No active OTP found");
    }

    @Test
    @DisplayName("12. Registration OTP cannot authenticate LOGIN (Purpose Isolation)")
    void test12_registrationOtpCannotAuthenticateLogin() {
        when(userRepository.findByMobileNumber(CITIZEN_MOBILE)).thenReturn(Optional.of(citizenUser));

        // Active OTP with purpose REGISTRATION exists, but LOGIN OTP does not
        when(mobileOtpRepository.findLatestActiveOtp(CITIZEN_MOBILE, "LOGIN")).thenReturn(Optional.empty());

        LoginOtpVerifyRequest verifyReq = new LoginOtpVerifyRequest(CITIZEN_MOBILE, "123456");

        BadCredentialsException ex = assertThrows(BadCredentialsException.class, () ->
                authService.loginWithOtp(verifyReq, "127.0.0.1"));

        assertThat(ex.getMessage()).contains("No active OTP found");
    }

    @Test
    @DisplayName("13. Login OTP cannot complete REGISTRATION (Purpose Isolation)")
    void test13_loginOtpCannotCompleteRegistration() {
        // Active OTP exists with purpose LOGIN only
        when(mobileOtpRepository.findLatestActiveOtp(CITIZEN_MOBILE, "REGISTRATION")).thenReturn(Optional.empty());

        boolean validForRegistration = otpService.validateAndConsumeOtp(CITIZEN_MOBILE, "123456", "REGISTRATION");
        assertThat(validForRegistration).isFalse();
    }

    @Test
    @DisplayName("14. Hourly Rate Limiting is enforced (Max 5 per hour)")
    void test14_sendLoginOtp_hourlyRateLimitEnforced() {
        when(userRepository.findByMobileNumber(CITIZEN_MOBILE)).thenReturn(Optional.of(citizenUser));
        when(mobileOtpRepository.countRecentRequests(eq(CITIZEN_MOBILE), any(Instant.class))).thenReturn(5);

        LoginOtpSendRequest request = new LoginOtpSendRequest(CITIZEN_MOBILE);
        OtpResponse response = authService.sendLoginOtp(request);

        assertThat(response.isSuccess()).isFalse();
        assertThat(response.getMessage()).contains("OTP request limit reached");
        verify(mobileOtpRepository, never()).save(any(MobileOtp.class));
    }

    @Test
    @DisplayName("15. 60-Second Resend Cooldown is enforced")
    void test15_sendLoginOtp_resendCooldownEnforced() {
        when(userRepository.findByMobileNumber(CITIZEN_MOBILE)).thenReturn(Optional.of(citizenUser));
        when(mobileOtpRepository.countRecentRequests(eq(CITIZEN_MOBILE), any(Instant.class))).thenReturn(1);

        MobileOtp recentOtp = new MobileOtp(
                CITIZEN_MOBILE,
                citizenUser.getId(),
                "RECENT_HASH",
                "LOGIN",
                3,
                Instant.now().plusSeconds(45), // 45s remaining cooldown
                Instant.now().plusSeconds(280)
        );

        when(mobileOtpRepository.findLatestByMobile(eq(CITIZEN_MOBILE), eq("LOGIN"))).thenReturn(Optional.of(recentOtp));

        LoginOtpSendRequest request = new LoginOtpSendRequest(CITIZEN_MOBILE);
        OtpResponse response = authService.sendLoginOtp(request);

        assertThat(response.isSuccess()).isFalse();
        assertThat(response.getMessage()).contains("Please wait");
        verify(mobileOtpRepository, never()).save(any(MobileOtp.class));
    }

    @Test
    @DisplayName("16. Maximum 3 failed attempts locks the OTP code")
    void test16_verifyLoginOtp_maxFailedAttemptsLocksOtp() {
        when(userRepository.findByMobileNumber(CITIZEN_MOBILE)).thenReturn(Optional.of(citizenUser));

        MobileOtp lockedOtp = new MobileOtp(
                CITIZEN_MOBILE,
                citizenUser.getId(),
                "SOME_HASH",
                "LOGIN",
                3,
                Instant.now().plusSeconds(60),
                Instant.now().plusSeconds(250)
        );
        lockedOtp.setAttemptCount(3); // Already locked!

        when(mobileOtpRepository.findLatestActiveOtp(CITIZEN_MOBILE, "LOGIN")).thenReturn(Optional.of(lockedOtp));

        LoginOtpVerifyRequest verifyReq = new LoginOtpVerifyRequest(CITIZEN_MOBILE, "123456");

        BadCredentialsException ex = assertThrows(BadCredentialsException.class, () ->
                authService.loginWithOtp(verifyReq, "127.0.0.1"));

        assertThat(ex.getMessage()).contains("Maximum verification attempts exceeded");
    }

    @Test
    @DisplayName("17. Existing Password Login remains fully functional and unchanged")
    void test17_existingPasswordLogin_remainsFunctional() {
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(adminUser));

        Authentication auth = new UsernamePasswordAuthenticationToken(adminUser, "Admin@CivicPulse2026", adminUser.getAuthorities());
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(auth);

        LoginRequest req = new LoginRequest("admin", "Admin@CivicPulse2026");
        AuthResponse response = authService.login(req, "127.0.0.1");

        assertThat(response).isNotNull();
        assertThat(response.getToken()).isNotEmpty();
        assertThat(response.getUser().getUsername()).isEqualTo("admin_user");
    }

    @Test
    @DisplayName("18. Existing Registration workflow remains fully functional")
    void test18_existingRegistrationWorkflow_remainsFunctional() {
        RegisterRequest registerReq = new RegisterRequest();
        registerReq.setUsername("new_citizen_2026");
        registerReq.setEmail("new_citizen_2026@smartcity.gov");
        registerReq.setPassword("Password@2026");
        registerReq.setFullName("New Citizen");
        registerReq.setMobileNumber("+919123456789");
        registerReq.setRole("CITIZEN");

        when(userRepository.existsByUsername("new_citizen_2026")).thenReturn(false);
        when(userRepository.existsByEmail("new_citizen_2026@smartcity.gov")).thenReturn(false);
        when(roleRepository.findByName("CITIZEN")).thenReturn(Optional.of(citizenUser.getRole()));
        when(passwordEncoder.encode(anyString())).thenReturn("$2a$10$encodedPassword");

        User savedUser = new User("new_citizen_2026", "new_citizen_2026@smartcity.gov", "$2a$10$encodedPassword", "New Citizen", "+919123456789", citizenUser.getRole(), null, null);
        savedUser.setId(UUID.randomUUID());
        savedUser.setIsMobileVerified(false);

        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        UserSummaryResponse response = authService.register(registerReq);

        assertThat(response).isNotNull();
        assertThat(response.getUsername()).isEqualTo("new_citizen_2026");
        assertThat(response.getRole()).isEqualTo("CITIZEN");
    }
}
