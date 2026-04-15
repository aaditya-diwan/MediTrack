package com.meditrack.insurance_service.infrastructure.persistence.repository;

import com.meditrack.insurance_service.infrastructure.persistence.entity.InsurancePolicyEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface JpaInsurancePolicyRepository extends JpaRepository<InsurancePolicyEntity, UUID> {

    List<InsurancePolicyEntity> findByPatientId(UUID patientId);

    Optional<InsurancePolicyEntity> findByPolicyNumber(String policyNumber);

    boolean existsByPolicyNumber(String policyNumber);
}
