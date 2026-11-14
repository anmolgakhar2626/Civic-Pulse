package com.smartcity.infrastructure;

import com.smartcity.common.PagedResponse;
import com.smartcity.infrastructure.dto.CreateInfrastructureRequest;
import com.smartcity.infrastructure.dto.InfrastructureResponse;
import com.smartcity.infrastructure.dto.UpdateInfrastructureRequest;

import java.util.UUID;

public interface InfrastructureService {

    InfrastructureResponse createInfrastructure(CreateInfrastructureRequest request);

    InfrastructureResponse updateInfrastructure(UUID id, UpdateInfrastructureRequest request);

    InfrastructureResponse getInfrastructureById(UUID id);

    PagedResponse<InfrastructureResponse> getInfrastructureList(
            String category,
            String status,
            String search,
            Double minLat,
            Double maxLat,
            Double minLng,
            Double maxLng,
            int page,
            int size
    );

    void deleteInfrastructure(UUID id);
}

