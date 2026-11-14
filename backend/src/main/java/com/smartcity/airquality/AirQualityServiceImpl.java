package com.smartcity.airquality;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartcity.airquality.dto.AirQualityResponse;
import com.smartcity.airquality.dto.OpenMeteoAirQualityResponse;
import com.smartcity.iot.IoTProcessingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

@Service
public class AirQualityServiceImpl implements AirQualityService {

    private static final Logger log = LoggerFactory.getLogger(AirQualityServiceImpl.class);

    private final AirQualityObservationRepository airQualityRepository;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final RestClient restClient;

    @Autowired(required = false)
    @Lazy
    private IoTProcessingService ioTProcessingService;

    @Value("${app.air-quality.api-url:https://air-quality-api.open-meteo.com/v1/air-quality}")
    private String apiUrl;

    @Value("${app.air-quality.default-latitude:30.7333}")
    private Double defaultLatitude;

    @Value("${app.air-quality.default-longitude:76.7794}")
    private Double defaultLongitude;

    @Value("${app.air-quality.pm25-alert-threshold:150.0}")
    private Double pm25AlertThreshold;

    @Value("${app.air-quality.pm10-alert-threshold:250.0}")
    private Double pm10AlertThreshold;

    public AirQualityServiceImpl(
            AirQualityObservationRepository airQualityRepository,
            @Autowired(required = false) StringRedisTemplate redisTemplate,
            ObjectMapper objectMapper
    ) {
        this.airQualityRepository = airQualityRepository;
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;

        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(4));
        factory.setReadTimeout(Duration.ofSeconds(4));

        this.restClient = RestClient.builder()
                .requestFactory(factory)
                .build();
    }

    public void setIoTProcessingService(IoTProcessingService ioTProcessingService) {
        this.ioTProcessingService = ioTProcessingService;
    }

    @Override
    public AirQualityResponse getCurrentAirQuality(Double latitude, Double longitude) {
        double lat = (latitude != null) ? latitude : defaultLatitude;
        double lon = (longitude != null) ? longitude : defaultLongitude;

        String cacheKey = String.format("airquality:%.4f:%.4f", lat, lon);

        // 1. Check Redis Cache
        if (redisTemplate != null) {
            try {
                String cachedJson = redisTemplate.opsForValue().get(cacheKey);
                if (cachedJson != null && !cachedJson.isBlank()) {
                    AirQualityResponse cached = objectMapper.readValue(cachedJson, AirQualityResponse.class);
                    cached.setCached(true);
                    return cached;
                }
            } catch (Exception e) {
                log.warn("Redis cache read failed for air quality key {}: {}", cacheKey, e.getMessage());
            }
        }

        // 2. Fetch from Open-Meteo Air Quality API
        try {
            String url = String.format("%s?latitude=%.4f&longitude=%.4f&current=pm10,pm2_5,european_aqi,us_aqi",
                    apiUrl, lat, lon);

            OpenMeteoAirQualityResponse apiResponse = restClient.get()
                    .uri(url)
                    .retrieve()
                    .body(OpenMeteoAirQualityResponse.class);

            if (apiResponse != null && apiResponse.getCurrent() != null) {
                OpenMeteoAirQualityResponse.CurrentAirQuality cur = apiResponse.getCurrent();

                double pm25 = cur.getPm25() != null ? cur.getPm25() : 0.0;
                double pm10 = cur.getPm10() != null ? cur.getPm10() : 0.0;
                int aqi = cur.getUsAqi() != null ? cur.getUsAqi().intValue()
                        : (cur.getEuropeanAqi() != null ? cur.getEuropeanAqi().intValue() : calculateAqi(pm25));

                String category = deriveCategory(aqi, pm25);
                boolean thresholdCrossed = (pm25 >= pm25AlertThreshold || pm10 >= pm10AlertThreshold);

                AirQualityResponse response = new AirQualityResponse(
                        pm25,
                        pm10,
                        aqi,
                        category,
                        lat,
                        lon,
                        Instant.now(),
                        thresholdCrossed,
                        false
                );

                // Cache in Redis for 10 minutes
                if (redisTemplate != null) {
                    try {
                        redisTemplate.opsForValue().set(cacheKey, objectMapper.writeValueAsString(response), Duration.ofMinutes(10));
                    } catch (Exception e) {
                        log.warn("Redis cache write failed: {}", e.getMessage());
                    }
                }

                // Save observation to MongoDB
                saveObservationToMongo(response);

                // If thresholds crossed or for regular telemetry stream, route through existing IoT pipeline
                if (ioTProcessingService != null) {
                    routeReadingToIoTPipeline(pm25, lat, lon);
                }

                return response;
            }
        } catch (Exception e) {
            log.warn("Failed to fetch live air quality from {}: {}. Falling back to historical observation.", apiUrl, e.getMessage());
        }

        // 3. Fallback to latest observation from MongoDB
        Optional<AirQualityObservation> latest = airQualityRepository.findTopByOrderByTimestampDesc();
        if (latest.isPresent()) {
            AirQualityObservation obs = latest.get();
            return new AirQualityResponse(
                    obs.getPm25(),
                    obs.getPm10(),
                    obs.getAqi(),
                    obs.getCategory(),
                    obs.getLatitude() != null ? obs.getLatitude() : lat,
                    obs.getLongitude() != null ? obs.getLongitude() : lon,
                    obs.getTimestamp(),
                    obs.isThresholdCrossed(),
                    true
            );
        }

        // Final fallback default
        return new AirQualityResponse(35.0, 55.0, 75, "MODERATE", lat, lon, Instant.now(), false, false);
    }

    @Override
    public List<AirQualityObservation> getHistoricalAirQuality(int hours) {
        Instant start = Instant.now().minus(hours > 0 ? hours : 24, ChronoUnit.HOURS);
        return airQualityRepository.findByTimestampBetweenOrderByTimestampAsc(start, Instant.now());
    }

    @Override
    @Scheduled(cron = "${app.air-quality.fetch-cron:0 */15 * * * *}")
    public void fetchAndRecordObservation() {
        log.info("Running scheduled air quality fetch for city centre [{}, {}]...", defaultLatitude, defaultLongitude);
        try {
            getCurrentAirQuality(defaultLatitude, defaultLongitude);
        } catch (Exception e) {
            log.error("Scheduled air quality observation fetch error: {}", e.getMessage());
        }
    }

    private void routeReadingToIoTPipeline(double pm25, double lat, double lon) {
        try {
            String topic = "smartcity/sensors/AQ-METEO-CITY/data";
            String payload = String.format(
                    "{\"sensorId\":\"AQ-METEO-CITY\",\"sensorType\":\"PM25\",\"value\":%.1f,\"unit\":\"µg/m³\",\"latitude\":%.4f,\"longitude\":%.4f,\"location\":\"City Meteorological Air Station\"}",
                    pm25, lat, lon
            );
            ioTProcessingService.processDataMessage(topic, payload);
        } catch (Exception e) {
            log.warn("Failed to route external air quality reading into IoT processing pipeline: {}", e.getMessage());
        }
    }

    private void saveObservationToMongo(AirQualityResponse aq) {
        try {
            AirQualityObservation obs = new AirQualityObservation(
                    aq.getLatitude(),
                    aq.getLongitude(),
                    aq.getPm25(),
                    aq.getPm10(),
                    aq.getAqi(),
                    aq.getCategory(),
                    aq.isThresholdCrossed(),
                    aq.getTimestamp()
            );
            airQualityRepository.save(obs);
        } catch (Exception e) {
            log.warn("Failed to record air quality observation in MongoDB: {}", e.getMessage());
        }
    }

    private int calculateAqi(double pm25) {
        if (pm25 <= 12.0) return (int) Math.round((50.0 / 12.0) * pm25);
        if (pm25 <= 35.4) return 50 + (int) Math.round(((100.0 - 50.0) / (35.4 - 12.1)) * (pm25 - 12.1));
        if (pm25 <= 55.4) return 100 + (int) Math.round(((150.0 - 100.0) / (55.4 - 35.5)) * (pm25 - 35.5));
        if (pm25 <= 150.4) return 150 + (int) Math.round(((200.0 - 150.0) / (150.4 - 55.5)) * (pm25 - 55.5));
        if (pm25 <= 250.4) return 200 + (int) Math.round(((300.0 - 200.0) / (250.4 - 150.5)) * (pm25 - 150.5));
        return 300 + (int) Math.round(((500.0 - 300.0) / (500.4 - 250.5)) * (pm25 - 250.5));
    }

    private String deriveCategory(int aqi, double pm25) {
        if (aqi > 300 || pm25 > 250) return "HAZARDOUS";
        if (aqi > 200 || pm25 > 150) return "VERY_UNHEALTHY";
        if (aqi > 150 || pm25 > 55) return "UNHEALTHY";
        if (aqi > 100 || pm25 > 35) return "UNHEALTHY_SENSITIVE";
        if (aqi > 50 || pm25 > 12) return "MODERATE";
        return "GOOD";
    }
}
