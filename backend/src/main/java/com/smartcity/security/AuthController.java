package com.smartcity.security;

import com.smartcity.security.dto.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentication", description = "Operator and citizen authentication, registration, session management, and profile APIs")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    @Operation(summary = "Register a new user",
            description = "Registers a new citizen or municipal user with BCrypt hashed password credentials using JDBC.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "User successfully registered",
                    content = @Content(schema = @Schema(implementation = UserSummaryResponse.class))),
            @ApiResponse(responseCode = "400", description = "Validation failure or invalid input"),
            @ApiResponse(responseCode = "409", description = "Username or email already in use")
    })
    public ResponseEntity<UserSummaryResponse> register(@Valid @RequestBody RegisterRequest request) {
        UserSummaryResponse response = authService.register(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @PostMapping("/login")
    @Operation(summary = "Authenticate user and get JWT access token",
            description = "Validates username/email and BCrypt password, and issues a cryptographically signed JWT Bearer access token.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Authentication successful",
                    content = @Content(schema = @Schema(implementation = AuthResponse.class))),
            @ApiResponse(responseCode = "400", description = "Validation failure"),
            @ApiResponse(responseCode = "401", description = "Invalid credentials or disabled account")
    })
    public ResponseEntity<AuthResponse> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest
    ) {
        String ipAddress = httpRequest.getRemoteAddr();
        AuthResponse response = authService.login(request, ipAddress);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/login-otp/send")
    @Operation(summary = "Send Mobile OTP for Citizen Login",
            description = "Dispatches a 6-digit OTP code to an existing verified citizen account respecting cooldown and hourly rate limits.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "OTP request dispatched or anti-enumeration response",
                    content = @Content(schema = @Schema(implementation = com.smartcity.security.otp.dto.OtpResponse.class))),
            @ApiResponse(responseCode = "400", description = "Validation failure or invalid mobile format")
    })
    public ResponseEntity<com.smartcity.security.otp.dto.OtpResponse> sendLoginOtp(
            @Valid @RequestBody LoginOtpSendRequest request
    ) {
        com.smartcity.security.otp.dto.OtpResponse response = authService.sendLoginOtp(request);
        if (!response.isSuccess()) {
            return ResponseEntity.badRequest().body(response);
        }
        return ResponseEntity.ok(response);
    }

    @PostMapping("/login-otp/verify")
    @Operation(summary = "Verify Mobile OTP and authenticate Citizen session",
            description = "Validates the 6-digit OTP code for purpose LOGIN, marks it consumed, and issues a standard JWT Bearer token.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Authentication successful, JWT token returned",
                    content = @Content(schema = @Schema(implementation = AuthResponse.class))),
            @ApiResponse(responseCode = "400", description = "Validation failure or incorrect input format"),
            @ApiResponse(responseCode = "401", description = "Invalid/expired OTP or account not eligible")
    })
    public ResponseEntity<AuthResponse> verifyLoginOtp(
            @Valid @RequestBody LoginOtpVerifyRequest request,
            HttpServletRequest httpRequest
    ) {
        String ipAddress = httpRequest.getRemoteAddr();
        AuthResponse response = authService.loginWithOtp(request, ipAddress);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/logout")
    @Operation(summary = "Logout user and record audit log",
            description = "Invalidates user session and records an audit log event.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Logout successful")
    })
    public ResponseEntity<Map<String, String>> logout(
            Authentication authentication,
            HttpServletRequest httpRequest
    ) {
        String ipAddress = httpRequest.getRemoteAddr();
        authService.logout(authentication, ipAddress);
        return ResponseEntity.ok(Map.of("message", "Logged out successfully"));
    }

    @GetMapping("/me")
    @SecurityRequirement(name = "BearerAuth")
    @Operation(summary = "Get current authenticated user profile",
            description = "Returns user profile, active roles, and department affiliations for the authenticated session.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Profile retrieved successfully",
                    content = @Content(schema = @Schema(implementation = UserSummaryResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthenticated session")
    })
    public ResponseEntity<UserSummaryResponse> getCurrentUser(Authentication authentication) {
        UserSummaryResponse response = authService.getCurrentUser(authentication);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/profile")
    @SecurityRequirement(name = "BearerAuth")
    @Operation(summary = "Update user profile",
            description = "Updates user profile information via parameterized JDBC update.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Profile updated successfully",
                    content = @Content(schema = @Schema(implementation = UserSummaryResponse.class))),
            @ApiResponse(responseCode = "400", description = "Validation failure"),
            @ApiResponse(responseCode = "401", description = "Unauthenticated session")
    })
    public ResponseEntity<UserSummaryResponse> updateProfile(
            Authentication authentication,
            @Valid @RequestBody UpdateProfileRequest request
    ) {
        UserSummaryResponse response = authService.updateProfile(authentication, request);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/change-password")
    @SecurityRequirement(name = "BearerAuth")
    @Operation(summary = "Change password",
            description = "Verifies current password and updates to new BCrypt hash via parameterized JDBC update.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Password changed successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid current password or validation failure"),
            @ApiResponse(responseCode = "401", description = "Unauthenticated session")
    })
    public ResponseEntity<Map<String, String>> changePassword(
            Authentication authentication,
            @Valid @RequestBody ChangePasswordRequest request
    ) {
        authService.changePassword(authentication, request);
        return ResponseEntity.ok(Map.of("message", "Password changed successfully"));
    }
}
