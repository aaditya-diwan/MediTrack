package com.meditrack.ai.application.service;

import com.meditrack.ai.domain.model.HistorySummaryCommand;
import com.meditrack.ai.domain.model.LabValue;
import com.meditrack.ai.domain.model.PatientHistorySummary;
import com.meditrack.ai.domain.model.VisitNote;
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
class PatientHistorySummaryServiceTest {

    @Mock
    private ClinicalReasoningPort reasoningPort;

    @InjectMocks
    private PatientHistorySummaryService service;

    private HistorySummaryCommand command() {
        return new HistorySummaryCommand(
                67, "FEMALE",
                List.of("type 2 diabetes", "chronic kidney disease stage 3"),
                List.of("metformin", "lisinopril"),
                List.of("penicillin"),
                List.of(new LabValue("eGFR", "44", null, null, "LOW")),
                List.of(new VisitNote("2026-06-01", "Complains of increasing fatigue.")));
    }

    @Test
    void returnsBrief_withRedFlagsAndAbnormalFindings() {
        PatientHistorySummary brief = new PatientHistorySummary(
                List.of("type 2 diabetes", "CKD stage 3"),
                List.of("metformin", "lisinopril"),
                List.of("penicillin"),
                List.of("eGFR 44 (LOW)"),
                List.of("metformin with declining renal function"),
                "67-year-old woman with T2DM and CKD stage 3.",
                List.of("review metformin dose against renal function"),
                "deepseek/deepseek-chat-v3.1");
        when(reasoningPort.summarizeHistory(any())).thenReturn(brief);

        PatientHistorySummary result = service.summarize(command());

        assertThat(result.keyConditions()).contains("type 2 diabetes");
        assertThat(result.criticalAllergies()).containsExactly("penicillin");
        assertThat(result.recentAbnormalFindings()).isNotEmpty();
        assertThat(result.redFlags()).isNotEmpty();
        verify(reasoningPort).summarizeHistory(any(HistorySummaryCommand.class));
    }

    @Test
    void returnsBrief_withEmptySectionsForUnremarkableRecord() {
        PatientHistorySummary quiet = new PatientHistorySummary(
                List.of(), List.of(), List.of(), List.of(), List.of(),
                "No significant history in the supplied record.",
                List.of(),
                "deepseek/deepseek-chat-v3.1");
        when(reasoningPort.summarizeHistory(any())).thenReturn(quiet);

        PatientHistorySummary result = service.summarize(command());

        assertThat(result.redFlags()).isEmpty();
        assertThat(result.recentAbnormalFindings()).isEmpty();
        assertThat(result.narrativeSummary()).isNotBlank();
    }

    @Test
    void passesCommandThroughToPort_unchanged() {
        HistorySummaryCommand command = command();
        PatientHistorySummary brief = new PatientHistorySummary(
                List.of("x"), List.of(), List.of(), List.of(), List.of(), "s", List.of(),
                "deepseek/deepseek-chat-v3.1");
        when(reasoningPort.summarizeHistory(command)).thenReturn(brief);

        PatientHistorySummary result = service.summarize(command);

        assertThat(result).isSameAs(brief);
        verify(reasoningPort).summarizeHistory(command);
    }
}
