package com.meditrack.ai.application.service;

import com.meditrack.ai.domain.model.ClinicalUrgency;
import com.meditrack.ai.domain.model.LabResultExplanation;
import com.meditrack.ai.domain.model.LabResultExplanationCommand;
import com.meditrack.ai.domain.model.LabValue;
import com.meditrack.ai.domain.port.ClinicalReasoningPort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LabResultExplanationServiceTest {

    @Mock
    private ClinicalReasoningPort reasoningPort;

    @InjectMocks
    private LabResultExplanationService service;

    private LabResultExplanationCommand command() {
        return new LabResultExplanationCommand(
                List.of(new LabValue("Potassium", "6.2", "mmol/L", "3.5-5.1", "H")),
                62, "MALE", "routine follow-up");
    }

    @Test
    void delegatesToPortAndReturnsExplanation() {
        LabResultExplanation expected = new LabResultExplanation(
                "Hyperkalemia", "Your potassium is high", "Recheck and review meds",
                ClinicalUrgency.URGENT, List.of(), "deepseek/deepseek-chat-v3.1");
        when(reasoningPort.explainLabResult(any())).thenReturn(expected);

        LabResultExplanation result = service.explain(command());

        assertThat(result).isSameAs(expected);
    }

    @Test
    void passesTheCommandThroughUnchanged() {
        when(reasoningPort.explainLabResult(any())).thenReturn(
                new LabResultExplanation("", "", "", ClinicalUrgency.ROUTINE, List.of(), "m"));
        LabResultExplanationCommand cmd = command();

        service.explain(cmd);

        ArgumentCaptor<LabResultExplanationCommand> captor =
                ArgumentCaptor.forClass(LabResultExplanationCommand.class);
        verify(reasoningPort).explainLabResult(captor.capture());
        assertThat(captor.getValue()).isSameAs(cmd);
        assertThat(captor.getValue().results()).hasSize(1);
    }
}
