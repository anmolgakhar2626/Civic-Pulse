package com.smartcity.user;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Data Access Object interface for User entities using Spring JdbcTemplate.
 * Provides explicit, parameterized database interactions for registration,
 * user lookup, account status, role retrieval, and profile updates.
 */
public interface UserDao {

    /**
     * Inserts or updates a user using parameterized SQL.
     */
    User save(User user);

    /**
     * Finds a user by primary key UUID.
     */
    Optional<User> findById(UUID id);

    /**
     * Finds a user by email address (case-insensitive).
     */
    Optional<User> findByEmail(String email);

    /**
     * Finds a user by username (case-insensitive).
     */
    Optional<User> findByUsername(String username);

    /**
     * Finds a user by either username or email address.
     */
    Optional<User> findByUsernameOrEmail(String identifier);

    /**
     * Checks if a user exists with the given email address.
     */
    boolean existsByEmail(String email);

    /**
     * Checks if a user exists with the given username.
     */
    boolean existsByUsername(String username);

    /**
     * Retrieves all users.
     */
    List<User> findAll();

    /**
     * Updates user profile attributes (full_name, phone) via parameterized SQL.
     */
    int updateProfile(UUID id, String fullName, String phone);

    /**
     * Updates user password hash via parameterized SQL.
     */
    int updatePassword(UUID id, String newPasswordHash);

    /**
     * Updates account enabled/active status via parameterized SQL.
     */
    int updateAccountStatus(UUID id, boolean enabled);

    /**
     * Updates user role via parameterized SQL.
     */
    int updateRole(UUID id, String role);

    /**
     * Retrieves the primary municipal role for a user ID.
     */
    Optional<String> findRoleByUserId(UUID id);

    /**
     * Checks whether an account is enabled by user ID.
     */
    boolean isAccountEnabled(UUID id);

    /**
     * Finds a user by mobile number.
     */
    Optional<User> findByMobileNumber(String mobileNumber);

    /**
     * Updates mobile number verification status.
     */
    int updateMobileVerification(UUID id, String mobileNumber, boolean isVerified);

    /**
     * Updates genuine complaint count and verified citizen badge eligibility.
     */
    int updateReputation(UUID id, int genuineCount, boolean isVerifiedCitizen);

    /**
     * Deletes user by ID.
     */
    void deleteById(UUID id);

    /**
     * Returns total user count.
     */
    long count();
}

