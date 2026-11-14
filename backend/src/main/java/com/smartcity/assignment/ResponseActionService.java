package com.smartcity.assignment;

import com.smartcity.assignment.dto.CreateResponseActionRequest;
import com.smartcity.assignment.dto.ResponseActionResponse;
import com.smartcity.assignment.dto.UpdateResponseActionStatusRequest;

import java.util.List;
import java.util.UUID;

public interface ResponseActionService {

    List<ResponseActionResponse> getActionsForIncident(String incidentIdentifier);

    ResponseActionResponse createAction(String incidentIdentifier, CreateResponseActionRequest request);

    ResponseActionResponse updateActionStatus(UUID actionId, UpdateResponseActionStatusRequest request);

    List<ResponseActionResponse> generateResponsePlan(String incidentIdentifier);
}
