package com.meditrack.prescription.application.service;

import com.meditrack.prescription.application.exception.PrescriptionSafetyRejectedException;
import com.meditrack.prescription.domain.model.Prescription;
import com.meditrack.prescription.domain.model.PrescriptionMedication;
import com.meditrack.prescription.domain.model.PrescriptionStatus;
import com.meditrack.prescription.domain.port.PatientSafetyContext;
import com.meditrack.prescription.domain.port.PrescriptionSafetyPort;
import com.meditrack.prescription.domain.port.SafetyScreenResult;
import com.meditrack.prescription.domain.repository.PrescriptionRepository;
import com.meditrack.prescription.infrastructure.messaging.event.PrescriptionIssuedEvent;
import com.meditrack.prescription.interfaces.dto.response.PrescriptionResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for the AI drug-safety gate in the issue-prescription flow.
 * The safety port is mocked; persistence returns the entity passed in.
 */
@ExtendWith(MockitoExtension.class)
class PrescriptionApplicationServiceIssueTest {

    @Mock
    private PrescriptionRepository prescriptionRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private PrescriptionSafetyPort prescriptionSafetyPort;

    private PrescriptionApplicationService service;

    private UUID prescriptionId;
    private Prescription draft;

    @BeforeEach
    void setUp() {
        service = new PrescriptionApplicationService(prescriptionRepository, eventPublisher, prescriptionSafetyPort);
        prescriptionId = UUID.randomUUID();
        draft = Prescription.builder()
                .id(prescriptionId)
                .patientId(UUID.randomUUID())
                .doctorId(UUID.randomUUID())
                .status(PrescriptionStatus.DRAFT)
                .medications(new ArrayList<>(List.of(PrescriptionMedication.builder()
                        .id(UUID.randomUUID())
                        .medicationName("Warfarin").dosage("5mg").frequency("OD").route("oral")
                        .build())))
                .labOrders(new ArrayList<>())
                .build();
        when(prescriptionRepository.findById(prescriptionId)).thenReturn(Optional.of(draft));
    }

    private static SafetyScreenResult contraindicated() {
        return new SafetyScreenResult(true, "CONTRAINDICATED",
                "Warfarin + Aspirin: severe bleeding risk", true,
                List.of(new SafetyScreenResult.SafetyFinding(
                        "INTERACTION", "CONTRAINDICATED", "Warfarin + Aspirin: severe bleeding risk")));
    }

    @Test
    void issueIsBlockedWhenScreenReportsContraindicated() {
        when(prescriptionSafetyPort.screen(any(Prescription.class), any(PatientSafetyContext.class)))
                .thenReturn(contraindicated());

        assertThatThrownBy(() -> service.issuePrescription(prescriptionId, false, null))
                .isInstanceOf(PrescriptionSafetyRejectedException.class)
                .satisfies(ex -> {
                    SafetyScreenResult screen = ((PrescriptionSafetyRejectedException) ex).getScreenResult();
                    assertThat(screen.highestSeverity()).isEqualTo("CONTRAINDICATED");
                    assertThat(screen.findings()).hasSize(1);
                });

        verify(prescriptionRepository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any());
        assertThat(draft.getStatus()).isEqualTo(PrescriptionStatus.DRAFT);
    }

    @Test
    void issueIsAllowedWithOverrideDespiteBlockingFinding() {
        when(prescriptionSafetyPort.screen(any(Prescription.class), any(PatientSafetyContext.class)))
                .thenReturn(contraindicated());
        when(prescriptionRepository.save(any(Prescription.class))).thenAnswer(inv -> inv.getArgument(0));

        PrescriptionResponse response =
                service.issuePrescription(prescriptionId, true, "Benefit outweighs risk; INR monitored weekly");

        assertThat(response.getStatus()).isEqualTo(PrescriptionStatus.ISSUED.name());
        assertThat(response.getSafety().isChecked()).isTrue();
        assertThat(response.getSafety().getSeverity()).isEqualTo("CONTRAINDICATED");
        assertThat(response.getSafety().isOverridden()).isTrue();
        assertThat(response.getSafety().getOverrideReason())
                .isEqualTo("Benefit outweighs risk; INR monitored weekly");
        assertThat(response.getSafety().getFindings()).hasSize(1);
        verify(eventPublisher).publishEvent(any(PrescriptionIssuedEvent.class));
    }

    @Test
    void issueFailsOpenWhenSafetyServiceIsUnavailable() {
        when(prescriptionSafetyPort.screen(any(Prescription.class), any(PatientSafetyContext.class)))
                .thenReturn(SafetyScreenResult.notChecked("connection timed out"));
        when(prescriptionRepository.save(any(Prescription.class))).thenAnswer(inv -> inv.getArgument(0));

        PrescriptionResponse response = service.issuePrescription(prescriptionId, false, null);

        assertThat(response.getStatus()).isEqualTo(PrescriptionStatus.ISSUED.name());
        assertThat(response.getSafety().isChecked()).isFalse();
        assertThat(response.getSafety().getSeverity()).isNull();
        assertThat(response.getSafety().isOverridden()).isFalse();
        verify(eventPublisher).publishEvent(any(PrescriptionIssuedEvent.class));
    }

    @Test
    void issueProceedsOnCleanPass() {
        when(prescriptionSafetyPort.screen(any(Prescription.class), any(PatientSafetyContext.class)))
                .thenReturn(new SafetyScreenResult(true, "NONE", "No interactions identified", false, List.of()));
        when(prescriptionRepository.save(any(Prescription.class))).thenAnswer(inv -> inv.getArgument(0));

        PrescriptionResponse response = service.issuePrescription(prescriptionId, false, null);

        assertThat(response.getStatus()).isEqualTo(PrescriptionStatus.ISSUED.name());
        assertThat(response.getSafety().isChecked()).isTrue();
        assertThat(response.getSafety().getSeverity()).isEqualTo("NONE");
        assertThat(response.getSafety().getSummary()).isEqualTo("No interactions identified");
        assertThat(response.getSafety().isOverridden()).isFalse();
        assertThat(response.getSafety().getRequiresPharmacistReview()).isFalse();
        verify(eventPublisher).publishEvent(any(PrescriptionIssuedEvent.class));
    }

    @Test
    void moderateSeverityDoesNotBlockIssue() {
        when(prescriptionSafetyPort.screen(any(Prescription.class), any(PatientSafetyContext.class)))
                .thenReturn(new SafetyScreenResult(true, "MODERATE", "Monitor for dizziness", false,
                        List.of(new SafetyScreenResult.SafetyFinding(
                                "INTERACTION", "MODERATE", "Additive hypotensive effect"))));
        when(prescriptionRepository.save(any(Prescription.class))).thenAnswer(inv -> inv.getArgument(0));

        PrescriptionResponse response = service.issuePrescription(prescriptionId, false, null);

        assertThat(response.getStatus()).isEqualTo(PrescriptionStatus.ISSUED.name());
        assertThat(response.getSafety().getSeverity()).isEqualTo("MODERATE");
        assertThat(response.getSafety().isOverridden()).isFalse();
    }
}
