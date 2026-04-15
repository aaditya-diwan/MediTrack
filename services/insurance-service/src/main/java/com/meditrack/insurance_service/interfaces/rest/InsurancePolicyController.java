package com.meditrack.insurance_service.interfaces.rest;

import com.meditrack.insurance_service.application.dto.CreatePolicyRequest;
import com.meditrack.insurance_service.application.dto.PolicyResponse;
import com.meditrack.insurance_service.application.usecase.CreatePolicyUseCase;
import com.meditrack.insurance_service.application.usecase.GetPatientPoliciesUseCase;
import com.meditrack.insurance_service.application.usecase.GetPolicyUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST controller for insurance policy management.
 *
 * Endpoints:
 *   POST   /api/v1/insurance/policies             - Create a new policy
 *   GET    /api/v1/insurance/policies/{policyId}  - Get policy by ID
 *   GET    /api/v1/insurance/policies/patient/{patientId} - Get all policies for a patient
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/insurance/policies")
@RequiredArgsConstructor
public class InsurancePolicyController {

    private final CreatePolicyUseCase createPolicyUseCase;
    private final GetPolicyUseCase getPolicyUseCase;
    private final GetPatientPoliciesUseCase getPatientPoliciesUseCase;

    @PostMapping
    public ResponseEntity<PolicyResponse> createPolicy(@Valid @RequestBody CreatePolicyRequest request) {
        log.info("Received create-policy request [patientId={}, policyNumber={}]",
                request.getPatientId(), request.getPolicyNumber());
        PolicyResponse response = createPolicyUseCase.createPolicy(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{policyId}")
    public ResponseEntity<PolicyResponse> getPolicy(@PathVariable UUID policyId) {
        log.info("Received get-policy request [policyId={}]", policyId);
        return ResponseEntity.ok(getPolicyUseCase.getPolicy(policyId));
    }

    @GetMapping("/patient/{patientId}")
    public ResponseEntity<List<PolicyResponse>> getPoliciesForPatient(@PathVariable UUID patientId) {
        log.info("Received get-patient-policies request [patientId={}]", patientId);
        return ResponseEntity.ok(getPatientPoliciesUseCase.getPoliciesForPatient(patientId));
    }
}
