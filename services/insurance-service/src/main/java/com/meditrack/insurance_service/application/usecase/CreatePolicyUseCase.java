package com.meditrack.insurance_service.application.usecase;

import com.meditrack.insurance_service.application.dto.CreatePolicyRequest;
import com.meditrack.insurance_service.application.dto.PolicyResponse;

public interface CreatePolicyUseCase {
    PolicyResponse createPolicy(CreatePolicyRequest request);
}
