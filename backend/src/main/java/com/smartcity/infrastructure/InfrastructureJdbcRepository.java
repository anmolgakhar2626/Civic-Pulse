package com.smartcity.infrastructure;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Primary;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Spring JdbcTemplate implementation of InfrastructureDao.
 * Uses explicit, parameterized SQL queries for all persistence operations,
 * ensuring high performance, SQL injection prevention, and zero JPA overhead.
 */
@Repository
@Primary
public class InfrastructureJdbcRepository implements InfrastructureDao {

    private static final Logger log = LoggerFactory.getLogger(InfrastructureJdbcRepository.class);

    private final JdbcTemplate jdbcTemplate;

    public InfrastructureJdbcRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private final RowMapper<Infrastructure> infrastructureRowMapper = (rs, rowNum) -> {
        Infrastructure inf = new Infrastructure();

        String idStr = rs.getString("id");
        if (idStr != null) {
            inf.setId(UUID.fromString(idStr));
        }

        inf.setCode(rs.getString("code"));
        inf.setName(rs.getString("name"));
        inf.setInfrastructureType(rs.getString("infrastructure_type"));
        inf.setLatitude(rs.getDouble("latitude"));
        inf.setLongitude(rs.getDouble("longitude"));
        inf.setAddress(rs.getString("address"));
        inf.setStatus(rs.getString("status"));
        inf.setImportanceLevel(rs.getString("importance_level"));
        inf.setMetadataJson(rs.getString("metadata_json"));
        inf.setMaintenancePriority(rs.getString("maintenance_priority"));
        inf.setNotes(rs.getString("notes"));

        Timestamp lastInsp = rs.getTimestamp("last_inspected_at");
        if (lastInsp != null) {
            inf.setLastInspectedAt(lastInsp.toInstant());
        }

        Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null) {
            inf.setCreatedAt(createdAt.toInstant());
        }

        Timestamp updatedAt = rs.getTimestamp("updated_at");
        if (updatedAt != null) {
            inf.setUpdatedAt(updatedAt.toInstant());
        }

        return inf;
    };

    @Override
    public Infrastructure save(Infrastructure infrastructure) {
        if (infrastructure.getId() == null) {
            infrastructure.setId(UUID.randomUUID());
        }
        if (infrastructure.getCreatedAt() == null) {
            infrastructure.setCreatedAt(Instant.now());
        }
        if (infrastructure.getUpdatedAt() == null) {
            infrastructure.setUpdatedAt(Instant.now());
        }

        String sql = """
            INSERT INTO infrastructure (
                id, code, name, infrastructure_type, latitude, longitude,
                address, status, importance_level, last_inspected_at,
                maintenance_priority, notes, created_at, updated_at
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """;

        jdbcTemplate.update(
                sql,
                infrastructure.getId(),
                infrastructure.getCode(),
                infrastructure.getName(),
                infrastructure.getInfrastructureType(),
                infrastructure.getLatitude(),
                infrastructure.getLongitude(),
                infrastructure.getAddress(),
                infrastructure.getStatus(),
                infrastructure.getImportanceLevel(),
                infrastructure.getLastInspectedAt() != null ? Timestamp.from(infrastructure.getLastInspectedAt()) : null,
                infrastructure.getMaintenancePriority() != null ? infrastructure.getMaintenancePriority() : "MEDIUM",
                infrastructure.getNotes(),
                Timestamp.from(infrastructure.getCreatedAt()),
                Timestamp.from(infrastructure.getUpdatedAt())
        );

        log.debug("Saved infrastructure asset id={} code={}", infrastructure.getId(), infrastructure.getCode());
        return infrastructure;
    }

    @Override
    public Optional<Infrastructure> findById(UUID id) {
        if (id == null) return Optional.empty();
        String sql = "SELECT * FROM infrastructure WHERE id = ?";
        try {
            Infrastructure inf = jdbcTemplate.queryForObject(sql, infrastructureRowMapper, id);
            return Optional.ofNullable(inf);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    @Override
    public Optional<Infrastructure> findByCode(String code) {
        if (code == null || code.isBlank()) return Optional.empty();
        String sql = "SELECT * FROM infrastructure WHERE code = ?";
        try {
            Infrastructure inf = jdbcTemplate.queryForObject(sql, infrastructureRowMapper, code.trim());
            return Optional.ofNullable(inf);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    @Override
    public List<Infrastructure> findAll(
            String category,
            String status,
            String search,
            Double minLat,
            Double maxLat,
            Double minLng,
            Double maxLng,
            int limit,
            int offset
    ) {
        StringBuilder sql = new StringBuilder("SELECT * FROM infrastructure WHERE 1=1 ");
        List<Object> params = new ArrayList<>();

        buildWhereConditions(sql, params, category, status, search, minLat, maxLat, minLng, maxLng);

        sql.append(" ORDER BY name ASC LIMIT ? OFFSET ?");
        params.add(Math.max(1, Math.min(limit, 500)));
        params.add(Math.max(0, offset));

        return jdbcTemplate.query(sql.toString(), infrastructureRowMapper, params.toArray());
    }

    @Override
    public long count(
            String category,
            String status,
            String search,
            Double minLat,
            Double maxLat,
            Double minLng,
            Double maxLng
    ) {
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM infrastructure WHERE 1=1 ");
        List<Object> params = new ArrayList<>();

        buildWhereConditions(sql, params, category, status, search, minLat, maxLat, minLng, maxLng);

        Long count = jdbcTemplate.queryForObject(sql.toString(), Long.class, params.toArray());
        return count != null ? count : 0L;
    }

    @Override
    public Infrastructure update(Infrastructure infrastructure) {
        if (infrastructure.getId() == null) {
            throw new IllegalArgumentException("Cannot update infrastructure asset without an ID");
        }
        infrastructure.setUpdatedAt(Instant.now());

        String sql = """
            UPDATE infrastructure SET
                name = ?,
                infrastructure_type = ?,
                latitude = ?,
                longitude = ?,
                address = ?,
                status = ?,
                importance_level = ?,
                last_inspected_at = ?,
                maintenance_priority = ?,
                notes = ?,
                updated_at = ?
            WHERE id = ?
        """;

        int rows = jdbcTemplate.update(
                sql,
                infrastructure.getName(),
                infrastructure.getInfrastructureType(),
                infrastructure.getLatitude(),
                infrastructure.getLongitude(),
                infrastructure.getAddress(),
                infrastructure.getStatus(),
                infrastructure.getImportanceLevel(),
                infrastructure.getLastInspectedAt() != null ? Timestamp.from(infrastructure.getLastInspectedAt()) : null,
                infrastructure.getMaintenancePriority(),
                infrastructure.getNotes(),
                Timestamp.from(infrastructure.getUpdatedAt()),
                infrastructure.getId()
        );

        if (rows == 0) {
            throw new IllegalArgumentException("Infrastructure asset not found with ID: " + infrastructure.getId());
        }

        return infrastructure;
    }

    @Override
    public boolean deleteById(UUID id) {
        if (id == null) return false;
        String sql = "DELETE FROM infrastructure WHERE id = ?";
        return jdbcTemplate.update(sql, id) > 0;
    }

    @Override
    public boolean existsByCode(String code) {
        if (code == null || code.isBlank()) return false;
        String sql = "SELECT COUNT(*) FROM infrastructure WHERE code = ?";
        Long count = jdbcTemplate.queryForObject(sql, Long.class, code.trim());
        return count != null && count > 0;
    }

    private void buildWhereConditions(
            StringBuilder sql,
            List<Object> params,
            String category,
            String status,
            String search,
            Double minLat,
            Double maxLat,
            Double minLng,
            Double maxLng
    ) {
        if (category != null && !category.isBlank()) {
            sql.append(" AND LOWER(infrastructure_type) = LOWER(?)");
            params.add(category.trim());
        }

        if (status != null && !status.isBlank()) {
            sql.append(" AND UPPER(status) = UPPER(?)");
            params.add(status.trim());
        }

        if (search != null && !search.isBlank()) {
            sql.append(" AND (LOWER(name) LIKE LOWER(?) OR LOWER(code) LIKE LOWER(?) OR LOWER(address) LIKE LOWER(?))");
            String searchPattern = "%" + search.trim() + "%";
            params.add(searchPattern);
            params.add(searchPattern);
            params.add(searchPattern);
        }

        if (minLat != null && maxLat != null && minLng != null && maxLng != null) {
            sql.append(" AND latitude BETWEEN ? AND ? AND longitude BETWEEN ? AND ?");
            params.add(Math.min(minLat, maxLat));
            params.add(Math.max(minLat, maxLat));
            params.add(Math.min(minLng, maxLng));
            params.add(Math.max(minLng, maxLng));
        }
    }
}

