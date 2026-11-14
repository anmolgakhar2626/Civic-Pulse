package com.smartcity.map;

import com.smartcity.map.dto.MapIncidentMarkerResponse;
import com.smartcity.map.dto.MapInfrastructureResponse;
import com.smartcity.map.dto.MapShelterResponse;
import com.smartcity.map.dto.MapSummaryResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
public class MapServiceImpl implements MapService {

    private static final Logger log = LoggerFactory.getLogger(MapServiceImpl.class);

    private final MapDao mapDao;

    public MapServiceImpl(MapDao mapDao) {
        this.mapDao = mapDao;
    }

    @Override
    @Transactional(readOnly = true)
    public List<MapIncidentMarkerResponse> getIncidentMarkers(
            Double minLat,
            Double maxLat,
            Double minLng,
            Double maxLng,
            String category,
            String severity,
            String status,
            Instant fromDate,
            Instant toDate,
            Integer limit
    ) {
        BoundingBox bbox = validateAndNormalizeBbox(minLat, maxLat, minLng, maxLng);
        return mapDao.findIncidents(
                bbox.minLat(), bbox.maxLat(), bbox.minLng(), bbox.maxLng(),
                category, severity, status, fromDate, toDate, limit
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<MapInfrastructureResponse> getInfrastructureMarkers(
            Double minLat,
            Double maxLat,
            Double minLng,
            Double maxLng,
            String category,
            String status,
            Integer limit
    ) {
        BoundingBox bbox = validateAndNormalizeBbox(minLat, maxLat, minLng, maxLng);
        return mapDao.findInfrastructure(
                bbox.minLat(), bbox.maxLat(), bbox.minLng(), bbox.maxLng(),
                category, status, limit
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<MapShelterResponse> getShelterMarkers(
            Double minLat,
            Double maxLat,
            Double minLng,
            Double maxLng,
            Boolean availableOnly,
            Integer limit
    ) {
        BoundingBox bbox = validateAndNormalizeBbox(minLat, maxLat, minLng, maxLng);
        return mapDao.findShelters(
                bbox.minLat(), bbox.maxLat(), bbox.minLng(), bbox.maxLng(),
                availableOnly, limit
        );
    }

    @Override
    @Transactional(readOnly = true)
    public MapSummaryResponse getMapSummary() {
        return mapDao.getSummary();
    }

    private record BoundingBox(Double minLat, Double maxLat, Double minLng, Double maxLng) {}

    private BoundingBox validateAndNormalizeBbox(Double minLat, Double maxLat, Double minLng, Double maxLng) {
        if (minLat == null && maxLat == null && minLng == null && maxLng == null) {
            return new BoundingBox(null, null, null, null);
        }

        if (minLat == null || maxLat == null || minLng == null || maxLng == null) {
            throw new IllegalArgumentException("Bounding box parameters (minLat, maxLat, minLng, maxLng) must either all be provided or all omitted.");
        }

        if (minLat < -90.0 || minLat > 90.0 || maxLat < -90.0 || maxLat > 90.0) {
            throw new IllegalArgumentException("Latitude values must be between -90.0 and 90.0");
        }

        if (minLng < -180.0 || minLng > 180.0 || maxLng < -180.0 || maxLng > 180.0) {
            throw new IllegalArgumentException("Longitude values must be between -180.0 and 180.0");
        }

        double normalizedMinLat = Math.min(minLat, maxLat);
        double normalizedMaxLat = Math.max(minLat, maxLat);
        double normalizedMinLng = Math.min(minLng, maxLng);
        double normalizedMaxLng = Math.max(minLng, maxLng);

        return new BoundingBox(normalizedMinLat, normalizedMaxLat, normalizedMinLng, normalizedMaxLng);
    }
}

