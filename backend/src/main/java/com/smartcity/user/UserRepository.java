package com.smartcity.user;

/**
 * Repository interface for User management and authentication.
 * Backed by Spring JdbcTemplate with explicit parameterized queries.
 */
public interface UserRepository extends UserDao {
}
