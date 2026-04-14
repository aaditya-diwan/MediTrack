package com.meditrack.insurance_service.interfaces.rest;

import com.meditrack.insurance_service.domain.model.InsurancePolicy;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Placeholder controller for insurance policy endpoints.
 * TODO: Wire to use cases once application layer is implemented.
 */
@RestController
@RequestMapping("/api/v1/insurance/policies")
public class InsurancePolicyController {

    @GetMapping("/{policyId}")
    public ResponseEntity<InsurancePolicy> getPolicy(@PathVariable UUID policyId) {
        // TODO: inject and call GetPolicyUseCase
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @GetMapping("/patient/{patientId}")
    public ResponseEntity<?> getPoliciesForPatient(@PathVariable UUID patientId) {
        // TODO: inject and call GetPatientPoliciesUseCase
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @PostMapping
    public ResponseEntity<InsurancePolicy> createPolicy(@RequestBody InsurancePolicy policy) {
        // TODO: inject and call CreatePolicyUseCase
        throw new UnsupportedOperationException("Not yet implemented");
    }
}
