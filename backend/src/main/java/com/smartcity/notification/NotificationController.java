package com.smartcity.notification;

import com.smartcity.common.ApiResponse;
import com.smartcity.user.User;
import com.smartcity.user.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/notifications")
@Tag(name = "Notifications", description = "Municipal emergency alerts, real-time alerts, and notification lifecycle management")
public class NotificationController {

    private final NotificationService notificationService;
    private final UserRepository userRepository;

    public NotificationController(NotificationService notificationService, UserRepository userRepository) {
        this.notificationService = notificationService;
        this.userRepository = userRepository;
    }

    private UserAuthInfo extractAuthInfo(Authentication authentication) {
        UUID userId = null;
        List<String> roles = new ArrayList<>();

        if (authentication != null && authentication.isAuthenticated() && !"anonymousUser".equals(authentication.getPrincipal())) {
            if (authentication.getPrincipal() instanceof User u) {
                userId = u.getId();
            } else {
                String username = authentication.getName();
                Optional<User> userOpt = userRepository.findByUsername(username)
                        .or(() -> userRepository.findByEmail(username));
                if (userOpt.isPresent()) {
                    userId = userOpt.get().getId();
                }
            }
            for (GrantedAuthority auth : authentication.getAuthorities()) {
                String role = auth.getAuthority().replace("ROLE_", "");
                roles.add(role);
            }
        }
        return new UserAuthInfo(userId, roles);
    }

    @GetMapping
    @Operation(summary = "Get user and operational notifications",
            description = "Retrieves recent notifications filtered by authenticated user, role, unread status, type, severity, and pagination.")
    public ResponseEntity<ApiResponse<List<NotificationDTO>>> getNotifications(
            @Parameter(description = "Filter only unread notifications")
            @RequestParam(required = false, defaultValue = "false") Boolean unreadOnly,
            @Parameter(description = "Filter by notification event type")
            @RequestParam(required = false) String type,
            @Parameter(description = "Filter by severity level (CRITICAL, HIGH, MEDIUM, LOW)")
            @RequestParam(required = false) String severity,
            @Parameter(description = "Filter only direct individual notifications for the user")
            @RequestParam(required = false) Boolean directOnly,
            @Parameter(description = "Notification scope: ALL or INDIVIDUAL")
            @RequestParam(required = false) String scope,
            @Parameter(description = "Pagination page number (0-based)")
            @RequestParam(required = false, defaultValue = "0") int page,
            @Parameter(description = "Page size (default: 20)")
            @RequestParam(required = false, defaultValue = "20") int size,
            Authentication authentication
    ) {
        UserAuthInfo auth = extractAuthInfo(authentication);
        boolean isDirect = Boolean.TRUE.equals(directOnly) || "INDIVIDUAL".equalsIgnoreCase(scope);
        List<NotificationDTO> list = isDirect
                ? notificationService.getNotificationsForUser(auth.userId, auth.roles, unreadOnly, type, severity, true, page, size)
                : notificationService.getNotificationsForUser(auth.userId, auth.roles, unreadOnly, type, severity, page, size);
        return ResponseEntity.ok(ApiResponse.ok(list, "Notifications retrieved successfully"));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get notification details by ID",
            description = "Retrieves full notification details by ID, enforcing recipient user ownership or target role authorization.")
    public ResponseEntity<ApiResponse<NotificationDTO>> getNotificationById(
            @PathVariable UUID id,
            Authentication authentication
    ) {
        UserAuthInfo auth = extractAuthInfo(authentication);
        NotificationDTO dto = notificationService.getNotificationById(id, auth.userId, auth.roles);
        return ResponseEntity.ok(ApiResponse.ok(dto, "Notification details retrieved successfully"));
    }

    @GetMapping("/unread-count")
    @Operation(summary = "Get unread notification count",
            description = "Returns count of unread notifications for the authenticated user and their active roles.")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getUnreadCount(Authentication authentication) {
        UserAuthInfo auth = extractAuthInfo(authentication);
        long count = notificationService.getUnreadCountForUser(auth.userId, auth.roles);
        return ResponseEntity.ok(ApiResponse.ok(Map.of("unreadCount", count), "Unread notification count retrieved"));
    }

    @PatchMapping("/{id}/read")
    @Operation(summary = "Mark notification as read",
            description = "Marks a specific notification as READ, enforcing recipient user ownership.")
    public ResponseEntity<ApiResponse<NotificationDTO>> markAsRead(
            @PathVariable UUID id,
            Authentication authentication
    ) {
        UserAuthInfo auth = extractAuthInfo(authentication);
        NotificationDTO dto = notificationService.markAsRead(id, auth.userId, auth.roles);
        return ResponseEntity.ok(ApiResponse.ok(dto, "Notification marked as read"));
    }

    @PatchMapping("/{id}/unread")
    @Operation(summary = "Mark notification as unread",
            description = "Marks a specific notification as UNREAD, enforcing recipient user ownership.")
    public ResponseEntity<ApiResponse<NotificationDTO>> markAsUnread(
            @PathVariable UUID id,
            Authentication authentication
    ) {
        UserAuthInfo auth = extractAuthInfo(authentication);
        NotificationDTO dto = notificationService.markAsUnread(id, auth.userId, auth.roles);
        return ResponseEntity.ok(ApiResponse.ok(dto, "Notification marked as unread"));
    }

    @PatchMapping("/read-all")
    @Operation(summary = "Mark all eligible notifications as read",
            description = "Marks all unread notifications for the current authenticated user as READ.")
    public ResponseEntity<ApiResponse<Map<String, Object>>> markAllAsRead(
            Authentication authentication
    ) {
        UserAuthInfo auth = extractAuthInfo(authentication);
        int count = notificationService.markAllAsRead(auth.userId, auth.roles);
        return ResponseEntity.ok(ApiResponse.ok(Map.of("markedCount", count), count + " notifications marked as read"));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete notification",
            description = "Deletes a notification, ensuring the authenticated user is the recipient or an administrator.")
    public ResponseEntity<ApiResponse<Void>> deleteNotification(
            @PathVariable UUID id,
            Authentication authentication
    ) {
        UserAuthInfo auth = extractAuthInfo(authentication);
        notificationService.deleteNotification(id, auth.userId, auth.roles);
        return ResponseEntity.ok(ApiResponse.ok(null, "Notification deleted successfully"));
    }

    private static class UserAuthInfo {
        final UUID userId;
        final List<String> roles;

        UserAuthInfo(UUID userId, List<String> roles) {
            this.userId = userId;
            this.roles = roles;
        }
    }
}
