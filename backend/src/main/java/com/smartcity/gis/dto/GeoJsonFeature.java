package com.smartcity.gis.dto;

import java.util.HashMap;
import java.util.Map;

public class GeoJsonFeature {
    private String type = "Feature";
    private String id;
    private GeoJsonGeometry geometry;
    private Map<String, Object> properties = new HashMap<>();

    public GeoJsonFeature() {}

    public GeoJsonFeature(String id, Double longitude, Double latitude, Map<String, Object> properties) {
        this.type = "Feature";
        this.id = id;
        this.geometry = new GeoJsonGeometry(longitude, latitude);
        this.properties = properties != null ? properties : new HashMap<>();
    }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public GeoJsonGeometry getGeometry() { return geometry; }
    public void setGeometry(GeoJsonGeometry geometry) { this.geometry = geometry; }

    public Map<String, Object> getProperties() { return properties; }
    public void setProperties(Map<String, Object> properties) { this.properties = properties; }
}
