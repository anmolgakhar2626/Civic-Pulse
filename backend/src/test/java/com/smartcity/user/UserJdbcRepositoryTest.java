package com.smartcity.user;

import com.smartcity.common.DuplicateResourceException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("UserJdbcRepository & SQL Parameterization Integration Tests")
class UserJdbcRepositoryTest {

    private JdbcTemplate jdbcTemplate;
    private UserJdbcRepository userJdbcRepository;

    @BeforeEach
    void setUp() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName("org.h2.Driver");
        dataSource.setUrl("jdbc:h2:mem:user_jdbc_test_" + UUID.randomUUID().toString().replace("-", "")
                + ";MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1");
        dataSource.setUsername("sa");
        dataSource.setPassword("");

        jdbcTemplate = new JdbcTemplate(dataSource);
        userJdbcRepository = new UserJdbcRepository(jdbcTemplate);

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
    }

    @Test
    @DisplayName("Should insert user using parameterized SQL and retrieve by ID and Email")
    void shouldInsertAndFindUser() {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setFullName("Aria Stark");
        user.setEmail("aria@winterfell.gov");
        user.setUsername("aria_stark");
        user.setPasswordHash("$2a$10$hashedSecurePasswordExample");
        user.setRoleName("COMMAND_OPERATOR");
        user.setEnabled(true);
        user.setActive(true);
        user.setPhone("+1-555-0100");
        user.setCreatedAt(Instant.now());
        user.setUpdatedAt(Instant.now());

        User saved = userJdbcRepository.save(user);
        assertNotNull(saved);
        assertEquals(user.getId(), saved.getId());

        // Find by ID
        Optional<User> byId = userJdbcRepository.findById(user.getId());
        assertTrue(byId.isPresent());
        assertEquals("Aria Stark", byId.get().getFullName());
        assertEquals("COMMAND_OPERATOR", byId.get().getRoleName());
        assertTrue(byId.get().isEnabled());

        // Find by Email (case-insensitive)
        Optional<User> byEmail = userJdbcRepository.findByEmail("ARIA@WINTERFELL.GOV");
        assertTrue(byEmail.isPresent());
        assertEquals(user.getId(), byEmail.get().getId());

        // Find by Username (case-insensitive)
        Optional<User> byUsername = userJdbcRepository.findByUsername("ARIA_STARK");
        assertTrue(byUsername.isPresent());
        assertEquals(user.getId(), byUsername.get().getId());
    }

    @Test
    @DisplayName("Should reject duplicate email registration via unique constraint")
    void shouldRejectDuplicateEmail() {
        User user1 = new User();
        user1.setId(UUID.randomUUID());
        user1.setFullName("Citizen One");
        user1.setEmail("duplicate@smartcity.gov");
        user1.setUsername("user_one");
        user1.setPasswordHash("$2a$10$dummyHash1");
        user1.setRoleName("CITIZEN");
        user1.setEnabled(true);
        userJdbcRepository.save(user1);

        User user2 = new User();
        user2.setId(UUID.randomUUID());
        user2.setFullName("Citizen Two");
        user2.setEmail("DUPLICATE@SMARTCITY.GOV");
        user2.setUsername("user_two");
        user2.setPasswordHash("$2a$10$dummyHash2");
        user2.setRoleName("CITIZEN");
        user2.setEnabled(true);

        assertThrows(DuplicateResourceException.class, () -> userJdbcRepository.save(user2));
    }

    @Test
    @DisplayName("Should check email and username existence correctly")
    void shouldCheckExistence() {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setFullName("John Snow");
        user.setEmail("john.snow@nightwatch.gov");
        user.setUsername("john_snow");
        user.setPasswordHash("$2a$10$hashedPw");
        user.setRoleName("FIELD_WORKER");
        user.setEnabled(true);
        userJdbcRepository.save(user);

        assertTrue(userJdbcRepository.existsByEmail("john.snow@nightwatch.gov"));
        assertTrue(userJdbcRepository.existsByEmail("JOHN.SNOW@NIGHTWATCH.GOV"));
        assertFalse(userJdbcRepository.existsByEmail("other@nightwatch.gov"));

        assertTrue(userJdbcRepository.existsByUsername("john_snow"));
        assertTrue(userJdbcRepository.existsByUsername("JOHN_SNOW"));
        assertFalse(userJdbcRepository.existsByUsername("non_existent"));
    }

    @Test
    @DisplayName("Should execute parameterized UPDATE queries for profile, password, role and status")
    void shouldUpdateProfileAndPasswordAndStatus() {
        User user = new User();
        UUID id = UUID.randomUUID();
        user.setId(id);
        user.setFullName("Original Name");
        user.setEmail("update.test@city.gov");
        user.setUsername("update_test");
        user.setPasswordHash("$2a$10$initialPasswordHash");
        user.setRoleName("CITIZEN");
        user.setEnabled(true);
        user.setPhone("111-222-3333");
        userJdbcRepository.save(user);

        // 1. Update Profile
        int profileRows = userJdbcRepository.updateProfile(id, "Updated Name", "999-888-7777");
        assertEquals(1, profileRows);
        User updatedUser = userJdbcRepository.findById(id).orElseThrow();
        assertEquals("Updated Name", updatedUser.getFullName());
        assertEquals("999-888-7777", updatedUser.getPhone());

        // 2. Update Password
        int pwRows = userJdbcRepository.updatePassword(id, "$2a$10$newSecureHashedPassword");
        assertEquals(1, pwRows);
        User userWithNewPw = userJdbcRepository.findById(id).orElseThrow();
        assertEquals("$2a$10$newSecureHashedPassword", userWithNewPw.getPasswordHash());

        // 3. Update Role
        int roleRows = userJdbcRepository.updateRole(id, "ADMIN");
        assertEquals(1, roleRows);
        assertEquals("ADMIN", userJdbcRepository.findRoleByUserId(id).orElse(null));

        // 4. Update Account Status (Disable)
        int statusRows = userJdbcRepository.updateAccountStatus(id, false);
        assertEquals(1, statusRows);
        assertFalse(userJdbcRepository.isAccountEnabled(id));
        User disabledUser = userJdbcRepository.findById(id).orElseThrow();
        assertFalse(disabledUser.isEnabled());
    }

    @Test
    @DisplayName("Should retrieve all users and total count")
    void shouldRetrieveAllUsersAndCount() {
        assertEquals(0, userJdbcRepository.count());

        for (int i = 1; i <= 3; i++) {
            User u = new User();
            u.setId(UUID.randomUUID());
            u.setFullName("User " + i);
            u.setEmail("user" + i + "@city.gov");
            u.setUsername("user" + i);
            u.setPasswordHash("$2a$10$dummyHash");
            u.setRoleName("CITIZEN");
            u.setEnabled(true);
            userJdbcRepository.save(u);
        }

        assertEquals(3, userJdbcRepository.count());
        List<User> list = userJdbcRepository.findAll();
        assertEquals(3, list.size());
    }
}

