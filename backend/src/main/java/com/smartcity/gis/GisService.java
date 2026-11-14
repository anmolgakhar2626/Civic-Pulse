package com.smartcity.gis;

import com.smartcity.gis.dto.GeoJsonFeatureCollection;

public interface GisService {

    GeoJsonFeatureCollection getIncidentsAsGeoJson();

    GeoJsonFeatureCollection getSensorsAsGeoJson();

    GeoJsonFeatureCollection getInfrastructureAsGeoJson();

    GeoJsonFeatureCollection getNearbyIncidents(Double latitude, Double longitude, Double radiusMeters);

    GeoJsonFeatureCollection getNearbyResources(Double latitude, Double longitude, Double radiusMeters);
}
