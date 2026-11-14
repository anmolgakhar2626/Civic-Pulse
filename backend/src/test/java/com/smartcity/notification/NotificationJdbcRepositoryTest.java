package com.smartcity.notification;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("NotificationJdbcRepository Unit Tests")
class NotificationJdbcRepositoryTest {

    private JdbcTemplate jdbcTemplate;
    private NotificationJdbcRepository repository;

    private UUID user1Id;
    private UUID user2Id;

    @BeforeEach
    void setUp() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName("org.h2.Driver");
        dataSource.setUrl("jdbc:h2:mem:notif_test_" + UUID.randomUUID().toString().replace("-", "")
                + ";MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1");
        dataSource.setUsername("sa");
        dataSource.setPassword("");

        jdbcTemplate = new JdbcTemplate(dataSource);
        repository = new NotificationJdbcRepository(jdbcTemplate);

        jdbcTemplate.execute("""
            CREATE TABLE users (
                id UUID PRIMARY KEY,
                username VARCHAR(50) NOT NULL UNIQUE
            )
        """);

        jdbcTemplate.execute("""
            CREATE TABLE notifications (
                id UUID PRIMARY KEY,
                recipient_user_id UUID,
                recipient_role VARCHAR(100),
                title VARCHAR(200) NOT NULL,
                message TEXT NOT NULL,
                notification_type VARCHAR(50) NOT NULL,
                incident_id UUID,
                severity VARCHAR(50) NOT NULL DEFAULT 'MEDIUM',
                channel VARCHAR(50) NOT NULL DEFAULT 'IN_APP',
                status VARCHAR(50) NOT NULL DEFAULT 'UNREAD',
                is_read BOOLEAN NOT NULL DEFAULT false,
                dedup_key VARCHAR(255),
                sent_at TIMESTAMP NOT NULL,
                read_at TIMESTAMP,
                created_at TIMESTAMP NOT NULL,
                complaint_id UUID,
                complaint_number VARCHAR(100)
            )
        """);

        user1Id = UUID.randomUUID();
        user2Id = UUID.randomUUID();

        jdbcTemplate.update("INSERT INTO users (id, username) VALUES (?, ?)", user1Id, "operator1");
        jdbcTemplate.update("INSERT INTO users (id, username) VALUES (?, ?)", user2Id, "citizen1");
    }

    @Test
    @DisplayName("save and findById correctly persists and retrieves notification")
    void testSaveAndFindById() {
        Notification notif = new Notification();
        notif.setId(UUID.randomUUID());
        notif.setTitle("Test Alert");
        notif.setMessage("Something happened");
        notif.setNotificationType("CRITICAL_ALERT");
        notif.setSeverity("CRITICAL");
        notif.setDedupKey("DEDUP-001");
        notif.setSentAt(Instant.now());
        notif.setCreatedAt(Instant.now());

        NotificationDTO saved = repository.save(notif);
        assertThat(saved.getId()).isEqualTo(notif.getId());

        Optional<NotificationDTO> found = repository.findById(notif.getId());
        assertThat(found).isPresent();
        assertThat(found.get().getTitle()).isEqualTo("Test Alert");
        assertThat(found.get().getSeverity()).isEqualTo("CRITICAL");
        assertThat(found.get().getDedupKey()).isEqualTo("DEDUP-001");
        assertThat(found.get().isRead()).isFalse();
    }

    @Test
    @DisplayName("findByRecipient filters by user and role correctly with pagination")
    void testFindByRecipient() {
        // Notification for user 1 directly
        Notification n1 = new Notification();
        n1.setId(UUID.randomUUID());
        com.smartcity.user.User u1 = new com.smartcity.user.User();
        u1.setId(user1Id);
        n1.setRecipientUser(u1);
        n1.setTitle("Personal Notice");
        n1.setMessage("Msg 1");
        n1.setNotificationType("INCIDENT_ASSIGNED");
        n1.setSeverity("HIGH");
        repository.save(n1);

        // Notification for COMMAND_OPERATOR role
        Notification n2 = new Notification();
        n2.setId(UUID.randomUUID());
        n2.setRecipientRole("COMMAND_OPERATOR");
        n2.setTitle("Role Notice");
        n2.setMessage("Msg 2");
        n2.setNotificationType("SLA_WARNING");
        n2.setSeverity("CRITICAL");
        repository.save(n2);

        // Notification for user 2 directly
        Notification n3 = new Notification();
        n3.setId(UUID.randomUUID());
        com.smartcity.user.User u2 = new com.smartcity.user.User();
        u2.setId(user2Id);
        n3.setRecipientUser(u2);
        n3.setTitle("Citizen Notice");
        n3.setMessage("Msg 3");
        n3.setNotificationType("INCIDENT_RESOLVED");
        n3.setSeverity("LOW");
        repository.save(n3);

        // User 1 with role COMMAND_OPERATOR should see n1 and n2, but NOT n3
        List<NotificationDTO> user1List = repository.findByRecipient(user1Id, List.of("COMMAND_OPERATOR"), false, null, null, 0, 10);
        assertThat(user1List).hasSize(2);
        assertThat(user1List).extracting(NotificationDTO::getTitle).containsExactlyInAnyOrder("Personal Notice", "Role Notice");

        // User 2 with role CITIZEN should see ONLY n3
        List<NotificationDTO> user2List = repository.findByRecipient(user2Id, List.of("CITIZEN"), false, null, null, 0, 10);
        assertThat(user2List).hasSize(1);
        assertThat(user2List.get(0).getTitle()).isEqualTo("Citizen Notice");
    }

    @Test
    @DisplayName("countUnread returns accurate unread count")
    void testCountUnread() {
        Notification n1 = new Notification();
        n1.setId(UUID.randomUUID());
        com.smartcity.user.User u1 = new com.smartcity.user.User();
        u1.setId(user1Id);
        n1.setRecipientUser(u1);
        n1.setTitle("Unread 1");
        n1.setMessage("Msg");
        n1.setNotificationType("CRITICAL_ALERT");
        repository.save(n1);

        Notification n2 = new Notification();
        n2.setId(UUID.randomUUID());
        n2.setRecipientUser(u1);
        n2.setTitle("Unread 2");
        n2.setMessage("Msg");
        n2.setNotificationType("CRITICAL_ALERT");
        repository.save(n2);

        long unread = repository.countUnread(user1Id, List.of());
        assertThat(unread).isEqualTo(2);

        // Mark one as read
        repository.markAsRead(n1.getId(), user1Id);

        unread = repository.countUnread(user1Id, List.of());
        assertThat(unread).isEqualTo(1);
    }

    @Test
    @DisplayName("markAsRead enforces user isolation: user cannot mark another user's notification as read")
    void testMarkAsReadUserIsolation() {
        Notification n = new Notification();
        n.setId(UUID.randomUUID());
        com.smartcity.user.User u1 = new com.smartcity.user.User();
        u1.setId(user1Id);
        n.setRecipientUser(u1);
        n.setTitle("User 1 Notice");
        n.setMessage("Msg");
        n.setNotificationType("CRITICAL_ALERT");
        repository.save(n);

        // User 2 attempts to mark User 1's notification as read
        int updated = repository.markAsRead(n.getId(), user2Id);
        assertThat(updated).isEqualTo(0);

        // Still unread
        NotificationDTO dto = repository.findById(n.getId()).orElseThrow();
        assertThat(dto.isRead()).isFalse();

        // User 1 marks it as read
        updated = repository.markAsRead(n.getId(), user1Id);
        assertThat(updated).isEqualTo(1);

        dto = repository.findById(n.getId()).orElseThrow();
        assertThat(dto.isRead()).isTrue();
    }

    @Test
    @DisplayName("deleteById enforces user isolation: user cannot delete another user's notification")
    void testDeleteByIdUserIsolation() {
        Notification n = new Notification();
        n.setId(UUID.randomUUID());
        com.smartcity.user.User u1 = new com.smartcity.user.User();
        u1.setId(user1Id);
        n.setRecipientUser(u1);
        n.setTitle("User 1 Notice");
        n.setMessage("Msg");
        n.setNotificationType("CRITICAL_ALERT");
        repository.save(n);

        // User 2 attempts to delete User 1's notification
        boolean deleted = repository.deleteById(n.getId(), user2Id);
        assertThat(deleted).isFalse();
        assertThat(repository.findById(n.getId())).isPresent();

        // User 1 deletes it
        deleted = repository.deleteById(n.getId(), user1Id);
        assertThat(deleted).isTrue();
        assertThat(repository.findById(n.getId())).isEmpty();
    }

    @Test
    @DisplayName("existsByDedupKey correctly detects duplicate keys")
    void testExistsByDedupKey() {
        Notification n = new Notification();
        n.setId(UUID.randomUUID());
        n.setTitle("Notice");
        n.setMessage("Msg");
        n.setNotificationType("SLA_BREACHED");
        n.setDedupKey("SLA_BREACH_12345");
        repository.save(n);

        assertThat(repository.existsByDedupKey("SLA_BREACH_12345")).isTrue();
        assertThat(repository.existsByDedupKey("SLA_BREACH_OTHER")).isFalse();
    }
}
