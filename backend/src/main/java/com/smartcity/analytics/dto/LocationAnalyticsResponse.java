package com.smartcity.analytics.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "Geographic incident concentration and hotspots")
public class LocationAnalyticsResponse {

    @Schema(description = "List of top incident locations/wards")
    private List<LocationAnalyticsDTO> locations;

    @Schema(description = "Total distinct locations recorded")
    private int totalLocations;

    public LocationAnalyticsResponse() {}

    public LocationAnalyticsResponse(List<LocationAnalyticsDTO> locations, int totalLocations) {
        this.locations = locations;
        this.totalLocations = totalLocations;
    }

    public List<LocationAnalyticsDTO> getLocations() { return locations; }
    public void setLocations(List<LocationAnalyticsDTO> locations) { this.locations = locations; }

    public int getTotalLocations() { return totalLocations; }
    public void setTotalLocations(int totalLocations) { this.totalLocations = totalLocations; }
}

