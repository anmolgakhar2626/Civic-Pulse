package com.smartcity.gis;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@DisplayName("GIS Spatial Integration and GeoJSON Endpoints Tests")
class GisIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("GET /api/gis/incidents returns 200 and valid GeoJSON FeatureCollection")
    void testGetIncidentsGeoJson() throws Exception {
        mockMvc.perform(get("/api/gis/incidents"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.type", is("FeatureCollection")))
                .andExpect(jsonPath("$.features", notNullValue()));
    }

    @Test
    @DisplayName("GET /api/gis/sensors returns 200 and valid GeoJSON FeatureCollection")
    void testGetSensorsGeoJson() throws Exception {
        mockMvc.perform(get("/api/gis/sensors"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.type", is("FeatureCollection")))
                .andExpect(jsonPath("$.features", notNullValue()))
                .andExpect(jsonPath("$.features", hasSize(greaterThanOrEqualTo(1))));
    }

    @Test
    @DisplayName("GET /api/gis/infrastructure returns 200 and GeoJSON containing infrastructure, signals, and bins")
    void testGetInfrastructureGeoJson() throws Exception {
        mockMvc.perform(get("/api/gis/infrastructure"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.type", is("FeatureCollection")))
                .andExpect(jsonPath("$.features", notNullValue()))
                .andExpect(jsonPath("$.features", hasSize(greaterThanOrEqualTo(1))));
    }

    @Test
    @DisplayName("GET /api/gis/nearby-incidents returns 200 with PostGIS spatial distance filtering")
    void testGetNearbyIncidents() throws Exception {
        mockMvc.perform(get("/api/gis/nearby-incidents")
                        .param("latitude", "30.7333")
                        .param("longitude", "76.7794")
                        .param("radius", "50000"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.type", is("FeatureCollection")))
                .andExpect(jsonPath("$.features", notNullValue()));
    }

    @Test
    @DisplayName("GET /api/gis/nearby-resources returns 200 with PostGIS spatial emergency resources")
    void testGetNearbyResources() throws Exception {
        mockMvc.perform(get("/api/gis/nearby-resources")
                        .param("latitude", "30.7410")
                        .param("longitude", "76.7850")
                        .param("radius", "10000"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.type", is("FeatureCollection")))
                .andExpect(jsonPath("$.features", notNullValue()))
                .andExpect(jsonPath("$.features", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$.features[0].geometry.type", is("Point")))
                .andExpect(jsonPath("$.features[0].properties.name", notNullValue()))
                .andExpect(jsonPath("$.features[0].properties.resourceType", notNullValue()));
    }
}
