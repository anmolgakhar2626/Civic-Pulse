package com.smartcity.complaint.dto;

import java.util.UUID;

public class CitizenReputationResponse {

    private UUID citizenId;
    private String username;
    private String fullName;
    private int genuineComplaintCount;
    private boolean isVerifiedCitizen;
    private int thresholdRequired = 5;
    private int complaintsNeededForBadge;

    public CitizenReputationResponse() {}

    public CitizenReputationResponse(UUID citizenId, String username, String fullName,
                                     int genuineComplaintCount, boolean isVerifiedCitizen) {
        this.citizenId = citizenId;
        this.username = username;
        this.fullName = fullName;
        this.genuineComplaintCount = genuineComplaintCount;
        this.isVerifiedCitizen = isVerifiedCitizen;
        this.thresholdRequired = 5;
        this.complaintsNeededForBadge = Math.max(0, 5 - genuineComplaintCount);
    }

    public UUID getCitizenId() { return citizenId; }
    public void setCitizenId(UUID citizenId) { this.citizenId = citizenId; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public int getGenuineComplaintCount() { return genuineComplaintCount; }
    public void setGenuineComplaintCount(int genuineComplaintCount) { this.genuineComplaintCount = genuineComplaintCount; }

    public boolean isVerifiedCitizen() { return isVerifiedCitizen; }
    public void setVerifiedCitizen(boolean verifiedCitizen) { isVerifiedCitizen = verifiedCitizen; }

    public int getThresholdRequired() { return thresholdRequired; }
    public void setThresholdRequired(int thresholdRequired) { this.thresholdRequired = thresholdRequired; }

    public int getComplaintsNeededForBadge() { return complaintsNeededForBadge; }
    public void setComplaintsNeededForBadge(int complaintsNeededForBadge) { this.complaintsNeededForBadge = complaintsNeededForBadge; }
}
