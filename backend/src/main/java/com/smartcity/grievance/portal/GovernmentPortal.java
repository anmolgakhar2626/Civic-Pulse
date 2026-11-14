package com.smartcity.grievance.portal;

import java.time.Instant;
import java.util.UUID;

public class GovernmentPortal {

    private UUID id;
    private String portalCode;
    private String portalName;
    private String endpointUrl;
    private boolean active = true;
    private boolean sandbox = true;
    private String authType = "API_KEY";
    private String configMetadata;
    private Instant createdAt = Instant.now();

    public GovernmentPortal() {}

    public GovernmentPortal(UUID id, String portalCode, String portalName, String endpointUrl,
                            boolean active, boolean sandbox, String authType) {
        this.id = id;
        this.portalCode = portalCode;
        this.portalName = portalName;
        this.endpointUrl = endpointUrl;
        this.active = active;
        this.sandbox = sandbox;
        this.authType = authType;
        this.createdAt = Instant.now();
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getPortalCode() { return portalCode; }
    public void setPortalCode(String portalCode) { this.portalCode = portalCode; }

    public String getPortalName() { return portalName; }
    public void setPortalName(String portalName) { this.portalName = portalName; }

    public String getEndpointUrl() { return endpointUrl; }
    public void setEndpointUrl(String endpointUrl) { this.endpointUrl = endpointUrl; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public boolean isSandbox() { return sandbox; }
    public void setSandbox(boolean sandbox) { this.sandbox = sandbox; }

    public String getAuthType() { return authType; }
    public void setAuthType(String authType) { this.authType = authType; }

    public String getConfigMetadata() { return configMetadata; }
    public void setConfigMetadata(String configMetadata) { this.configMetadata = configMetadata; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
