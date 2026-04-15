package com.meditrack.insurance_service.application.usecase;

import com.meditrack.insurance_service.application.dto.PolicyResponse;

import java.util.List;
import java.util.UUID;

public interface GetPatientPoliciesUseCase {
    List<PolicyResponse> getPoliciesForPatient(UUID patientId);
}
