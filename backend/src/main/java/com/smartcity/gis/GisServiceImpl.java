package com.smartcity.gis;

import com.smartcity.gis.dto.GeoJsonFeature;
import com.smartcity.gis.dto.GeoJsonFeatureCollection;
import com.smartcity.incident.Incident;
import com.smartcity.incident.IncidentRepository;
import com.smartcity.infrastructure.Infrastructure;
import com.smartcity.infrastructure.InfrastructureRepository;
import com.smartcity.iot.SensorDevice;
import com.smartcity.iot.SensorDeviceRepository;
import com.smartcity.traffic.TrafficSignal;
import com.smartcity.traffic.TrafficSignalRepository;
import com.smartcity.waste.WasteBin;
import com.smartcity.waste.WasteBinRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@Transactional(readOnly = true)
public class GisServiceImpl implements GisService {

    private static final Logger log = LoggerFactory.getLogger(GisServiceImpl.class);

    private final IncidentRepository incidentRepository;
    private final SensorGisRepository sensorGisRepository;
    private final InfrastructureRepository infrastructureRepository;
    private final EmergencyResourceRepository emergencyResourceRepository;
    private final TrafficSignalRepository trafficSignalRepository;
    private final WasteBinRepository wasteBinRepository;
    private final LocationRepository locationRepository;
    private final SensorDeviceRepository mongoSensorRepository;

    public GisServiceImpl(
            IncidentRepository incidentRepository,
            SensorGisRepository sensorGisRepository,
            InfrastructureRepository infrastructureRepository,
            EmergencyResourceRepository emergencyResourceRepository,
            TrafficSignalRepository trafficSignalRepository,
            WasteBinRepository wasteBinRepository,
            LocationRepository locationRepository,
            @Autowired(required = false) SensorDeviceRepository mongoSensorRepository
    ) {
        this.incidentRepository = incidentRepository;
        this.sensorGisRepository = sensorGisRepository;
        this.infrastructureRepository = infrastructureRepository;
        this.emergencyResourceRepository = emergencyResourceRepository;
        this.trafficSignalRepository = trafficSignalRepository;
        this.wasteBinRepository = wasteBinRepository;
        this.locationRepository = locationRepository;
        this.mongoSensorRepository = mongoSensorRepository;
    }

    @Override
    public GeoJsonFeatureCollection getIncidentsAsGeoJson() {
        List<Incident> incidents = incidentRepository.findAll();
        List<GeoJsonFeature> features = new ArrayList<>();

        for (Incident inc : incidents) {
            if (inc.getLatitude() != null && inc.getLongitude() != null) {
                Map<String, Object> props = new LinkedHashMap<>();
                props.put("id", inc.getId() != null ? inc.getId().toString() : null);
                props.put("incidentNumber", inc.getIncidentNumber());
                props.put("incidentType", inc.getIncidentType());
                props.put("title", inc.getIncidentType());
                props.put("description", inc.getDescription());
                props.put("status", inc.getStatus() != null ? inc.getStatus().name() : null);
                props.put("priorityTier", inc.getTier() != null ? inc.getTier().name() : "MEDIUM");
                props.put("priorityLabel", inc.getPriorityLabel());
                props.put("source", inc.getSource() != null ? inc.getSource().name() : null);
                props.put("address", inc.getAddress());
                props.put("reportedTime", inc.getReportedAt() != null ? inc.getReportedAt().toString() : null);
                if (inc.getAssignedDepartment() != null) {
                    props.put("assignedDepartment", inc.getAssignedDepartment().getName());
                    props.put("departmentCode", inc.getAssignedDepartment().getCode());
                }

                features.add(new GeoJsonFeature(
                        inc.getId() != null ? inc.getId().toString() : inc.getIncidentNumber(),
                        inc.getLongitude(),
                        inc.getLatitude(),
                        props
                ));
            }
        }

        return new GeoJsonFeatureCollection(features);
    }

    @Override
    public GeoJsonFeatureCollection getSensorsAsGeoJson() {
        // Read sensors from sensors_gis
        List<SensorGis> sensors = sensorGisRepository.findAll();
        Set<String> seenIds = new HashSet<>();
        List<GeoJsonFeature> features = new ArrayList<>();

        for (SensorGis s : sensors) {
            seenIds.add(s.getSensorId());
            Map<String, Object> props = new LinkedHashMap<>();
            props.put("sensorId", s.getSensorId());
            props.put("sensorType", s.getSensorType());
            props.put("locationName", s.getLocationName());
            props.put("status", s.getStatus());
            props.put("lastValue", s.getLastValue());
            props.put("unit", s.getUnit());
            props.put("lastSeenAt", s.getLastSeenAt() != null ? s.getLastSeenAt().toString() : null);

            features.add(new GeoJsonFeature(
                    s.getSensorId(),
                    s.getLongitude(),
                    s.getLatitude(),
                    props
            ));
        }

        // Also merge any MongoDB sensors not yet in PostgreSQL
        if (mongoSensorRepository != null) {
            try {
                List<SensorDevice> mongoSensors = mongoSensorRepository.findAll();
                for (SensorDevice ms : mongoSensors) {
                    if (ms.getLatitude() != null && ms.getLongitude() != null && !seenIds.contains(ms.getSensorId())) {
                        Map<String, Object> props = new LinkedHashMap<>();
                        props.put("sensorId", ms.getSensorId());
                        props.put("sensorType", ms.getSensorType() != null ? ms.getSensorType().name() : "IOT");
                        props.put("locationName", ms.getLocation());
                        props.put("status", ms.getStatus() != null ? ms.getStatus().name() : "ONLINE");
                        props.put("lastValue", ms.getLastValue());
                        props.put("unit", ms.getUnit());
                        props.put("lastSeenAt", ms.getLastSeenAt() != null ? ms.getLastSeenAt().toString() : null);

                        features.add(new GeoJsonFeature(
                                ms.getSensorId(),
                                ms.getLongitude(),
                                ms.getLatitude(),
                                props
                        ));
                    }
                }
            } catch (Exception e) {
                log.warn("Could not query MongoDB sensors for GIS: {}", e.getMessage());
            }
        }

        return new GeoJsonFeatureCollection(features);
    }

    @Override
    public GeoJsonFeatureCollection getInfrastructureAsGeoJson() {
        List<GeoJsonFeature> features = new ArrayList<>();

        // 1. Civil Infrastructure
        List<Infrastructure> infraList = infrastructureRepository.findAll();
        for (Infrastructure inf : infraList) {
            if (inf.getLatitude() != null && inf.getLongitude() != null) {
                Map<String, Object> props = new LinkedHashMap<>();
                props.put("category", "INFRASTRUCTURE");
                props.put("code", inf.getCode());
                props.put("name", inf.getName());
                props.put("infrastructureType", inf.getInfrastructureType());
                props.put("status", inf.getStatus());
                props.put("importanceLevel", inf.getImportanceLevel());
                props.put("address", inf.getAddress());

                features.add(new GeoJsonFeature(inf.getCode(), inf.getLongitude(), inf.getLatitude(), props));
            }
        }

        // 2. Traffic Signals
        List<TrafficSignal> signals = trafficSignalRepository.findAll();
        for (TrafficSignal sig : signals) {
            if (sig.getLatitude() != null && sig.getLongitude() != null) {
                Map<String, Object> props = new LinkedHashMap<>();
                props.put("category", "TRAFFIC_SIGNAL");
                props.put("code", sig.getSignalCode());
                props.put("name", sig.getJunctionName());
                props.put("junctionType", sig.getJunctionType());
                props.put("status", sig.getOperationalStatus());
                props.put("cycleSeconds", sig.getCurrentCycleSeconds());

                features.add(new GeoJsonFeature(sig.getSignalCode(), sig.getLongitude(), sig.getLatitude(), props));
            }
        }

        // 3. Waste Bins
        List<WasteBin> bins = wasteBinRepository.findAll();
        for (WasteBin bin : bins) {
            if (bin.getLatitude() != null && bin.getLongitude() != null) {
                Map<String, Object> props = new LinkedHashMap<>();
                props.put("category", "WASTE_BIN");
                props.put("code", bin.getBinCode());
                props.put("name", bin.getLocationName());
                props.put("capacityLiters", bin.getCapacityLiters());
                props.put("currentFillPercentage", bin.getCurrentFillPercentage());
                props.put("status", bin.getSensorStatus());

                features.add(new GeoJsonFeature(bin.getBinCode(), bin.getLongitude(), bin.getLatitude(), props));
            }
        }

        // 4. Public Facilities / Locations
        List<Location> locations = locationRepository.findAll();
        for (Location loc : locations) {
            if (loc.getLatitude() != null && loc.getLongitude() != null) {
                Map<String, Object> props = new LinkedHashMap<>();
                props.put("category", "PUBLIC_FACILITY");
                props.put("code", loc.getCode());
                props.put("name", loc.getName());
                props.put("locationType", loc.getLocationType());

                features.add(new GeoJsonFeature(loc.getCode(), loc.getLongitude(), loc.getLatitude(), props));
            }
        }

        return new GeoJsonFeatureCollection(features);
    }

    @Override
    public GeoJsonFeatureCollection getNearbyIncidents(Double latitude, Double longitude, Double radiusMeters) {
        if (latitude == null || longitude == null) {
            throw new IllegalArgumentException("Latitude and longitude must not be null");
        }
        double radius = (radiusMeters != null && radiusMeters > 0) ? radiusMeters : 5000.0;

        List<Incident> nearby = incidentRepository.findNearbyIncidents(latitude, longitude, radius);
        List<GeoJsonFeature> features = new ArrayList<>();

        for (Incident inc : nearby) {
            Map<String, Object> props = new LinkedHashMap<>();
            props.put("id", inc.getId() != null ? inc.getId().toString() : null);
            props.put("incidentNumber", inc.getIncidentNumber());
            props.put("incidentType", inc.getIncidentType());
            props.put("title", inc.getIncidentType());
            props.put("status", inc.getStatus() != null ? inc.getStatus().name() : null);
            props.put("priorityTier", inc.getTier() != null ? inc.getTier().name() : "MEDIUM");
            props.put("address", inc.getAddress());

            features.add(new GeoJsonFeature(
                    inc.getId() != null ? inc.getId().toString() : inc.getIncidentNumber(),
                    inc.getLongitude(),
                    inc.getLatitude(),
                    props
            ));
        }

        return new GeoJsonFeatureCollection(features);
    }

    @Override
    public GeoJsonFeatureCollection getNearbyResources(Double latitude, Double longitude, Double radiusMeters) {
        if (latitude == null || longitude == null) {
            throw new IllegalArgumentException("Latitude and longitude must not be null");
        }
        double radius = (radiusMeters != null && radiusMeters > 0) ? radiusMeters : 5000.0;

        List<EmergencyResource> nearby = emergencyResourceRepository.findNearbyResources(latitude, longitude, radius);
        List<GeoJsonFeature> features = new ArrayList<>();

        for (EmergencyResource res : nearby) {
            Map<String, Object> props = new LinkedHashMap<>();
            props.put("id", res.getId() != null ? res.getId().toString() : null);
            props.put("name", res.getName());
            props.put("resourceType", res.getResourceType());
            props.put("address", res.getAddress());
            props.put("contactNumber", res.getContactNumber());
            props.put("available", res.getAvailable());
            props.put("capacity", res.getCapacity());
            props.put("operationalHours", res.getOperationalHours());

            features.add(new GeoJsonFeature(
                    res.getId() != null ? res.getId().toString() : res.getName(),
                    res.getLongitude(),
                    res.getLatitude(),
                    props
            ));
        }

        return new GeoJsonFeatureCollection(features);
    }
}
