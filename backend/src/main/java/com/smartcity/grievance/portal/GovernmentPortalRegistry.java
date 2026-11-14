package com.smartcity.grievance.portal;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class GovernmentPortalRegistry {

    private final Map<String, GovernmentPortalAdapter> adapters = new ConcurrentHashMap<>();

    public GovernmentPortalRegistry(List<GovernmentPortalAdapter> adapterList) {
        if (adapterList != null) {
            for (GovernmentPortalAdapter adapter : adapterList) {
                adapters.put(adapter.getPortalCode().toUpperCase(), adapter);
            }
        }
    }

    public Optional<GovernmentPortalAdapter> getAdapter(String portalCode) {
        if (portalCode == null) return Optional.empty();
        return Optional.ofNullable(adapters.get(portalCode.trim().toUpperCase()));
    }

    public List<GovernmentPortalAdapter> getAllAdapters() {
        return List.copyOf(adapters.values());
    }
}
