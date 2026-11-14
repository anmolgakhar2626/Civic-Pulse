package com.smartcity.user;

import com.smartcity.common.DuplicateResourceException;
import com.smartcity.department.Department;
import com.smartcity.department.Team;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Primary;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Spring JdbcTemplate implementation of UserRepository and UserDao.
 * Uses explicit, parameterized SQL queries for all persistence operations.
 * Spring Data JPA / Hibernate is not used for user persistence.
 */
@Repository
@Primary
public class UserJdbcRepository implements UserRepository, UserDao {

    private static final Logger log = LoggerFactory.getLogger(UserJdbcRepository.class);

    private final JdbcTemplate jdbcTemplate;

    public UserJdbcRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private static final String BASE_USER_SELECT = """
        SELECT u.id, u.full_name, u.email, u.username, u.password_hash, u.role, u.enabled, u.phone,
               u.mobile_number, u.is_mobile_verified, u.genuine_complaint_count, u.is_verified_citizen,
               u.department_id, d.name AS dept_name, d.code AS dept_code,
               u.team_id, t.name AS team_name, t.code AS team_code,
               u.role_id, r.name AS role_obj_name,
               u.active, u.created_at, u.updated_at
        FROM users u
        LEFT JOIN departments d ON d.id = u.department_id
        LEFT JOIN teams t ON t.id = u.team_id
        LEFT JOIN roles r ON r.id = u.role_id
    """;

    private final RowMapper<User> userRowMapper = (rs, rowNum) -> {
        User user = new User();
        String idStr = rs.getString("id");
        if (idStr != null) {
            user.setId(UUID.fromString(idStr));
        }

        user.setFullName(rs.getString("full_name"));
        user.setEmail(rs.getString("email"));
        user.setUsername(rs.getString("username"));
        user.setPasswordHash(rs.getString("password_hash"));

        String roleName = rs.getString("role");
        if (roleName == null || roleName.isBlank()) {
            roleName = "CITIZEN";
        }
        user.setRoleName(roleName);

        Role roleObj = new Role(roleName, roleName + " role");
        String roleIdStr = rs.getString("role_id");
        if (roleIdStr != null) {
            roleObj.setId(UUID.fromString(roleIdStr));
        }
        user.setRole(roleObj);

        boolean enabled = rs.getBoolean("enabled");
        user.setEnabled(enabled);
        user.setActive(enabled);

        user.setPhone(rs.getString("phone"));

        try {
            user.setMobileNumber(rs.getString("mobile_number"));
            user.setIsMobileVerified(rs.getBoolean("is_mobile_verified"));
            user.setGenuineComplaintCount(rs.getInt("genuine_complaint_count"));
            user.setIsVerifiedCitizen(rs.getBoolean("is_verified_citizen"));
        } catch (SQLException ignored) {
            // Support backward compatibility if columns aren't present in specific query projection
        }

        String deptIdStr = rs.getString("department_id");
        if (deptIdStr != null) {
            Department dept = new Department();
            dept.setId(UUID.fromString(deptIdStr));
            try {
                dept.setName(rs.getString("dept_name"));
                dept.setCode(rs.getString("dept_code"));
            } catch (SQLException ignored) {}
            user.setDepartment(dept);
        }

        String teamIdStr = rs.getString("team_id");
        if (teamIdStr != null) {
            Team team = new Team();
            team.setId(UUID.fromString(teamIdStr));
            try {
                team.setName(rs.getString("team_name"));
                team.setCode(rs.getString("team_code"));
            } catch (SQLException ignored) {}
            user.setTeam(team);
        }

        Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null) {
            user.setCreatedAt(createdAt.toInstant());
        }

        Timestamp updatedAt = rs.getTimestamp("updated_at");
        if (updatedAt != null) {
            user.setUpdatedAt(updatedAt.toInstant());
        }

        return user;
    };

    @Override
    @Transactional
    public User save(User user) {
        if (user.getEmail() != null) {
            user.setEmail(user.getEmail().trim().toLowerCase());
        }
        if (user.getUsername() != null) {
            user.setUsername(user.getUsername().trim());
        }
        if (user.getId() == null) {
            user.setId(UUID.randomUUID());
            return insertUser(user);
        }

        if (existsById(user.getId())) {
            updateUser(user);
            return user;
        } else {
            return insertUser(user);
        }
    }

    private User insertUser(User user) {
        if (user.getCreatedAt() == null) {
            user.setCreatedAt(Instant.now());
        }
        user.setUpdatedAt(Instant.now());

        String roleStr = user.getRoleName() != null ? user.getRoleName() : "CITIZEN";
        boolean enabled = user.getEnabled() != null ? user.getEnabled() : true;

        String sql = """
            INSERT INTO users (
                id, full_name, email, username, password_hash, role, enabled, phone,
                mobile_number, is_mobile_verified, genuine_complaint_count, is_verified_citizen,
                department_id, team_id, role_id, active, created_at, updated_at
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """;

        try {
            jdbcTemplate.update(
                    sql,
                    user.getId(),
                    user.getFullName(),
                    user.getEmail(),
                    user.getUsername(),
                    user.getPasswordHash(),
                    roleStr,
                    enabled,
                    user.getPhone(),
                    user.getMobileNumber(),
                    Boolean.TRUE.equals(user.getIsMobileVerified()),
                    user.getGenuineComplaintCount() != null ? user.getGenuineComplaintCount() : 0,
                    Boolean.TRUE.equals(user.getIsVerifiedCitizen()),
                    user.getDepartment() != null ? user.getDepartment().getId() : null,
                    user.getTeam() != null ? user.getTeam().getId() : null,
                    user.getRole() != null ? user.getRole().getId() : null,
                    enabled,
                    Timestamp.from(user.getCreatedAt()),
                    Timestamp.from(user.getUpdatedAt())
            );
            log.debug("Inserted user '{}' ({}) via parameterized JDBC query", user.getEmail(), user.getId());
            return user;
        } catch (DataIntegrityViolationException ex) {
            log.error("Failed to insert user '{}': {}", user.getEmail(), ex.getMessage(), ex);
            Throwable rootCause = ex.getMostSpecificCause();
            String rootMsg = rootCause != null && rootCause.getMessage() != null ? rootCause.getMessage().toLowerCase() : "";
            if (rootMsg.contains("users_email_key") || (rootMsg.contains("unique") && rootMsg.contains("email"))) {
                throw new DuplicateResourceException("User", "email", user.getEmail());
            } else if (rootMsg.contains("users_username_key") || (rootMsg.contains("unique") && rootMsg.contains("username"))) {
                throw new DuplicateResourceException("User", "username", user.getUsername());
            }
            throw ex;
        }
    }

    private void updateUser(User user) {
        user.setUpdatedAt(Instant.now());
        String roleStr = user.getRoleName() != null ? user.getRoleName() : "CITIZEN";
        boolean enabled = user.getEnabled() != null ? user.getEnabled() : true;

        String sql = """
            UPDATE users SET
                full_name = ?,
                email = ?,
                username = ?,
                password_hash = ?,
                role = ?,
                enabled = ?,
                active = ?,
                phone = ?,
                mobile_number = ?,
                is_mobile_verified = ?,
                genuine_complaint_count = ?,
                is_verified_citizen = ?,
                department_id = ?,
                team_id = ?,
                role_id = ?,
                updated_at = ?
            WHERE id = ?
        """;

        try {
            jdbcTemplate.update(
                    sql,
                    user.getFullName(),
                    user.getEmail(),
                    user.getUsername(),
                    user.getPasswordHash(),
                    roleStr,
                    enabled,
                    enabled,
                    user.getPhone(),
                    user.getMobileNumber(),
                    Boolean.TRUE.equals(user.getIsMobileVerified()),
                    user.getGenuineComplaintCount() != null ? user.getGenuineComplaintCount() : 0,
                    Boolean.TRUE.equals(user.getIsVerifiedCitizen()),
                    user.getDepartment() != null ? user.getDepartment().getId() : null,
                    user.getTeam() != null ? user.getTeam().getId() : null,
                    user.getRole() != null ? user.getRole().getId() : null,
                    Timestamp.from(user.getUpdatedAt()),
                    user.getId()
            );
            log.debug("Updated user '{}' ({}) via parameterized JDBC query", user.getEmail(), user.getId());
        } catch (DataIntegrityViolationException ex) {
            log.error("Failed to insert user '{}': {}", user.getEmail(), ex.getMessage(), ex);
            Throwable rootCause = ex.getMostSpecificCause();
            String rootMsg = rootCause != null && rootCause.getMessage() != null ? rootCause.getMessage().toLowerCase() : "";
            if (rootMsg.contains("users_email_key") || (rootMsg.contains("unique") && rootMsg.contains("email"))) {
                throw new DuplicateResourceException("User", "email", user.getEmail());
            } else if (rootMsg.contains("users_username_key") || (rootMsg.contains("unique") && rootMsg.contains("username"))) {
                throw new DuplicateResourceException("User", "username", user.getUsername());
            }
            throw ex;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<User> findById(UUID id) {
        String sql = BASE_USER_SELECT + " WHERE u.id = ?";
        List<User> results = jdbcTemplate.query(sql, userRowMapper, id);
        return results.stream().findFirst();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<User> findByEmail(String email) {
        if (email == null) return Optional.empty();
        String sql = BASE_USER_SELECT + " WHERE LOWER(u.email) = LOWER(?)";
        List<User> results = jdbcTemplate.query(sql, userRowMapper, email.trim());
        return results.stream().findFirst();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<User> findByUsername(String username) {
        if (username == null) return Optional.empty();
        String sql = BASE_USER_SELECT + " WHERE LOWER(u.username) = LOWER(?)";
        List<User> results = jdbcTemplate.query(sql, userRowMapper, username.trim());
        return results.stream().findFirst();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<User> findByUsernameOrEmail(String identifier) {
        if (identifier == null) return Optional.empty();
        String trimmed = identifier.trim();
        String sql = BASE_USER_SELECT + " WHERE LOWER(u.username) = LOWER(?) OR LOWER(u.email) = LOWER(?)";
        List<User> results = jdbcTemplate.query(sql, userRowMapper, trimmed, trimmed);
        return results.stream().findFirst();
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByEmail(String email) {
        if (email == null) return false;
        String sql = "SELECT COUNT(*) FROM users WHERE LOWER(email) = LOWER(?)";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, email.trim());
        return count != null && count > 0;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByUsername(String username) {
        if (username == null) return false;
        String sql = "SELECT COUNT(*) FROM users WHERE LOWER(username) = LOWER(?)";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, username.trim());
        return count != null && count > 0;
    }

    private boolean existsById(UUID id) {
        String sql = "SELECT COUNT(*) FROM users WHERE id = ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, id);
        return count != null && count > 0;
    }

    @Override
    @Transactional(readOnly = true)
    public List<User> findAll() {
        String sql = BASE_USER_SELECT + " ORDER BY u.created_at DESC";
        return jdbcTemplate.query(sql, userRowMapper);
    }

    @Override
    @Transactional
    public int updateProfile(UUID id, String fullName, String phone) {
        String sql = "UPDATE users SET full_name = ?, phone = ?, updated_at = ? WHERE id = ?";
        int updated = jdbcTemplate.update(sql, fullName, phone, Timestamp.from(Instant.now()), id);
        log.debug("Updated profile for user ID {} (affected rows: {})", id, updated);
        return updated;
    }

    @Override
    @Transactional
    public int updatePassword(UUID id, String newPasswordHash) {
        String sql = "UPDATE users SET password_hash = ?, updated_at = ? WHERE id = ?";
        int updated = jdbcTemplate.update(sql, newPasswordHash, Timestamp.from(Instant.now()), id);
        log.debug("Updated password hash for user ID {} (affected rows: {})", id, updated);
        return updated;
    }

    @Override
    @Transactional
    public int updateAccountStatus(UUID id, boolean enabled) {
        String sql = "UPDATE users SET enabled = ?, active = ?, updated_at = ? WHERE id = ?";
        int updated = jdbcTemplate.update(sql, enabled, enabled, Timestamp.from(Instant.now()), id);
        log.debug("Updated account enabled status to {} for user ID {} (affected rows: {})", enabled, id, updated);
        return updated;
    }

    @Override
    @Transactional
    public int updateRole(UUID id, String role) {
        String sql = "UPDATE users SET role = ?, updated_at = ? WHERE id = ?";
        int updated = jdbcTemplate.update(sql, role, Timestamp.from(Instant.now()), id);
        log.debug("Updated role to {} for user ID {} (affected rows: {})", role, id, updated);
        return updated;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<String> findRoleByUserId(UUID id) {
        String sql = "SELECT role FROM users WHERE id = ?";
        try {
            String role = jdbcTemplate.queryForObject(sql, String.class, id);
            return Optional.ofNullable(role);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isAccountEnabled(UUID id) {
        String sql = "SELECT enabled FROM users WHERE id = ?";
        try {
            Boolean enabled = jdbcTemplate.queryForObject(sql, Boolean.class, id);
            return Boolean.TRUE.equals(enabled);
        } catch (EmptyResultDataAccessException e) {
            return false;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<User> findByMobileNumber(String mobileNumber) {
        if (mobileNumber == null || mobileNumber.isBlank()) {
            return Optional.empty();
        }
        String sql = BASE_USER_SELECT + " WHERE u.mobile_number = ? OR u.phone = ?";
        List<User> list = jdbcTemplate.query(sql, userRowMapper, mobileNumber.trim(), mobileNumber.trim());
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }

    @Override
    @Transactional
    public int updateMobileVerification(UUID id, String mobileNumber, boolean isVerified) {
        String sql = "UPDATE users SET mobile_number = ?, phone = COALESCE(phone, ?), is_mobile_verified = ?, updated_at = ? WHERE id = ?";
        int updated = jdbcTemplate.update(sql, mobileNumber, mobileNumber, isVerified, Timestamp.from(Instant.now()), id);
        log.debug("Updated mobile verification to {} for user ID {} (affected rows: {})", isVerified, id, updated);
        return updated;
    }

    @Override
    @Transactional
    public int updateReputation(UUID id, int genuineCount, boolean isVerifiedCitizen) {
        String sql = "UPDATE users SET genuine_complaint_count = ?, is_verified_citizen = ?, updated_at = ? WHERE id = ?";
        int updated = jdbcTemplate.update(sql, genuineCount, isVerifiedCitizen, Timestamp.from(Instant.now()), id);
        log.debug("Updated reputation for user ID {}: genuineCount={}, isVerifiedCitizen={} (affected rows: {})", id, genuineCount, isVerifiedCitizen, updated);
        return updated;
    }

    @Override
    @Transactional
    public void deleteById(UUID id) {
        String sql = "DELETE FROM users WHERE id = ?";
        jdbcTemplate.update(sql, id);
    }

    @Override
    @Transactional(readOnly = true)
    public long count() {
        String sql = "SELECT COUNT(*) FROM users";
        Long count = jdbcTemplate.queryForObject(sql, Long.class);
        return count != null ? count : 0L;
    }
}
