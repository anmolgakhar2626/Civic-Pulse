package com.smartcity.incident;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Data Access Object interface for Incident persistence using Spring JdbcTemplate.
 * Provides explicit, parameterized SQL queries for incident ingestion,
 * retrieval, paginated search and filters, updates, and role-based deletions.
 */
public interface IncidentDao {

    /**
     * Persists or updates an incident record using parameterized SQL.
     */
    Incident save(Incident incident);

    /**
     * Finds an incident by primary key UUID.
     */
    Optional<Incident> findById(UUID id);

    /**
     * Finds an incident by unique incident number (e.g. INC-20261028-XXXXXXXX).
     */
    Optional<Incident> findByIncidentNumber(String incidentNumber);

    /**
     * Checks if an incident exists with the specified UUID.
     */
    boolean existsById(UUID id);

    /**
     * Updates incident mutable fields (title, description, category, severity, coordinates, location, assignedTo).
     */
    int update(UUID id, Incident incident);

    /**
     * Updates incident status with remarks and resolution timestamp handling.
     */
    int updateStatus(UUID id, String status, String remarks, String changedBy);

    /**
     * Deletes an incident record by primary key UUID.
     */
    int deleteById(UUID id);

    /**
     * Retrieves a paginated and filtered list of incidents using parameterized SQL.
     */
    List<Incident> findPaginatedAndFiltered(String search, String category, String severity, String status, int offset, int limit);

    /**
     * Counts the total number of incidents matching the search and filter criteria.
     */
    long countFiltered(String search, String category, String severity, String status);

    /**
     * Retrieves all incidents.
     */
    List<Incident> findAll();
}

