package com.meditrack.insurance_service.application.service;

import com.meditrack.insurance_service.application.dto.CreatePolicyRequest;
import com.meditrack.insurance_service.application.dto.PolicyResponse;
import com.meditrack.insurance_service.application.exception.DuplicatePolicyException;
import com.meditrack.insurance_service.application.exception.PolicyNotFoundException;
import com.meditrack.insurance_service.application.usecase.CreatePolicyUseCase;
import com.meditrack.insurance_service.application.usecase.GetPatientPoliciesUseCase;
import com.meditrack.insurance_service.application.usecase.GetPolicyUseCase;
import com.meditrack.insurance_service.domain.model.InsurancePolicy;
import com.meditrack.insurance_service.domain.repository.InsurancePolicyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class InsurancePolicyApplicationService
        implements CreatePolicyUseCase, GetPolicyUseCase, GetPatientPoliciesUseCase {

    private final InsurancePolicyRepository policyRepository;

    @Override
    @Transactional
    public PolicyResponse createPolicy(CreatePolicyRequest request) {
        log.info("Creating insurance policy for patientId={}, policyNumber={}",
                request.getPatientId(), request.getPolicyNumber());

        if (policyRepository.existsByPolicyNumber(request.getPolicyNumber())) {
            throw new DuplicatePolicyException(request.getPolicyNumber());
        }

        InsurancePolicy policy = InsurancePolicy.builder()
                .patientId(request.getPatientId())
                .policyNumber(request.getPolicyNumber())
                .payerId(request.getPayerId())
                .payerName(request.getPayerName())
                .planName(request.getPlanName())
                .groupNumber(request.getGroupNumber())
                .subscriberId(request.getSubscriberId())
                .subscriberName(request.getSubscriberName())
                .relationship(request.getRelationship())
                .effectiveDate(request.getEffectiveDate())
                .terminationDate(request.getTerminationDate())
                .copayAmount(request.getCopayAmount())
                .deductibleAmount(request.getDeductibleAmount())
                .outOfPocketMax(request.getOutOfPocketMax())
                .build();

        policy.initialize();

        InsurancePolicy saved = policyRepository.save(policy);
        log.info("Created insurance policy [policyId={}, patientId={}]", saved.getPolicyId(), saved.getPatientId());

        return toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public PolicyResponse getPolicy(UUID policyId) {
        log.debug("Fetching policy [policyId={}]", policyId);
        InsurancePolicy policy = policyRepository.findById(policyId)
                .orElseThrow(() -> new PolicyNotFoundException(policyId));
        return toResponse(policy);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PolicyResponse> getPoliciesForPatient(UUID patientId) {
        log.debug("Fetching policies for patientId={}", patientId);
        return policyRepository.findByPatientId(patientId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    // -------------------------------------------------------------------------
    // Mapping
    // -------------------------------------------------------------------------

    private PolicyResponse toResponse(InsurancePolicy policy) {
        return PolicyResponse.builder()
                .policyId(policy.getPolicyId())
                .patientId(policy.getPatientId())
                .policyNumber(policy.getPolicyNumber())
                .payerId(policy.getPayerId())
                .payerName(policy.getPayerName())
                .planName(policy.getPlanName())
                .groupNumber(policy.getGroupNumber())
                .subscriberId(policy.getSubscriberId())
                .subscriberName(policy.getSubscriberName())
                .relationship(policy.getRelationship())
                .effectiveDate(policy.getEffectiveDate())
                .terminationDate(policy.getTerminationDate())
                .active(policy.isActive())
                .copayAmount(policy.getCopayAmount())
                .deductibleAmount(policy.getDeductibleAmount())
                .deductibleMet(policy.getDeductibleMet())
                .outOfPocketMax(policy.getOutOfPocketMax())
                .outOfPocketMet(policy.getOutOfPocketMet())
                .createdAt(policy.getCreatedAt())
                .updatedAt(policy.getUpdatedAt())
                .build();
    }
}
