package com.meditrack.ai.application.service;

import com.meditrack.ai.domain.model.AllergyConflict;
import com.meditrack.ai.domain.model.DrugInteraction;
import com.meditrack.ai.domain.model.Medication;
import com.meditrack.ai.domain.model.SafetyAssessment;
import com.meditrack.ai.domain.model.SafetyCheckCommand;
import com.meditrack.ai.domain.model.Severity;
import com.meditrack.ai.domain.port.ClinicalReasoningPort;
import com.meditrack.ai.infrastructure.messaging.PrescriptionSafetyEventProducer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PrescriptionSafetyServiceTest {

    @Mock
    private ClinicalReasoningPort reasoningPort;

    @Mock
    private PrescriptionSafetyEventProducer eventProducer;

    @InjectMocks
    private PrescriptionSafetyService service;

    private SafetyCheckCommand command() {
        return new SafetyCheckCommand(
                List.of(new Medication("Warfarin", "5mg", "oral")),
                List.of("Aspirin"),
                List.of(),
                62, "MALE", null, null);
    }

    @Test
    void publishesFlaggedEvent_whenRiskIsMajor() {
        SafetyAssessment major = new SafetyAssessment(
                Severity.MAJOR, "bleeding risk", "hold aspirin", true,
                List.of(new DrugInteraction("Warfarin", "Aspirin", Severity.MAJOR,
                        "additive anticoagulation", "bleeding", "monitor INR")),
                List.of(), "deepseek/deepseek-chat-v3.1");
        when(reasoningPort.assess(any())).thenReturn(major);

        SafetyAssessment result = service.check(command());

        assertThat(result.overallRisk()).isEqualTo(Severity.MAJOR);
        verify(eventProducer).publishFlagged(any(), any());
    }

    @Test
    void publishesFlaggedEvent_whenAllergyConflictPresent() {
        SafetyAssessment withAllergy = new SafetyAssessment(
                Severity.MODERATE, "allergy", "avoid", true,
                List.of(),
                List.of(new AllergyConflict("Amoxicillin", "penicillin", Severity.MODERATE, "cross-reactivity")),
                "deepseek/deepseek-chat-v3.1");
        when(reasoningPort.assess(any())).thenReturn(withAllergy);

        service.check(command());

        verify(eventProducer).publishFlagged(any(), any());
    }

    @Test
    void doesNotPublish_whenNoRiskAndNoAllergy() {
        SafetyAssessment clear = new SafetyAssessment(
                Severity.NONE, "no issues", "proceed", false,
                List.of(), List.of(), "deepseek/deepseek-chat-v3.1");
        when(reasoningPort.assess(any())).thenReturn(clear);

        SafetyAssessment result = service.check(command());

        assertThat(result.isFlagged()).isFalse();
        verify(eventProducer, never()).publishFlagged(any(), any());
    }
}
