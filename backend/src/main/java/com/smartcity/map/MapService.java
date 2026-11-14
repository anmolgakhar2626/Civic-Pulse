package com.smartcity.map;

import com.smartcity.map.dto.MapIncidentMarkerResponse;
import com.smartcity.map.dto.MapInfrastructureResponse;
import com.smartcity.map.dto.MapShelterResponse;
import com.smartcity.map.dto.MapSummaryResponse;

import java.time.Instant;
import java.util.List;

public interface MapService {

    List<MapIncidentMarkerResponse> getIncidentMarkers(
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
    );

    List<MapInfrastructureResponse> getInfrastructureMarkers(
            Double minLat,
            Double maxLat,
            Double minLng,
            Double maxLng,
            String category,
            String status,
            Integer limit
    );

    List<MapShelterResponse> getShelterMarkers(
            Double minLat,
            Double maxLat,
            Double minLng,
            Double maxLng,
            Boolean availableOnly,
            Integer limit
    );

    MapSummaryResponse getMapSummary();
}

