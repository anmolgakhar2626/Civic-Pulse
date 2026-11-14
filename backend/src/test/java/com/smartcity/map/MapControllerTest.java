package com.smartcity.map;

import com.smartcity.common.GlobalExceptionHandler;
import com.smartcity.map.dto.MapIncidentMarkerResponse;
import com.smartcity.map.dto.MapInfrastructureResponse;
import com.smartcity.map.dto.MapShelterResponse;
import com.smartcity.map.dto.MapSummaryResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("MapController Unit & Validation Tests")
class MapControllerTest {

    private MockMvc mockMvc;

    @Mock
    private MapService mapService;

    @BeforeEach
    void setUp() {
        MapController controller = new MapController(mapService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("GET /api/map/incidents returns 200 with list of incident markers")
    void testGetMapIncidents() throws Exception {
        MapIncidentMarkerResponse marker = new MapIncidentMarkerResponse();
        marker.setId(UUID.randomUUID());
        marker.setIncidentNumber("INC-20261028-01");
        marker.setTitle("Water Main Burst");
        marker.setCategory("WATER");
        marker.setSeverity("HIGH");
        marker.setStatus("OPEN");
        marker.setLatitude(30.7333);
        marker.setLongitude(76.7794);

        when(mapService.getIncidentMarkers(any(), any(), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(List.of(marker));

        mockMvc.perform(get("/api/map/incidents")
                        .param("minLat", "30.70")
                        .param("maxLat", "30.80")
                        .param("minLng", "76.70")
                        .param("maxLng", "76.85")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].incidentNumber", is("INC-20261028-01")))
                .andExpect(jsonPath("$[0].latitude", is(30.7333)))
                .andExpect(jsonPath("$[0].longitude", is(76.7794)));
    }

    @Test
    @DisplayName("GET /api/map/incidents returns 400 when bounding box is incomplete")
    void testGetMapIncidentsIncompleteBbox() throws Exception {
        when(mapService.getIncidentMarkers(eq(30.70), any(), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenThrow(new IllegalArgumentException("Bounding box parameters must either all be provided or all omitted."));

        mockMvc.perform(get("/api/map/incidents")
                        .param("minLat", "30.70")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("Bounding box parameters")));
    }

    @Test
    @DisplayName("GET /api/map/infrastructure returns 200 with infrastructure list")
    void testGetMapInfrastructure() throws Exception {
        MapInfrastructureResponse inf = new MapInfrastructureResponse();
        inf.setId(UUID.randomUUID());
        inf.setCode("INF-HOSP-01");
        inf.setName("Sector 22 Hospital");
        inf.setCategory("HOSPITAL");
        inf.setStatus("OPERATIONAL");
        inf.setLatitude(30.732);
        inf.setLongitude(76.771);

        when(mapService.getInfrastructureMarkers(any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(List.of(inf));

        mockMvc.perform(get("/api/map/infrastructure")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].code", is("INF-HOSP-01")))
                .andExpect(jsonPath("$[0].status", is("OPERATIONAL")));
    }

    @Test
    @DisplayName("GET /api/map/shelters returns 200 with shelters")
    void testGetMapShelters() throws Exception {
        MapShelterResponse shelter = new MapShelterResponse();
        shelter.setId(UUID.randomUUID());
        shelter.setName("Sector 22 Emergency Shelter");
        shelter.setCapacity(350);
        shelter.setAvailable(true);

        when(mapService.getShelterMarkers(any(), any(), any(), any(), any(), any()))
                .thenReturn(List.of(shelter));

        mockMvc.perform(get("/api/map/shelters")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].name", is("Sector 22 Emergency Shelter")))
                .andExpect(jsonPath("$[0].capacity", is(350)));
    }

    @Test
    @DisplayName("GET /api/map/summary returns 200 with aggregated metrics")
    void testGetMapSummary() throws Exception {
        MapSummaryResponse summary = new MapSummaryResponse();
        summary.setTotalIncidents(42);
        summary.setActiveIncidents(15);
        summary.setCriticalIncidents(3);
        summary.setOperationalInfrastructure(10);
        summary.setAvailableShelters(4);
        summary.setLastUpdated(Instant.now());

        when(mapService.getMapSummary()).thenReturn(summary);

        mockMvc.perform(get("/api/map/summary")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalIncidents", is(42)))
                .andExpect(jsonPath("$.activeIncidents", is(15)))
                .andExpect(jsonPath("$.criticalIncidents", is(3)))
                .andExpect(jsonPath("$.operationalInfrastructure", is(10)))
                .andExpect(jsonPath("$.availableShelters", is(4)));
    }
}

