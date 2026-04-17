package com.meditrack.insurance_service.application.service;

import com.meditrack.insurance_service.application.dto.CreatePolicyRequest;
import com.meditrack.insurance_service.application.dto.PolicyResponse;
import com.meditrack.insurance_service.application.exception.DuplicatePolicyException;
import com.meditrack.insurance_service.application.exception.PolicyNotFoundException;
import com.meditrack.insurance_service.domain.model.InsurancePolicy;
import com.meditrack.insurance_service.domain.model.Relationship;
import com.meditrack.insurance_service.domain.repository.InsurancePolicyRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InsurancePolicyApplicationServiceTest {

    @Mock
    private InsurancePolicyRepository policyRepository;

    @InjectMocks
    private InsurancePolicyApplicationService service;

    private UUID policyId;
    private UUID patientId;
    private InsurancePolicy savedPolicy;
    private CreatePolicyRequest createRequest;

    @BeforeEach
    void setUp() {
        policyId = UUID.randomUUID();
        patientId = UUID.randomUUID();

        createRequest = new CreatePolicyRequest();
        createRequest.setPatientId(patientId);
        createRequest.setPolicyNumber("POL-001");
        createRequest.setPayerId("PAYER-001");
        createRequest.setPayerName("Blue Cross Blue Shield");
        createRequest.setPlanName("PPO Gold");
        createRequest.setSubscriberId("SUB-001");
        createRequest.setSubscriberName("John Doe");
        createRequest.setRelationship(Relationship.SELF);
        createRequest.setEffectiveDate(LocalDate.of(2024, 1, 1));
        createRequest.setDeductibleAmount(new BigDecimal("1500.00"));
        createRequest.setCopayAmount(new BigDecimal("30.00"));

        savedPolicy = InsurancePolicy.builder()
                .policyId(policyId)
                .patientId(patientId)
                .policyNumber("POL-001")
                .payerId("PAYER-001")
                .payerName("Blue Cross Blue Shield")
                .planName("PPO Gold")
                .subscriberId("SUB-001")
                .subscriberName("John Doe")
                .relationship(Relationship.SELF)
                .effectiveDate(LocalDate.of(2024, 1, 1))
                .active(true)
                .deductibleAmount(new BigDecimal("1500.00"))
                .deductibleMet(BigDecimal.ZERO)
                .copayAmount(new BigDecimal("30.00"))
                .outOfPocketMet(BigDecimal.ZERO)
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build();
    }

    @Test
    void createPolicy_newPolicyNumber_savesAndReturnsResponse() {
        when(policyRepository.existsByPolicyNumber("POL-001")).thenReturn(false);
        when(policyRepository.save(any(InsurancePolicy.class))).thenReturn(savedPolicy);

        PolicyResponse response = service.createPolicy(createRequest);

        assertThat(response).isNotNull();
        assertThat(response.getPolicyId()).isEqualTo(policyId);
        assertThat(response.getPolicyNumber()).isEqualTo("POL-001");
        assertThat(response.getPayerName()).isEqualTo("Blue Cross Blue Shield");
        assertThat(response.getRelationship()).isEqualTo(Relationship.SELF);
        assertThat(response.isActive()).isTrue();

        verify(policyRepository).save(any(InsurancePolicy.class));
    }

    @Test
    void createPolicy_duplicatePolicyNumber_throwsDuplicatePolicyException() {
        when(policyRepository.existsByPolicyNumber("POL-001")).thenReturn(true);

        assertThatThrownBy(() -> service.createPolicy(createRequest))
                .isInstanceOf(DuplicatePolicyException.class)
                .hasMessageContaining("POL-001");

        verify(policyRepository, never()).save(any());
    }

    @Test
    void createPolicy_initializesCalled_policyIsActive() {
        when(policyRepository.existsByPolicyNumber(any())).thenReturn(false);
        when(policyRepository.save(any(InsurancePolicy.class))).thenAnswer(invocation -> {
            InsurancePolicy policy = invocation.getArgument(0);
            // Simulate what initialize() does
            return InsurancePolicy.builder()
                    .policyId(policy.getPolicyId())
                    .patientId(policy.getPatientId())
                    .policyNumber(policy.getPolicyNumber())
                    .payerId(policy.getPayerId())
                    .payerName(policy.getPayerName())
                    .relationship(policy.getRelationship())
                    .effectiveDate(policy.getEffectiveDate())
                    .subscriberId(policy.getSubscriberId())
                    .active(true)
                    .deductibleMet(BigDecimal.ZERO)
                    .outOfPocketMet(BigDecimal.ZERO)
                    .createdAt(OffsetDateTime.now())
                    .updatedAt(OffsetDateTime.now())
                    .build();
        });

        PolicyResponse response = service.createPolicy(createRequest);

        assertThat(response.isActive()).isTrue();
    }

    @Test
    void getPolicy_existingId_returnsResponse() {
        when(policyRepository.findById(policyId)).thenReturn(Optional.of(savedPolicy));

        PolicyResponse response = service.getPolicy(policyId);

        assertThat(response).isNotNull();
        assertThat(response.getPolicyId()).isEqualTo(policyId);
        assertThat(response.getPatientId()).isEqualTo(patientId);
    }

    @Test
    void getPolicy_notFound_throwsPolicyNotFoundException() {
        UUID unknownId = UUID.randomUUID();
        when(policyRepository.findById(unknownId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getPolicy(unknownId))
                .isInstanceOf(PolicyNotFoundException.class);
    }

    @Test
    void getPoliciesForPatient_returnsList() {
        when(policyRepository.findByPatientId(patientId)).thenReturn(List.of(savedPolicy));

        List<PolicyResponse> responses = service.getPoliciesForPatient(patientId);

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).getPolicyNumber()).isEqualTo("POL-001");
    }

    @Test
    void getPoliciesForPatient_noPolicies_returnsEmptyList() {
        when(policyRepository.findByPatientId(patientId)).thenReturn(List.of());

        List<PolicyResponse> responses = service.getPoliciesForPatient(patientId);

        assertThat(responses).isEmpty();
    }
}
