package com.smartcity.security;

import com.smartcity.common.AuditLog;
import com.smartcity.common.AuditLogRepository;
import com.smartcity.common.DuplicateResourceException;
import com.smartcity.common.ResourceNotFoundException;
import com.smartcity.department.Department;
import com.smartcity.department.DepartmentRepository;
import com.smartcity.department.Team;
import com.smartcity.department.TeamRepository;
import com.smartcity.security.dto.*;
import com.smartcity.user.Role;
import com.smartcity.user.RoleRepository;
import com.smartcity.user.User;
import com.smartcity.user.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class AuthServiceImpl implements AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthServiceImpl.class);

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final DepartmentRepository departmentRepository;
    private final TeamRepository teamRepository;
    private final AuditLogRepository auditLogRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtUtils jwtUtils;
    private final com.smartcity.security.otp.OtpService otpService;

    public AuthServiceImpl(
            UserRepository userRepository,
            RoleRepository roleRepository,
            DepartmentRepository departmentRepository,
            TeamRepository teamRepository,
            AuditLogRepository auditLogRepository,
            PasswordEncoder passwordEncoder,
            AuthenticationManager authenticationManager,
            JwtUtils jwtUtils
    ) {
        this(userRepository, roleRepository, departmentRepository, teamRepository,
                auditLogRepository, passwordEncoder, authenticationManager, jwtUtils, null);
    }

    @org.springframework.beans.factory.annotation.Autowired
    public AuthServiceImpl(
            UserRepository userRepository,
            RoleRepository roleRepository,
            DepartmentRepository departmentRepository,
            TeamRepository teamRepository,
            AuditLogRepository auditLogRepository,
            PasswordEncoder passwordEncoder,
            AuthenticationManager authenticationManager,
            JwtUtils jwtUtils,
            @org.springframework.beans.factory.annotation.Autowired(required = false) com.smartcity.security.otp.OtpService otpService
    ) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.departmentRepository = departmentRepository;
        this.teamRepository = teamRepository;
        this.auditLogRepository = auditLogRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtUtils = jwtUtils;
        this.otpService = otpService;
    }

    @Override
    @Transactional
    public UserSummaryResponse register(RegisterRequest request) {
        String username = request.getUsername();
        if (username == null || username.trim().isEmpty()) {
            username = request.getEmail();
        } else {
            username = username.trim();
        }

        if (userRepository.existsByUsername(username)) {
            throw new DuplicateResourceException("User", "username", username);
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("User", "email", request.getEmail());
        }

        String roleName = request.getRole() != null && !request.getRole().trim().isEmpty()
                ? request.getRole().trim().toUpperCase()
                : "CITIZEN";

        if ("ADMIN".equalsIgnoreCase(roleName) || "ROLE_ADMIN".equalsIgnoreCase(roleName)) {
            log.warn("Attempt to register with forbidden ADMIN role rejected for username '{}', email '{}'", username, request.getEmail());
            throw new IllegalArgumentException("Registration with ADMIN role is not permitted.");
        }

        Role role = null;
        if (roleRepository != null) {
            role = roleRepository.findByName(roleName).orElse(null);
        }
        if (role == null) {
            role = new Role(roleName, roleName + " municipal role");
        }

        Department department = null;
        if (request.getDepartmentId() != null && departmentRepository != null) {
            department = departmentRepository.findById(request.getDepartmentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Department", "id", request.getDepartmentId()));
        }

        Team team = null;
        if (request.getTeamId() != null && teamRepository != null) {
            team = teamRepository.findById(request.getTeamId())
                    .orElseThrow(() -> new ResourceNotFoundException("Team", "id", request.getTeamId()));
        }

        // BCrypt hashing of raw password
        String hashedPassword = passwordEncoder.encode(request.getPassword());

        String phoneOrMobile = request.getMobileNumber() != null && !request.getMobileNumber().isBlank()
                ? request.getMobileNumber().trim()
                : request.getPhone();

        User user = new User(
                username,
                request.getEmail().trim().toLowerCase(),
                hashedPassword,
                request.getFullName(),
                phoneOrMobile,
                role,
                department,
                team
        );
        user.setRoleName(roleName);
        user.setEnabled(true);
        user.setActive(true);
        user.setMobileNumber(phoneOrMobile);

        // If OTP provided during registration, validate and verify mobile
        if (request.getOtp() != null && !request.getOtp().isBlank() && phoneOrMobile != null) {
            if (otpService != null) {
                boolean verified = otpService.validateAndConsumeOtp(phoneOrMobile, request.getOtp(), "REGISTRATION");
                if (!verified) {
                    throw new IllegalArgumentException("Invalid or expired mobile verification code.");
                }
                user.setIsMobileVerified(true);
            } else {
                user.setIsMobileVerified(true);
            }
        } else {
            // Default: unverified until OTP verification completed
            user.setIsMobileVerified(false);
        }

        User savedUser = userRepository.save(user);

        // Security Audit Logging
        if (auditLogRepository != null) {
            AuditLog auditLog = new AuditLog(
                    "USER",
                    savedUser.getId().toString(),
                    "USER_REGISTERED",
                    savedUser.getUsername(),
                    String.format("{\"username\":\"%s\",\"role\":\"%s\",\"email\":\"%s\"}",
                            savedUser.getUsername(), roleName, savedUser.getEmail()),
                    null
            );
            auditLogRepository.save(auditLog);
        }

        log.info("Registered new user '{}' with role '{}'", savedUser.getUsername(), roleName);
        return UserSummaryResponse.fromEntity(savedUser);
    }

    @Override
    @Transactional
    public AuthResponse login(LoginRequest request, String ipAddress) {
        String identifier = request.getUsername() != null ? request.getUsername().trim() : "";

        // Check account disabled status prior to authentication
        Optional<User> potentialUser = userRepository.findByUsername(identifier)
                .or(() -> userRepository.findByEmail(identifier))
                .or(() -> userRepository.findByUsernameOrEmail(identifier));
        if (potentialUser.isPresent() && !potentialUser.get().isEnabled()) {
            log.warn("Login attempt for disabled account '{}' from IP {}", identifier, ipAddress);
            throw new DisabledException("User account is disabled. Contact your administrator.");
        }

        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(identifier, request.getPassword())
            );

            User user = (User) authentication.getPrincipal();
            if (!user.isEnabled()) {
                throw new DisabledException("User account is disabled. Contact your administrator.");
            }

            String jwt = jwtUtils.generateJwtToken(authentication);

            // Audit log success
            if (auditLogRepository != null) {
                AuditLog auditLog = new AuditLog(
                        "AUTH",
                        user.getId().toString(),
                        "LOGIN_SUCCESS",
                        user.getUsername(),
                        "{\"status\":\"SUCCESS\"}",
                        ipAddress
                );
                auditLogRepository.save(auditLog);
            }

            log.info("User '{}' successfully logged in from IP {}", user.getUsername(), ipAddress);

            return new AuthResponse(
                    jwt,
                    jwtUtils.getExpirationMs(),
                    UserSummaryResponse.fromEntity(user)
            );
        } catch (DisabledException e) {
            throw e;
        } catch (AuthenticationException e) {
            // Audit log failure
            if (auditLogRepository != null) {
                AuditLog auditLog = new AuditLog(
                        "AUTH",
                        null,
                        "LOGIN_FAILURE",
                        identifier,
                        String.format("{\"error\":\"%s\"}", e.getMessage()),
                        ipAddress
                );
                auditLogRepository.save(auditLog);
            }

            log.warn("Failed login attempt for user '{}' from IP {}: {}", identifier, ipAddress, e.getMessage());
            throw new BadCredentialsException("Invalid username or password");
        }
    }

    @Override
    @Transactional
    public com.smartcity.security.otp.dto.OtpResponse sendLoginOtp(LoginOtpSendRequest request) {
        String rawMobile = request != null ? request.getMobileNumber() : "";
        String mobile = rawMobile != null ? rawMobile.replaceAll("\\s+", "").trim() : "";

        // Standard anti-enumeration response
        com.smartcity.security.otp.dto.OtpResponse antiEnumerationResponse =
                com.smartcity.security.otp.dto.OtpResponse.sent(
                        mobile,
                        60,
                        300,
                        "If this mobile number is registered with an active citizen account, a verification code has been dispatched."
                );

        if (mobile.isBlank() || otpService == null) {
            return antiEnumerationResponse;
        }

        // 1. Account Lookup by mobile number
        Optional<User> userOpt = userRepository.findByMobileNumber(mobile);
        if (userOpt.isEmpty()) {
            log.info("Login OTP requested for non-existent mobile number: {}", maskPhone(mobile));
            return antiEnumerationResponse;
        }

        User user = userOpt.get();

        // 2. Strict Role Isolation: CITIZEN accounts only
        String roleName = user.getRoleName() != null ? user.getRoleName() : (user.getRole() != null ? user.getRole().getName() : "");
        boolean isCitizen = "CITIZEN".equalsIgnoreCase(roleName) || "ROLE_CITIZEN".equalsIgnoreCase(roleName)
                || (user.getAuthorities() != null && user.getAuthorities().stream().anyMatch(a -> "ROLE_CITIZEN".equalsIgnoreCase(a.getAuthority())));

        if (!isCitizen) {
            log.warn("Non-citizen user '{}' (role: {}) attempted to request citizen login OTP for mobile {}",
                    user.getUsername(), roleName, maskPhone(mobile));
            return antiEnumerationResponse;
        }

        // 3. Mandatory Refinement 1: Check both user.active and user.enabled status
        if (!Boolean.TRUE.equals(user.getEnabled()) || !Boolean.TRUE.equals(user.getActive())) {
            log.warn("Login OTP requested for disabled/inactive citizen account '{}' for mobile {}",
                    user.getUsername(), maskPhone(mobile));
            return antiEnumerationResponse;
        }

        // 4. Require mobile number to be already verified
        if (!Boolean.TRUE.equals(user.getIsMobileVerified())) {
            log.warn("Login OTP requested for unverified mobile number for citizen account '{}'", user.getUsername());
            return antiEnumerationResponse;
        }

        // 5. Dispatch OTP via OtpService with purpose 'LOGIN'
        com.smartcity.security.otp.dto.SendOtpRequest sendReq =
                new com.smartcity.security.otp.dto.SendOtpRequest(mobile, "LOGIN");

        return otpService.sendOtp(sendReq, user.getId());
    }

    @Override
    @Transactional
    public AuthResponse loginWithOtp(LoginOtpVerifyRequest request, String ipAddress) {
        String rawMobile = request != null ? request.getMobileNumber() : "";
        String mobile = rawMobile != null ? rawMobile.replaceAll("\\s+", "").trim() : "";
        String candidateOtp = request != null && request.getOtp() != null ? request.getOtp().trim() : "";

        // 1. Account Lookup
        User user = userRepository.findByMobileNumber(mobile)
                .orElseThrow(() -> new BadCredentialsException("Unable to authenticate. Please check your registered mobile number."));

        // 2. Strict Role Isolation: CITIZEN accounts only
        String roleName = user.getRoleName() != null ? user.getRoleName() : (user.getRole() != null ? user.getRole().getName() : "");
        boolean isCitizen = "CITIZEN".equalsIgnoreCase(roleName) || "ROLE_CITIZEN".equalsIgnoreCase(roleName)
                || (user.getAuthorities() != null && user.getAuthorities().stream().anyMatch(a -> "ROLE_CITIZEN".equalsIgnoreCase(a.getAuthority())));

        if (!isCitizen) {
            log.warn("Non-citizen user '{}' (role: {}) attempted to complete mobile OTP login from IP {}",
                    user.getUsername(), roleName, ipAddress);
            recordAuditFailure(user.getId() != null ? user.getId().toString() : null, user.getUsername(),
                    "Mobile OTP login is restricted to Citizen accounts. Municipal officers must sign in with password.", ipAddress);
            throw new BadCredentialsException("Mobile OTP login is restricted to Citizen accounts. Municipal officers must sign in with password.");
        }

        // 3. Mandatory Refinement 1: Check both user.active and user.enabled status
        if (!Boolean.TRUE.equals(user.getEnabled()) || !Boolean.TRUE.equals(user.getActive())) {
            log.warn("OTP login attempt for disabled citizen account '{}' from IP {}", user.getUsername(), ipAddress);
            recordAuditFailure(user.getId() != null ? user.getId().toString() : null, user.getUsername(),
                    "User account is disabled. Contact your administrator.", ipAddress);
            throw new DisabledException("User account is disabled. Contact your administrator.");
        }

        // 4. Require mobile number to be verified
        if (!Boolean.TRUE.equals(user.getIsMobileVerified())) {
            log.warn("OTP login attempt for citizen account '{}' with unverified mobile from IP {}", user.getUsername(), ipAddress);
            recordAuditFailure(user.getId() != null ? user.getId().toString() : null, user.getUsername(),
                    "Mobile number is not verified. Please complete citizen verification.", ipAddress);
            throw new BadCredentialsException("Mobile number is not verified. Please complete citizen verification.");
        }

        // 5. Verify OTP atomically via OtpService (purpose 'LOGIN', replay protection, attempt limits)
        if (otpService == null) {
            throw new IllegalStateException("OTP Service is unavailable");
        }

        com.smartcity.security.otp.dto.OtpResponse verifyResult = otpService.verifyLoginOtp(mobile, candidateOtp);
        if (!verifyResult.isSuccess()) {
            recordAuditFailure(user.getId() != null ? user.getId().toString() : null, user.getUsername(),
                    verifyResult.getMessage(), ipAddress);
            throw new BadCredentialsException(verifyResult.getMessage());
        }

        // 6. Generate signed JWT token for the authenticated citizen
        String jwt = jwtUtils.generateTokenForUser(user);

        // 7. Mandatory Refinement 4: Record success audit log in existing AuditLogRepository
        if (auditLogRepository != null) {
            AuditLog auditLog = new AuditLog(
                    "AUTH",
                    user.getId() != null ? user.getId().toString() : null,
                    "LOGIN_SUCCESS",
                    user.getUsername(),
                    "{\"method\":\"MOBILE_OTP\"}",
                    ipAddress
            );
            auditLogRepository.save(auditLog);
        }

        log.info("Citizen user '{}' successfully authenticated via Mobile OTP from IP {}", user.getUsername(), ipAddress);

        return new AuthResponse(
                jwt,
                jwtUtils.getExpirationMs(),
                UserSummaryResponse.fromEntity(user)
        );
    }

    private void recordAuditFailure(String userId, String username, String errorMessage, String ipAddress) {
        if (auditLogRepository != null) {
            AuditLog auditLog = new AuditLog(
                    "AUTH",
                    userId,
                    "LOGIN_FAILURE",
                    username,
                    String.format("{\"error\":\"%s\",\"method\":\"MOBILE_OTP\"}", errorMessage),
                    ipAddress
            );
            auditLogRepository.save(auditLog);
        }
    }

    private String maskPhone(String phone) {
        if (phone == null || phone.length() < 6) return "***";
        return phone.substring(0, 3) + "******" + phone.substring(phone.length() - 2);
    }

    @Override
    @Transactional(readOnly = true)
    public UserSummaryResponse getCurrentUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new BadCredentialsException("User is not authenticated");
        }
        String identifier = authentication.getName();
        User user = userRepository.findByUsername(identifier)
                .or(() -> userRepository.findByEmail(identifier))
                .or(() -> userRepository.findByUsernameOrEmail(identifier))
                .orElseThrow(() -> new ResourceNotFoundException("User", "username", identifier));

        return UserSummaryResponse.fromEntity(user);
    }

    @Override
    @Transactional
    public void logout(Authentication authentication, String ipAddress) {
        String username = authentication != null ? authentication.getName() : "anonymous";
        if (auditLogRepository != null && authentication != null) {
            AuditLog auditLog = new AuditLog(
                    "AUTH",
                    null,
                    "LOGOUT",
                    username,
                    "{\"status\":\"LOGOUT_SUCCESS\"}",
                    ipAddress
            );
            auditLogRepository.save(auditLog);
        }
        log.info("User '{}' logged out from IP {}", username, ipAddress);
    }

    @Override
    @Transactional
    public UserSummaryResponse updateProfile(Authentication authentication, UpdateProfileRequest request) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new BadCredentialsException("User is not authenticated");
        }
        String identifier = authentication.getName();
        User user = userRepository.findByUsername(identifier)
                .or(() -> userRepository.findByEmail(identifier))
                .or(() -> userRepository.findByUsernameOrEmail(identifier))
                .orElseThrow(() -> new ResourceNotFoundException("User", "username", identifier));

        userRepository.updateProfile(user.getId(), request.getFullName(), request.getPhone());

        user.setFullName(request.getFullName());
        user.setPhone(request.getPhone());
        return UserSummaryResponse.fromEntity(user);
    }

    @Override
    @Transactional
    public void changePassword(Authentication authentication, ChangePasswordRequest request) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new BadCredentialsException("User is not authenticated");
        }
        String identifier = authentication.getName();
        User user = userRepository.findByUsername(identifier)
                .or(() -> userRepository.findByEmail(identifier))
                .or(() -> userRepository.findByUsernameOrEmail(identifier))
                .orElseThrow(() -> new ResourceNotFoundException("User", "username", identifier));

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
            throw new BadCredentialsException("Current password does not match");
        }

        String newPasswordHash = passwordEncoder.encode(request.getNewPassword());
        userRepository.updatePassword(user.getId(), newPasswordHash);
        log.info("Password successfully changed for user '{}'", user.getUsername());
    }
}
