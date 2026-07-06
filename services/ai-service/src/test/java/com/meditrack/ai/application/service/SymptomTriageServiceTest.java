package com.meditrack.ai.application.service;

import com.meditrack.ai.domain.model.TriageAssessment;
import com.meditrack.ai.domain.model.TriageCommand;
import com.meditrack.ai.domain.model.TriageUrgency;
import com.meditrack.ai.domain.port.ClinicalReasoningPort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SymptomTriageServiceTest {

    @Mock
    private ClinicalReasoningPort reasoningPort;

    @InjectMocks
    private SymptomTriageService service;

    private TriageCommand command() {
        return new TriageCommand(
                58, "MALE", "crushing chest pain radiating to left arm", "30 minutes",
                List.of("hypertension"), List.of("amlodipine"), List.of());
    }

    @Test
    void returnsEmergencyAssessment_withRedFlags() {
        TriageAssessment emergency = new TriageAssessment(
                TriageUrgency.EMERGENCY, "Emergency Medicine",
                List.of("chest pain radiating to arm"),
                "possible acute coronary syndrome presentation", null,
                "deepseek/deepseek-chat-v3.1");
        when(reasoningPort.triage(any())).thenReturn(emergency);

        TriageAssessment result = service.triage(command());

        assertThat(result.urgency()).isEqualTo(TriageUrgency.EMERGENCY);
        assertThat(result.urgency().isEmergency()).isTrue();
        assertThat(result.redFlags()).isNotEmpty();
        verify(reasoningPort).triage(any(TriageCommand.class));
    }

    @Test
    void returnsRoutineAssessment_withSelfCareAdvice() {
        TriageAssessment routine = new TriageAssessment(
                TriageUrgency.ROUTINE, "General Practice", List.of(),
                "mild self-limiting symptoms", "rest and fluids",
                "deepseek/deepseek-chat-v3.1");
        when(reasoningPort.triage(any())).thenReturn(routine);

        TriageAssessment result = service.triage(command());

        assertThat(result.urgency()).isEqualTo(TriageUrgency.ROUTINE);
        assertThat(result.urgency().isEmergency()).isFalse();
        assertThat(result.selfCareAdvice()).isEqualTo("rest and fluids");
    }

    @Test
    void passesCommandThroughToPort_unchanged() {
        TriageCommand command = command();
        TriageAssessment urgent = new TriageAssessment(
                TriageUrgency.URGENT, "Cardiology", List.of(), "needs prompt review", null,
                "deepseek/deepseek-chat-v3.1");
        when(reasoningPort.triage(command)).thenReturn(urgent);

        TriageAssessment result = service.triage(command);

        assertThat(result).isSameAs(urgent);
        verify(reasoningPort).triage(command);
    }
}
