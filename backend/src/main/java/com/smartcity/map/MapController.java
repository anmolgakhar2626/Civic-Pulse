package com.smartcity.map;

import com.smartcity.map.dto.MapIncidentMarkerResponse;
import com.smartcity.map.dto.MapInfrastructureResponse;
import com.smartcity.map.dto.MapShelterResponse;
import com.smartcity.map.dto.MapSummaryResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/map")
@Tag(name = "GIS Map", description = "GIS Map visualization endpoints for incidents, infrastructure, and public shelters")
public class MapController {

    private final MapService mapService;

    public MapController(MapService mapService) {
        this.mapService = mapService;
    }

    @GetMapping("/incidents")
    @Operation(summary = "Get incidents for map visualization with bounding-box and category/severity/status filters")
    public ResponseEntity<List<MapIncidentMarkerResponse>> getMapIncidents(
            @Parameter(description = "Minimum latitude of map viewport")
            @RequestParam(required = false) Double minLat,
            @Parameter(description = "Maximum latitude of map viewport")
            @RequestParam(required = false) Double maxLat,
            @Parameter(description = "Minimum longitude of map viewport")
            @RequestParam(required = false) Double minLng,
            @Parameter(description = "Maximum longitude of map viewport")
            @RequestParam(required = false) Double maxLng,
            @Parameter(description = "Filter by category")
            @RequestParam(required = false) String category,
            @Parameter(description = "Filter by severity: CRITICAL, HIGH, MEDIUM, LOW")
            @RequestParam(required = false) String severity,
            @Parameter(description = "Filter by status: OPEN, IN_PROGRESS, RESOLVED, CLOSED")
            @RequestParam(required = false) String status,
            @Parameter(description = "Filter created after timestamp")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant fromDate,
            @Parameter(description = "Filter created before timestamp")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant toDate,
            @Parameter(description = "Maximum number of markers to return (default 200, max 1000)")
            @RequestParam(required = false, defaultValue = "200") Integer limit
    ) {
        List<MapIncidentMarkerResponse> markers = mapService.getIncidentMarkers(
                minLat, maxLat, minLng, maxLng, category, severity, status, fromDate, toDate, limit
        );
        return ResponseEntity.ok(markers);
    }

    @GetMapping("/infrastructure")
    @Operation(summary = "Get infrastructure assets for map visualization with optional bounding-box and category filters")
    public ResponseEntity<List<MapInfrastructureResponse>> getMapInfrastructure(
            @Parameter(description = "Minimum latitude of map viewport")
            @RequestParam(required = false) Double minLat,
            @Parameter(description = "Maximum latitude of map viewport")
            @RequestParam(required = false) Double maxLat,
            @Parameter(description = "Minimum longitude of map viewport")
            @RequestParam(required = false) Double minLng,
            @Parameter(description = "Maximum longitude of map viewport")
            @RequestParam(required = false) Double maxLng,
            @Parameter(description = "Filter by category: ROAD, BRIDGE, HOSPITAL, WATER_PIPELINE, DRAINAGE, SUBSTATION, SHELTER")
            @RequestParam(required = false) String category,
            @Parameter(description = "Filter by status: OPERATIONAL, DEGRADED, OFFLINE, UNDER_MAINTENANCE")
            @RequestParam(required = false) String status,
            @Parameter(description = "Maximum number of assets to return (default 100, max 500)")
            @RequestParam(required = false, defaultValue = "100") Integer limit
    ) {
        List<MapInfrastructureResponse> assets = mapService.getInfrastructureMarkers(
                minLat, maxLat, minLng, maxLng, category, status, limit
        );
        return ResponseEntity.ok(assets);
    }

    @GetMapping("/shelters")
    @Operation(summary = "Get emergency shelters and public refuge facilities for map visualization")
    public ResponseEntity<List<MapShelterResponse>> getMapShelters(
            @Parameter(description = "Minimum latitude of map viewport")
            @RequestParam(required = false) Double minLat,
            @Parameter(description = "Maximum latitude of map viewport")
            @RequestParam(required = false) Double maxLat,
            @Parameter(description = "Minimum longitude of map viewport")
            @RequestParam(required = false) Double minLng,
            @Parameter(description = "Maximum longitude of map viewport")
            @RequestParam(required = false) Double maxLng,
            @Parameter(description = "Filter only available shelters")
            @RequestParam(required = false) Boolean availableOnly,
            @Parameter(description = "Maximum number of shelters to return (default 50, max 200)")
            @RequestParam(required = false, defaultValue = "50") Integer limit
    ) {
        List<MapShelterResponse> shelters = mapService.getShelterMarkers(
                minLat, maxLat, minLng, maxLng, availableOnly, limit
        );
        return ResponseEntity.ok(shelters);
    }

    @GetMapping("/summary")
    @Operation(summary = "Get aggregated summary statistics and KPI metrics for GIS operations map")
    public ResponseEntity<MapSummaryResponse> getMapSummary() {
        return ResponseEntity.ok(mapService.getMapSummary());
    }
}

