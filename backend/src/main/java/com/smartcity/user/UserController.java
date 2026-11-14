package com.smartcity.user;

import com.smartcity.common.ResourceNotFoundException;
import com.smartcity.security.dto.UserSummaryResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/users")
@Tag(name = "User Management", description = "Administrative user, role, and account status management APIs")
@SecurityRequirement(name = "BearerAuth")
@PreAuthorize("hasRole('ADMIN')")
public class UserController {

    private final UserRepository userRepository;

    public UserController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @GetMapping
    @Operation(summary = "List all municipal users",
            description = "Retrieves all registered users across all roles (Admin only). Safe profile representation without password hashes.")
    public ResponseEntity<List<UserSummaryResponse>> getAllUsers() {
        List<User> users = userRepository.findAll();
        List<UserSummaryResponse> responses = users.stream()
                .map(UserSummaryResponse::fromEntity)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get user by ID",
            description = "Retrieves safe user profile by primary key UUID (Admin only).")
    public ResponseEntity<UserSummaryResponse> getUserById(@PathVariable UUID id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));
        return ResponseEntity.ok(UserSummaryResponse.fromEntity(user));
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Update user account active/enabled status",
            description = "Enables or disables a user account (Admin only).")
    public ResponseEntity<UserSummaryResponse> updateStatus(
            @PathVariable UUID id,
            @RequestBody Map<String, Boolean> statusUpdate
    ) {
        Boolean enabled = statusUpdate.get("enabled");
        if (enabled == null) {
            enabled = statusUpdate.get("active");
        }
        if (enabled == null) {
            throw new IllegalArgumentException("Field 'enabled' or 'active' is required");
        }
        userRepository.updateAccountStatus(id, enabled);
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));
        return ResponseEntity.ok(UserSummaryResponse.fromEntity(user));
    }

    @PatchMapping("/{id}/role")
    @Operation(summary = "Update user municipal role",
            description = "Assigns a new role to a user (Admin only).")
    public ResponseEntity<UserSummaryResponse> updateRole(
            @PathVariable UUID id,
            @RequestBody Map<String, String> roleUpdate
    ) {
        String newRole = roleUpdate.get("role");
        if (newRole == null || newRole.isBlank()) {
            throw new IllegalArgumentException("Field 'role' is required");
        }
        String cleanRole = newRole.replace("ROLE_", "").toUpperCase();
        userRepository.updateRole(id, cleanRole);
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));
        return ResponseEntity.ok(UserSummaryResponse.fromEntity(user));
    }

    @GetMapping("/stats")
    @Operation(summary = "Get user role counts and statistics",
            description = "Returns total user count and breakdown by role (Admin only).")
    public ResponseEntity<Map<String, Object>> getUserStats() {
        List<User> users = userRepository.findAll();
        Map<String, Long> byRole = users.stream()
                .collect(Collectors.groupingBy(User::getRoleName, Collectors.counting()));
        long activeCount = users.stream().filter(User::isEnabled).count();
        long disabledCount = users.size() - activeCount;

        return ResponseEntity.ok(Map.of(
                "totalUsers", users.size(),
                "activeUsers", activeCount,
                "disabledUsers", disabledCount,
                "rolesDistribution", byRole
        ));
    }
}
