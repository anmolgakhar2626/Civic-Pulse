package com.smartcity.infrastructure;

import com.smartcity.common.DuplicateResourceException;
import com.smartcity.common.PagedResponse;
import com.smartcity.common.ResourceNotFoundException;
import com.smartcity.infrastructure.dto.CreateInfrastructureRequest;
import com.smartcity.infrastructure.dto.InfrastructureResponse;
import com.smartcity.infrastructure.dto.UpdateInfrastructureRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class InfrastructureServiceImpl implements InfrastructureService {

    private static final Logger log = LoggerFactory.getLogger(InfrastructureServiceImpl.class);

    private final InfrastructureDao infrastructureDao;

    public InfrastructureServiceImpl(InfrastructureDao infrastructureDao) {
        this.infrastructureDao = infrastructureDao;
    }

    @Override
    @Transactional
    public InfrastructureResponse createInfrastructure(CreateInfrastructureRequest request) {
        String code = request.getCode();
        if (code == null || code.isBlank()) {
            String prefix = request.getCategory() != null ? request.getCategory().toUpperCase() : "INF";
            if (prefix.length() > 4) {
                prefix = prefix.substring(0, 4);
            }
            code = "INF-" + prefix + "-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        } else {
            code = code.trim().toUpperCase();
            if (infrastructureDao.existsByCode(code)) {
                throw new DuplicateResourceException("Infrastructure", "code", code);
            }
        }

        Infrastructure inf = new Infrastructure();
        inf.setCode(code);
        inf.setName(request.getName().trim());
        inf.setInfrastructureType(normalizeCategory(request.getCategory()));
        inf.setStatus(normalizeStatus(request.getStatus()));
        inf.setLatitude(request.getLatitude());
        inf.setLongitude(request.getLongitude());
        inf.setAddress(request.getAddress());
        inf.setImportanceLevel(request.getImportanceLevel() != null ? request.getImportanceLevel().toUpperCase() : "PRIMARY");
        inf.setMaintenancePriority(request.getMaintenancePriority() != null ? request.getMaintenancePriority().toUpperCase() : "MEDIUM");
        inf.setLastInspectedAt(request.getLastInspectedAt() != null ? request.getLastInspectedAt() : Instant.now());
        inf.setNotes(request.getNotes());
        inf.setCreatedAt(Instant.now());
        inf.setUpdatedAt(Instant.now());

        Infrastructure saved = infrastructureDao.save(inf);
        log.info("Created infrastructure asset id={} code={} name='{}'", saved.getId(), saved.getCode(), saved.getName());
        return mapToResponse(saved);
    }

    @Override
    @Transactional
    public InfrastructureResponse updateInfrastructure(UUID id, UpdateInfrastructureRequest request) {
        Infrastructure inf = infrastructureDao.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Infrastructure", "id", id));

        if (request.getName() != null && !request.getName().isBlank()) {
            inf.setName(request.getName().trim());
        }
        if (request.getCategory() != null && !request.getCategory().isBlank()) {
            inf.setInfrastructureType(normalizeCategory(request.getCategory()));
        }
        if (request.getStatus() != null && !request.getStatus().isBlank()) {
            inf.setStatus(normalizeStatus(request.getStatus()));
        }
        if (request.getLatitude() != null) {
            inf.setLatitude(request.getLatitude());
        }
        if (request.getLongitude() != null) {
            inf.setLongitude(request.getLongitude());
        }
        if (request.getAddress() != null) {
            inf.setAddress(request.getAddress().trim());
        }
        if (request.getImportanceLevel() != null && !request.getImportanceLevel().isBlank()) {
            inf.setImportanceLevel(request.getImportanceLevel().toUpperCase());
        }
        if (request.getMaintenancePriority() != null && !request.getMaintenancePriority().isBlank()) {
            inf.setMaintenancePriority(request.getMaintenancePriority().toUpperCase());
        }
        if (request.getLastInspectedAt() != null) {
            inf.setLastInspectedAt(request.getLastInspectedAt());
        }
        if (request.getNotes() != null) {
            inf.setNotes(request.getNotes().trim());
        }

        inf.setUpdatedAt(Instant.now());
        Infrastructure updated = infrastructureDao.update(inf);
        log.info("Updated infrastructure asset id={} code={}", updated.getId(), updated.getCode());
        return mapToResponse(updated);
    }

    @Override
    @Transactional(readOnly = true)
    public InfrastructureResponse getInfrastructureById(UUID id) {
        Infrastructure inf = infrastructureDao.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Infrastructure", "id", id));
        return mapToResponse(inf);
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<InfrastructureResponse> getInfrastructureList(
            String category,
            String status,
            String search,
            Double minLat,
            Double maxLat,
            Double minLng,
            Double maxLng,
            int page,
            int size
    ) {
        int safePage = Math.max(0, page);
        int safeSize = Math.max(1, Math.min(size, 200));
        int offset = safePage * safeSize;

        long totalElements = infrastructureDao.count(category, status, search, minLat, maxLat, minLng, maxLng);
        List<Infrastructure> list = infrastructureDao.findAll(
                category, status, search, minLat, maxLat, minLng, maxLng, safeSize, offset
        );

        List<InfrastructureResponse> responses = list.stream().map(this::mapToResponse).toList();
        int totalPages = safeSize > 0 ? (int) Math.ceil((double) totalElements / safeSize) : 0;
        boolean first = safePage == 0;
        boolean last = safePage >= totalPages - 1;

        return new PagedResponse<>(responses, safePage, safeSize, totalElements, totalPages, first, last);
    }

    @Override
    @Transactional
    public void deleteInfrastructure(UUID id) {
        if (!infrastructureDao.findById(id).isPresent()) {
            throw new ResourceNotFoundException("Infrastructure", "id", id);
        }
        boolean deleted = infrastructureDao.deleteById(id);
        if (!deleted) {
            throw new ResourceNotFoundException("Infrastructure", "id", id);
        }
        log.info("Deleted infrastructure asset id={}", id);
    }

    public InfrastructureResponse mapToResponse(Infrastructure inf) {
        InfrastructureResponse resp = new InfrastructureResponse();
        resp.setId(inf.getId());
        resp.setCode(inf.getCode());
        resp.setName(inf.getName());
        resp.setCategory(inf.getInfrastructureType());
        resp.setInfrastructureType(inf.getInfrastructureType());
        resp.setStatus(inf.getStatus());
        resp.setLatitude(inf.getLatitude());
        resp.setLongitude(inf.getLongitude());
        resp.setAddress(inf.getAddress());
        resp.setImportanceLevel(inf.getImportanceLevel());
        resp.setMaintenancePriority(inf.getMaintenancePriority());
        resp.setLastInspectedAt(inf.getLastInspectedAt());
        resp.setNotes(inf.getNotes());
        resp.setCreatedAt(inf.getCreatedAt());
        resp.setUpdatedAt(inf.getUpdatedAt());
        return resp;
    }

    private String normalizeCategory(String category) {
        if (category == null || category.isBlank()) return "OTHER";
        String normalized = category.trim().toUpperCase().replace(" ", "_");
        return switch (normalized) {
            case "ROADS", "ROAD", "BRIDGES", "BRIDGE" -> "BRIDGE".equals(normalized) ? "BRIDGE" : "ROAD";
            case "HOSPITALS", "HOSPITAL", "EMERGENCY", "EMERGENCY_FACILITY" -> "HOSPITAL";
            case "WATER", "WATER_AND_DRAINAGE", "DRAINAGE", "WATER_PIPELINE" ->
                    normalized.contains("DRAIN") ? "DRAINAGE" : "WATER_PIPELINE";
            case "POWER", "POWER_INFRASTRUCTURE", "SUBSTATION", "ELECTRIC" -> "SUBSTATION";
            case "SHELTER", "SHELTERS", "PUBLIC_SHELTER", "PUBLIC_SHELTERS" -> "SHELTER";
            default -> normalized;
        };
    }

    private String normalizeStatus(String status) {
        if (status == null || status.isBlank()) return "OPERATIONAL";
        String normalized = status.trim().toUpperCase().replace(" ", "_");
        return switch (normalized) {
            case "OPERATIONAL", "DEGRADED", "OFFLINE", "UNDER_MAINTENANCE" -> normalized;
            case "MAINTENANCE" -> "UNDER_MAINTENANCE";
            case "DOWN" -> "OFFLINE";
            default -> "OPERATIONAL";
        };
    }
}

