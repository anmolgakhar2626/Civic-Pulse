package com.smartcity.map.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;

@Schema(description = "Shelter and emergency facility representation for GIS Map display")
public class MapShelterResponse {

    @Schema(description = "Shelter UUID")
    private UUID id;

    @Schema(description = "Shelter or facility name", example = "Disaster Emergency Shelter Sector 22")
    private String name;

    @Schema(description = "Facility type", example = "EMERGENCY_SHELTER")
    private String resourceType;

    @Schema(description = "Latitude coordinate", example = "30.7350")
    private Double latitude;

    @Schema(description = "Longitude coordinate", example = "76.7720")
    private Double longitude;

    @Schema(description = "Physical address", example = "Sector 22-B Community Centre, Chandigarh")
    private String address;

    @Schema(description = "Contact phone number", example = "+91-172-2701234")
    private String contactNumber;

    @Schema(description = "Whether the facility is currently available for occupancy")
    private Boolean available;

    @Schema(description = "Refuge capacity count", example = "350")
    private Integer capacity;

    @Schema(description = "Operational hours", example = "24/7")
    private String operationalHours;

    @Schema(description = "Operational status", example = "OPERATIONAL")
    private String status;

    public MapShelterResponse() {}

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getResourceType() { return resourceType; }
    public void setResourceType(String resourceType) { this.resourceType = resourceType; }

    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }

    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getContactNumber() { return contactNumber; }
    public void setContactNumber(String contactNumber) { this.contactNumber = contactNumber; }

    public Boolean getAvailable() { return available; }
    public void setAvailable(Boolean available) { this.available = available; }

    public Integer getCapacity() { return capacity; }
    public void setCapacity(Integer capacity) { this.capacity = capacity; }

    public String getOperationalHours() { return operationalHours; }
    public void setOperationalHours(String operationalHours) { this.operationalHours = operationalHours; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}

