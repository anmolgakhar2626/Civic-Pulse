package com.smartcity.weather;

import com.smartcity.common.ApiResponse;
import com.smartcity.weather.dto.WeatherResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/weather")
@Tag(name = "Weather", description = "Municipal Weather observation and forecast API")
public class WeatherController {

    private final WeatherService weatherService;

    public WeatherController(WeatherService weatherService) {
        this.weatherService = weatherService;
    }

    @GetMapping("/current")
    @Operation(summary = "Get current weather observation", description = "Retrieves live weather conditions via Open-Meteo with Redis caching")
    public ResponseEntity<ApiResponse<WeatherResponse>> getCurrentWeather(
            @RequestParam(required = false) Double latitude,
            @RequestParam(required = false) Double longitude
    ) {
        WeatherResponse data = weatherService.getCurrentWeather(latitude, longitude);
        return ResponseEntity.ok(ApiResponse.success(data, "Current weather retrieved successfully"));
    }

    @GetMapping("/history")
    @Operation(summary = "Get historical weather observations", description = "Returns stored observations from MongoDB")
    public ResponseEntity<ApiResponse<List<WeatherObservation>>> getHistoricalWeather(
            @RequestParam(defaultValue = "24") int hours
    ) {
        List<WeatherObservation> data = weatherService.getHistoricalWeather(hours);
        return ResponseEntity.ok(ApiResponse.success(data, "Historical weather observations retrieved"));
    }
}
