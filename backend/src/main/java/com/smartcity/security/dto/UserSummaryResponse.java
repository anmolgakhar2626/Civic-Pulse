package com.smartcity.security.dto;

import com.smartcity.user.User;

import java.time.Instant;
import java.util.UUID;

public class UserSummaryResponse {

    private UUID id;
    private String username;
    private String email;
    private String fullName;
    private String phone;
    private String mobileNumber;
    private boolean isMobileVerified;
    private int genuineComplaintCount;
    private boolean isVerifiedCitizen;
    private String role;
    private java.util.Set<String> roles = new java.util.HashSet<>();
    private UUID departmentId;
    private String departmentName;
    private UUID teamId;
    private String teamName;
    private boolean active;
    private boolean enabled;
    private Instant createdAt;

    public UserSummaryResponse() {}

    public static UserSummaryResponse fromEntity(User user) {
        if (user == null) return null;
        UserSummaryResponse response = new UserSummaryResponse();
        response.setId(user.getId());
        response.setUsername(user.getUsername());
        response.setEmail(user.getEmail());
        response.setFullName(user.getFullName());
        response.setPhone(user.getPhone());
        response.setMobileNumber(user.getMobileNumber());
        response.setMobileVerified(Boolean.TRUE.equals(user.getIsMobileVerified()));
        response.setGenuineComplaintCount(user.getGenuineComplaintCount() != null ? user.getGenuineComplaintCount() : 0);
        response.setVerifiedCitizen(Boolean.TRUE.equals(user.getIsVerifiedCitizen()));

        String rName = user.getRoleName() != null ? user.getRoleName() : (user.getRole() != null ? user.getRole().getName() : "CITIZEN");
        response.setRole(rName);

        if (user.getRoles() != null && !user.getRoles().isEmpty()) {
            response.setRoles(user.getRoles().stream()
                    .map(r -> r.getName())
                    .collect(java.util.stream.Collectors.toSet()));
        } else if (rName != null) {
            response.setRoles(java.util.Set.of(rName));
        }

        if (user.getDepartment() != null) {
            response.setDepartmentId(user.getDepartment().getId());
            response.setDepartmentName(user.getDepartment().getName());
        }
        if (user.getTeam() != null) {
            response.setTeamId(user.getTeam().getId());
            response.setTeamName(user.getTeam().getName());
        }
        boolean isEn = Boolean.TRUE.equals(user.getEnabled()) && Boolean.TRUE.equals(user.getActive());
        response.setActive(isEn);
        response.setEnabled(isEn);
        response.setCreatedAt(user.getCreatedAt());
        return response;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getMobileNumber() { return mobileNumber != null ? mobileNumber : phone; }
    public void setMobileNumber(String mobileNumber) { this.mobileNumber = mobileNumber; }

    public boolean isMobileVerified() { return isMobileVerified; }
    public void setMobileVerified(boolean mobileVerified) { isMobileVerified = mobileVerified; }

    public int getGenuineComplaintCount() { return genuineComplaintCount; }
    public void setGenuineComplaintCount(int genuineComplaintCount) { this.genuineComplaintCount = genuineComplaintCount; }

    public boolean isVerifiedCitizen() { return isVerifiedCitizen; }
    public void setVerifiedCitizen(boolean verifiedCitizen) { isVerifiedCitizen = verifiedCitizen; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public java.util.Set<String> getRoles() { return roles; }
    public void setRoles(java.util.Set<String> roles) { this.roles = roles != null ? roles : new java.util.HashSet<>(); }

    public UUID getDepartmentId() { return departmentId; }
    public void setDepartmentId(UUID departmentId) { this.departmentId = departmentId; }

    public String getDepartmentName() { return departmentName; }
    public void setDepartmentName(String departmentName) { this.departmentName = departmentName; }

    public UUID getTeamId() { return teamId; }
    public void setTeamId(UUID teamId) { this.teamId = teamId; }

    public String getTeamName() { return teamName; }
    public void setTeamName(String teamName) { this.teamName = teamName; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
