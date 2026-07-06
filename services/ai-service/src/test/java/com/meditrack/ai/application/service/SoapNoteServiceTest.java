package com.meditrack.ai.application.service;

import com.meditrack.ai.domain.model.SoapNote;
import com.meditrack.ai.domain.model.SoapNoteCommand;
import com.meditrack.ai.domain.port.ClinicalReasoningPort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SoapNoteServiceTest {

    @Mock
    private ClinicalReasoningPort reasoningPort;

    @InjectMocks
    private SoapNoteService service;

    private SoapNoteCommand command() {
        return new SoapNoteCommand(
                "Patient reports 3 days of productive cough. Chest clear, temp 37.2. "
                        + "Likely viral URTI. Advised rest and fluids.",
                45, "FEMALE", List.of("asthma"), Map.of("temperature", "37.2"));
    }

    @Test
    void returnsStructuredNote_fromPort() {
        SoapNote note = new SoapNote(
                "3 days of productive cough",
                "Chest clear, temperature 37.2",
                "Likely viral URTI",
                "Rest and fluids",
                List.of("viral URTI"),
                null,
                "deepseek/deepseek-chat-v3.1");
        when(reasoningPort.generateSoapNote(any())).thenReturn(note);

        SoapNote result = service.generate(command());

        assertThat(result.subjective()).isEqualTo("3 days of productive cough");
        assertThat(result.assessmentProblems()).containsExactly("viral URTI");
        assertThat(result.followUp()).isNull();
        verify(reasoningPort).generateSoapNote(any(SoapNoteCommand.class));
    }

    @Test
    void preservesNotDocumentedSections() {
        SoapNote sparse = new SoapNote(
                "headache since morning",
                "Not documented.",
                "Not documented.",
                "Not documented.",
                List.of(),
                null,
                "deepseek/deepseek-chat-v3.1");
        when(reasoningPort.generateSoapNote(any())).thenReturn(sparse);

        SoapNote result = service.generate(command());

        assertThat(result.objective()).isEqualTo("Not documented.");
        assertThat(result.assessment()).isEqualTo("Not documented.");
        assertThat(result.plan()).isEqualTo("Not documented.");
        assertThat(result.assessmentProblems()).isEmpty();
    }

    @Test
    void passesCommandThroughToPort_unchanged() {
        SoapNoteCommand command = command();
        SoapNote note = new SoapNote("s", "o", "a", "p", List.of(), "review in 2 weeks",
                "deepseek/deepseek-chat-v3.1");
        when(reasoningPort.generateSoapNote(command)).thenReturn(note);

        SoapNote result = service.generate(command);

        assertThat(result).isSameAs(note);
        verify(reasoningPort).generateSoapNote(command);
    }
}
