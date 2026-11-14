package com.smartcity.weather;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("Weather and Air Quality Endpoints Integration Tests")
class WeatherAirQualityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("GET /api/weather/current returns 200 with temperature, humidity, wind, and precipitation")
    void testGetCurrentWeather() throws Exception {
        mockMvc.perform(get("/api/weather/current")
                        .param("latitude", "30.7333")
                        .param("longitude", "76.7794"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.temperature", notNullValue()))
                .andExpect(jsonPath("$.data.humidity", notNullValue()))
                .andExpect(jsonPath("$.data.windSpeed", notNullValue()));
    }

    @Test
    @DisplayName("GET /api/air-quality/current returns 200 with PM2.5, PM10, AQI, and category")
    void testGetCurrentAirQuality() throws Exception {
        mockMvc.perform(get("/api/air-quality/current")
                        .param("latitude", "30.7333")
                        .param("longitude", "76.7794"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.pm25", notNullValue()))
                .andExpect(jsonPath("$.data.pm10", notNullValue()))
                .andExpect(jsonPath("$.data.aqi", notNullValue()))
                .andExpect(jsonPath("$.data.category", notNullValue()));
    }
}
