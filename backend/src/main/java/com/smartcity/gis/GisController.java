package com.smartcity.gis;

import com.smartcity.gis.dto.GeoJsonFeatureCollection;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/gis")
@Tag(name = "GIS", description = "GIS Spatial endpoints and GeoJSON mapping layers")
public class GisController {

    private final GisService gisService;

    public GisController(GisService gisService) {
        this.gisService = gisService;
    }

    @GetMapping("/incidents")
    @Operation(summary = "Get all incidents as GeoJSON FeatureCollection")
    public ResponseEntity<GeoJsonFeatureCollection> getIncidents() {
        return ResponseEntity.ok(gisService.getIncidentsAsGeoJson());
    }

    @GetMapping("/sensors")
    @Operation(summary = "Get all IoT sensors as GeoJSON FeatureCollection")
    public ResponseEntity<GeoJsonFeatureCollection> getSensors() {
        return ResponseEntity.ok(gisService.getSensorsAsGeoJson());
    }

    @GetMapping("/infrastructure")
    @Operation(summary = "Get all municipal infrastructure, traffic signals, waste bins, and public facilities as GeoJSON")
    public ResponseEntity<GeoJsonFeatureCollection> getInfrastructure() {
        return ResponseEntity.ok(gisService.getInfrastructureAsGeoJson());
    }

    @GetMapping("/nearby-incidents")
    @Operation(summary = "Find nearby incidents within a given radius using PostGIS spatial queries")
    public ResponseEntity<GeoJsonFeatureCollection> getNearbyIncidents(
            @Parameter(description = "Latitude of point", required = true)
            @RequestParam("latitude") Double latitude,
            @Parameter(description = "Longitude of point", required = true)
            @RequestParam("longitude") Double longitude,
            @Parameter(description = "Radius in meters (default 5000)")
            @RequestParam(value = "radius", required = false, defaultValue = "5000") Double radius
    ) {
        return ResponseEntity.ok(gisService.getNearbyIncidents(latitude, longitude, radius));
    }

    @GetMapping("/nearby-resources")
    @Operation(summary = "Find nearby emergency resources within a given radius using PostGIS spatial queries")
    public ResponseEntity<GeoJsonFeatureCollection> getNearbyResources(
            @Parameter(description = "Latitude of point", required = true)
            @RequestParam("latitude") Double latitude,
            @Parameter(description = "Longitude of point", required = true)
            @RequestParam("longitude") Double longitude,
            @Parameter(description = "Radius in meters (default 5000)")
            @RequestParam(value = "radius", required = false, defaultValue = "5000") Double radius
    ) {
        return ResponseEntity.ok(gisService.getNearbyResources(latitude, longitude, radius));
    }
}
