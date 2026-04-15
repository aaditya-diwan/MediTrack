package com.meditrack.insurance_service.application.usecase;

import com.meditrack.insurance_service.application.dto.PolicyResponse;

import java.util.UUID;

public interface GetPolicyUseCase {
    PolicyResponse getPolicy(UUID policyId);
}
