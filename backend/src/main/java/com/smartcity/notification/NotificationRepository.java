package com.smartcity.notification;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    @Query("SELECT n FROM Notification n WHERE (n.recipientUser.id = :userId OR n.recipientUser IS NULL OR n.recipientRole IN :roles) ORDER BY n.sentAt DESC")
    List<Notification> findForUserAndRoles(@Param("userId") UUID userId, @Param("roles") List<String> roles);

    @Query("SELECT n FROM Notification n WHERE (n.recipientUser.id = :userId OR n.recipientUser IS NULL OR n.recipientRole IN :roles) AND n.status = 'UNREAD' ORDER BY n.sentAt DESC")
    List<Notification> findUnreadForUserAndRoles(@Param("userId") UUID userId, @Param("roles") List<String> roles);

    List<Notification> findTop50ByOrderBySentAtDesc();

    List<Notification> findTop50ByStatusOrderBySentAtDesc(String status);

    long countByRecipientUserIdAndStatus(UUID recipientUserId, String status);

    @Modifying
    @Query("UPDATE Notification n SET n.status = 'READ', n.readAt = :readAt WHERE n.id = :id")
    int markAsRead(@Param("id") UUID id, @Param("readAt") Instant readAt);

    @Modifying
    @Query("UPDATE Notification n SET n.status = 'READ', n.readAt = :readAt WHERE (n.recipientUser.id = :userId OR n.recipientUser IS NULL) AND n.status = 'UNREAD'")
    int markAllAsReadForUser(@Param("userId") UUID userId, @Param("readAt") Instant readAt);

    @Modifying
    @Query("UPDATE Notification n SET n.status = 'READ', n.readAt = :readAt WHERE n.status = 'UNREAD'")
    int markAllAsRead(@Param("readAt") Instant readAt);
}
