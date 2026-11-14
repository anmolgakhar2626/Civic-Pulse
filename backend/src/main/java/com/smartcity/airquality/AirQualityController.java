package com.smartcity.airquality;

import com.smartcity.airquality.dto.AirQualityResponse;
import com.smartcity.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/air-quality")
@Tag(name = "Air Quality", description = "Municipal Air Quality telemetry and monitoring API")
public class AirQualityController {

    private final AirQualityService airQualityService;

    public AirQualityController(AirQualityService airQualityService) {
        this.airQualityService = airQualityService;
    }

    @GetMapping("/current")
    @Operation(summary = "Get current air quality observation", description = "Retrieves live PM2.5, PM10, and AQI via Open-Meteo with Redis caching")
    public ResponseEntity<ApiResponse<AirQualityResponse>> getCurrentAirQuality(
            @RequestParam(required = false) Double latitude,
            @RequestParam(required = false) Double longitude
    ) {
        AirQualityResponse data = airQualityService.getCurrentAirQuality(latitude, longitude);
        return ResponseEntity.ok(ApiResponse.success(data, "Current air quality retrieved successfully"));
    }

    @GetMapping("/history")
    @Operation(summary = "Get historical air quality observations", description = "Returns stored observations from MongoDB")
    public ResponseEntity<ApiResponse<List<AirQualityObservation>>> getHistoricalAirQuality(
            @RequestParam(defaultValue = "24") int hours
    ) {
        List<AirQualityObservation> data = airQualityService.getHistoricalAirQuality(hours);
        return ResponseEntity.ok(ApiResponse.success(data, "Historical air quality observations retrieved"));
    }
}
