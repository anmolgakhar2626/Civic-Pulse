package com.smartcity.notification;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface NotificationDao {

    NotificationDTO save(Notification notification);

    Optional<NotificationDTO> findById(UUID id);

    List<NotificationDTO> findByRecipient(
            UUID userId,
            List<String> roles,
            Boolean unreadOnly,
            String type,
            String severity,
            int offset,
            int limit
    );

    List<NotificationDTO> findByRecipient(
            UUID userId,
            List<String> roles,
            Boolean unreadOnly,
            String type,
            String severity,
            Boolean directOnly,
            int offset,
            int limit
    );

    long countUnread(UUID userId, List<String> roles);

    int markAsRead(UUID id, UUID userId);

    int markAsUnread(UUID id, UUID userId);

    int markAllAsRead(UUID userId, List<String> roles);

    boolean deleteById(UUID id, UUID userId);

    boolean existsByDedupKey(String dedupKey);
}
