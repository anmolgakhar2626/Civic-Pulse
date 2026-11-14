package com.smartcity.gis.dto;

import java.util.Arrays;
import java.util.List;

public class GeoJsonGeometry {
    private String type = "Point";
    private List<Double> coordinates; // [longitude, latitude]

    public GeoJsonGeometry() {}

    public GeoJsonGeometry(Double longitude, Double latitude) {
        this.type = "Point";
        this.coordinates = Arrays.asList(longitude, latitude);
    }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public List<Double> getCoordinates() { return coordinates; }
    public void setCoordinates(List<Double> coordinates) { this.coordinates = coordinates; }
}
