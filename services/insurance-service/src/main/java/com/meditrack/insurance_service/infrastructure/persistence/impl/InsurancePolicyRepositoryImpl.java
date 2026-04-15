package com.meditrack.insurance_service.infrastructure.persistence.impl;

import com.meditrack.insurance_service.domain.model.InsurancePolicy;
import com.meditrack.insurance_service.domain.model.Relationship;
import com.meditrack.insurance_service.domain.repository.InsurancePolicyRepository;
import com.meditrack.insurance_service.infrastructure.persistence.entity.InsurancePolicyEntity;
import com.meditrack.insurance_service.infrastructure.persistence.repository.JpaInsurancePolicyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class InsurancePolicyRepositoryImpl implements InsurancePolicyRepository {

    private final JpaInsurancePolicyRepository jpaRepository;

    @Override
    public InsurancePolicy save(InsurancePolicy policy) {
        InsurancePolicyEntity entity = toEntity(policy);
        InsurancePolicyEntity saved = jpaRepository.save(entity);
        return toDomain(saved);
    }

    @Override
    public Optional<InsurancePolicy> findById(UUID policyId) {
        return jpaRepository.findById(policyId).map(this::toDomain);
    }

    @Override
    public List<InsurancePolicy> findByPatientId(UUID patientId) {
        return jpaRepository.findByPatientId(patientId).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<InsurancePolicy> findByPolicyNumber(String policyNumber) {
        return jpaRepository.findByPolicyNumber(policyNumber).map(this::toDomain);
    }

    @Override
    public boolean existsByPolicyNumber(String policyNumber) {
        return jpaRepository.existsByPolicyNumber(policyNumber);
    }

    // -------------------------------------------------------------------------
    // Mapping helpers
    // -------------------------------------------------------------------------

    private InsurancePolicyEntity toEntity(InsurancePolicy policy) {
        InsurancePolicyEntity entity = new InsurancePolicyEntity();
        entity.setPolicyId(policy.getPolicyId());
        entity.setPatientId(policy.getPatientId());
        entity.setPolicyNumber(policy.getPolicyNumber());
        entity.setPayerId(policy.getPayerId());
        entity.setPayerName(policy.getPayerName());
        entity.setPlanName(policy.getPlanName());
        entity.setGroupNumber(policy.getGroupNumber());
        entity.setSubscriberId(policy.getSubscriberId());
        entity.setSubscriberName(policy.getSubscriberName());
        entity.setRelationship(policy.getRelationship() != null ? policy.getRelationship().name() : null);
        entity.setEffectiveDate(policy.getEffectiveDate());
        entity.setTerminationDate(policy.getTerminationDate());
        entity.setActive(policy.isActive());
        entity.setCopayAmount(policy.getCopayAmount());
        entity.setDeductibleAmount(policy.getDeductibleAmount());
        entity.setDeductibleMet(policy.getDeductibleMet());
        entity.setOutOfPocketMax(policy.getOutOfPocketMax());
        entity.setOutOfPocketMet(policy.getOutOfPocketMet());
        return entity;
    }

    private InsurancePolicy toDomain(InsurancePolicyEntity entity) {
        return InsurancePolicy.builder()
                .policyId(entity.getPolicyId())
                .patientId(entity.getPatientId())
                .policyNumber(entity.getPolicyNumber())
                .payerId(entity.getPayerId())
                .payerName(entity.getPayerName())
                .planName(entity.getPlanName())
                .groupNumber(entity.getGroupNumber())
                .subscriberId(entity.getSubscriberId())
                .subscriberName(entity.getSubscriberName())
                .relationship(entity.getRelationship() != null ? Relationship.valueOf(entity.getRelationship()) : null)
                .effectiveDate(entity.getEffectiveDate())
                .terminationDate(entity.getTerminationDate())
                .active(entity.isActive())
                .copayAmount(entity.getCopayAmount())
                .deductibleAmount(entity.getDeductibleAmount())
                .deductibleMet(entity.getDeductibleMet())
                .outOfPocketMax(entity.getOutOfPocketMax())
                .outOfPocketMet(entity.getOutOfPocketMet())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
