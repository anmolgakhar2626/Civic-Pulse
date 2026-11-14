package com.smartcity.weather;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartcity.weather.dto.OpenMeteoWeatherResponse;
import com.smartcity.weather.dto.WeatherResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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
public class WeatherServiceImpl implements WeatherService {

    private static final Logger log = LoggerFactory.getLogger(WeatherServiceImpl.class);

    private final CityWeatherObservationRepository weatherRepository;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final RestClient restClient;

    @Value("${app.weather.api-url:https://api.open-meteo.com/v1/forecast}")
    private String apiUrl;

    @Value("${app.weather.default-latitude:30.7333}")
    private Double defaultLatitude;

    @Value("${app.weather.default-longitude:76.7794}")
    private Double defaultLongitude;

    public WeatherServiceImpl(
            CityWeatherObservationRepository weatherRepository,
            @Autowired(required = false) StringRedisTemplate redisTemplate,
            ObjectMapper objectMapper
    ) {
        this.weatherRepository = weatherRepository;
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;

        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(4));
        factory.setReadTimeout(Duration.ofSeconds(4));

        this.restClient = RestClient.builder()
                .requestFactory(factory)
                .build();
    }

    @Override
    public WeatherResponse getCurrentWeather(Double latitude, Double longitude) {
        double lat = (latitude != null) ? latitude : defaultLatitude;
        double lon = (longitude != null) ? longitude : defaultLongitude;

        String cacheKey = String.format("weather:%.4f:%.4f", lat, lon);

        // 1. Check Redis Cache
        if (redisTemplate != null) {
            try {
                String cachedJson = redisTemplate.opsForValue().get(cacheKey);
                if (cachedJson != null && !cachedJson.isBlank()) {
                    WeatherResponse cached = objectMapper.readValue(cachedJson, WeatherResponse.class);
                    cached.setCached(true);
                    return cached;
                }
            } catch (Exception e) {
                log.warn("Redis cache read failed for weather key {}: {}", cacheKey, e.getMessage());
            }
        }

        // 2. Fetch from Open-Meteo Weather API
        try {
            String url = String.format("%s?latitude=%.4f&longitude=%.4f&current=temperature_2m,relative_humidity_2m,precipitation,wind_speed_10m",
                    apiUrl, lat, lon);

            OpenMeteoWeatherResponse apiResponse = restClient.get()
                    .uri(url)
                    .retrieve()
                    .body(OpenMeteoWeatherResponse.class);

            if (apiResponse != null && apiResponse.getCurrent() != null) {
                OpenMeteoWeatherResponse.CurrentWeather cur = apiResponse.getCurrent();
                String condition = deriveCondition(cur.getPrecipitation(), cur.getTemperature2m());

                WeatherResponse response = new WeatherResponse(
                        cur.getTemperature2m(),
                        cur.getRelativeHumidity2m(),
                        cur.getPrecipitation(),
                        cur.getWindSpeed10m(),
                        lat,
                        lon,
                        Instant.now(),
                        condition,
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

                // Save observation asynchronously / synchronously to MongoDB
                saveObservationToMongo(response);

                return response;
            }
        } catch (Exception e) {
            log.warn("Failed to fetch live weather from {}: {}. Falling back to historical observation.", apiUrl, e.getMessage());
        }

        // 3. Fallback to latest observation from MongoDB
        Optional<WeatherObservation> latest = weatherRepository.findTopByOrderByTimestampDesc();
        if (latest.isPresent()) {
            WeatherObservation obs = latest.get();
            return new WeatherResponse(
                    obs.getTemperature(),
                    obs.getHumidity(),
                    obs.getPrecipitation(),
                    obs.getWindSpeed(),
                    obs.getLatitude() != null ? obs.getLatitude() : lat,
                    obs.getLongitude() != null ? obs.getLongitude() : lon,
                    obs.getTimestamp(),
                    obs.getCondition() != null ? obs.getCondition() : "Clear",
                    true
            );
        }

        // Final fallback with reasonable defaults
        return new WeatherResponse(24.0, 50.0, 0.0, 10.0, lat, lon, Instant.now(), "Clear", false);
    }

    @Override
    public List<WeatherObservation> getHistoricalWeather(int hours) {
        Instant start = Instant.now().minus(hours > 0 ? hours : 24, ChronoUnit.HOURS);
        return weatherRepository.findByTimestampBetweenOrderByTimestampAsc(start, Instant.now());
    }

    @Override
    @Scheduled(cron = "${app.weather.fetch-cron:0 */15 * * * *}")
    public void fetchAndRecordObservation() {
        log.info("Running scheduled weather observation fetch for city centre [{}, {}]...", defaultLatitude, defaultLongitude);
        try {
            getCurrentWeather(defaultLatitude, defaultLongitude);
        } catch (Exception e) {
            log.error("Scheduled weather observation fetch error: {}", e.getMessage());
        }
    }

    private void saveObservationToMongo(WeatherResponse wr) {
        try {
            WeatherObservation obs = new WeatherObservation(
                    wr.getLatitude(),
                    wr.getLongitude(),
                    wr.getTemperature(),
                    wr.getHumidity(),
                    wr.getPrecipitation(),
                    wr.getWindSpeed(),
                    wr.getCondition(),
                    wr.getTimestamp()
            );
            weatherRepository.save(obs);
        } catch (Exception e) {
            log.warn("Failed to record weather observation in MongoDB: {}", e.getMessage());
        }
    }

    private String deriveCondition(Double precipitation, Double temp) {
        if (precipitation != null && precipitation > 5.0) return "Heavy Rain";
        if (precipitation != null && precipitation > 0.1) return "Rain";
        if (temp != null && temp > 35.0) return "Hot / Sunny";
        if (temp != null && temp < 10.0) return "Chilly";
        return "Clear / Mild";
    }
}
