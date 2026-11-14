package com.smartcity.analytics.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Geographic location incident concentration metric")
public class LocationAnalyticsDTO {

    @Schema(description = "Location name or ward label", example = "Sector 17 Commercial Promenade")
    private String locationName;

    @Schema(description = "Total incidents recorded at this location", example = "28")
    private long count;

    @Schema(description = "Critical severity incidents at this location", example = "4")
    private long criticalCount;

    @Schema(description = "Latitude coordinate", example = "30.7425")
    private Double latitude;

    @Schema(description = "Longitude coordinate", example = "76.7820")
    private Double longitude;

    public LocationAnalyticsDTO() {}

    public LocationAnalyticsDTO(String locationName, long count, long criticalCount, Double latitude, Double longitude) {
        this.locationName = locationName;
        this.count = count;
        this.criticalCount = criticalCount;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public String getLocationName() { return locationName; }
    public void setLocationName(String locationName) { this.locationName = locationName; }

    public long getCount() { return count; }
    public void setCount(long count) { this.count = count; }

    public long getCriticalCount() { return criticalCount; }
    public void setCriticalCount(long criticalCount) { this.criticalCount = criticalCount; }

    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }

    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }
}

