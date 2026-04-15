package com.meditrack.insurance_service.domain.repository;

import com.meditrack.insurance_service.domain.model.InsurancePolicy;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface InsurancePolicyRepository {

    InsurancePolicy save(InsurancePolicy policy);

    Optional<InsurancePolicy> findById(UUID policyId);

    List<InsurancePolicy> findByPatientId(UUID patientId);

    Optional<InsurancePolicy> findByPolicyNumber(String policyNumber);

    boolean existsByPolicyNumber(String policyNumber);
}
