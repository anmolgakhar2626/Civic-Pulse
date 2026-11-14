package com.smartcity.priority.dto;

import com.smartcity.priority.PublicExposure;
import com.smartcity.priority.Severity;

import java.util.HashMap;
import java.util.Map;

public class EvaluatePriorityRequest {

    private String incidentType;
    private Severity severity;
    private PublicExposure publicExposure;
    private String infrastructureImportance;
    private String safetyImpact;
    private Boolean majorJunction;
    private Boolean blockingEmergencyAccess;
    private Boolean exposedLiveWire;
    private Boolean transformerSpark;
    private Boolean busyRoad;
    private Boolean submergedUnderpass;
    private Boolean roadCaveIn;
    private Double pm25;
    private Double wasteBinFillPercentage;
    private String description;
    private String address;
    private Map<String, Object> customAttributes = new HashMap<>();

    public EvaluatePriorityRequest() {}

    public String getIncidentType() { return incidentType; }
    public void setIncidentType(String incidentType) { this.incidentType = incidentType; }

    public Severity getSeverity() { return severity; }
    public void setSeverity(Severity severity) { this.severity = severity; }

    public PublicExposure getPublicExposure() { return publicExposure; }
    public void setPublicExposure(PublicExposure publicExposure) { this.publicExposure = publicExposure; }

    public String getInfrastructureImportance() { return infrastructureImportance; }
    public void setInfrastructureImportance(String infrastructureImportance) { this.infrastructureImportance = infrastructureImportance; }

    public String getSafetyImpact() { return safetyImpact; }
    public void setSafetyImpact(String safetyImpact) { this.safetyImpact = safetyImpact; }

    public Boolean getMajorJunction() { return majorJunction; }
    public void setMajorJunction(Boolean majorJunction) { this.majorJunction = majorJunction; }
    public boolean isMajorJunction() { return Boolean.TRUE.equals(majorJunction); }

    public Boolean getBlockingEmergencyAccess() { return blockingEmergencyAccess; }
    public void setBlockingEmergencyAccess(Boolean blockingEmergencyAccess) { this.blockingEmergencyAccess = blockingEmergencyAccess; }
    public boolean isBlockingEmergencyAccess() { return Boolean.TRUE.equals(blockingEmergencyAccess); }

    public Boolean getExposedLiveWire() { return exposedLiveWire; }
    public void setExposedLiveWire(Boolean exposedLiveWire) { this.exposedLiveWire = exposedLiveWire; }
    public boolean isExposedLiveWire() { return Boolean.TRUE.equals(exposedLiveWire); }

    public Boolean getTransformerSpark() { return transformerSpark; }
    public void setTransformerSpark(Boolean transformerSpark) { this.transformerSpark = transformerSpark; }
    public boolean isTransformerSpark() { return Boolean.TRUE.equals(transformerSpark); }

    public Boolean getBusyRoad() { return busyRoad; }
    public void setBusyRoad(Boolean busyRoad) { this.busyRoad = busyRoad; }
    public boolean isBusyRoad() { return Boolean.TRUE.equals(busyRoad); }

    public Boolean getSubmergedUnderpass() { return submergedUnderpass; }
    public void setSubmergedUnderpass(Boolean submergedUnderpass) { this.submergedUnderpass = submergedUnderpass; }
    public boolean isSubmergedUnderpass() { return Boolean.TRUE.equals(submergedUnderpass); }

    public Boolean getRoadCaveIn() { return roadCaveIn; }
    public void setRoadCaveIn(Boolean roadCaveIn) { this.roadCaveIn = roadCaveIn; }
    public boolean isRoadCaveIn() { return Boolean.TRUE.equals(roadCaveIn); }

    public Double getPm25() { return pm25; }
    public void setPm25(Double pm25) { this.pm25 = pm25; }

    public Double getWasteBinFillPercentage() { return wasteBinFillPercentage; }
    public void setWasteBinFillPercentage(Double wasteBinFillPercentage) { this.wasteBinFillPercentage = wasteBinFillPercentage; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public Map<String, Object> getCustomAttributes() { return customAttributes; }
    public void setCustomAttributes(Map<String, Object> customAttributes) { this.customAttributes = customAttributes; }
}
