package com.meditrack.ai.interfaces.dto.request;

import com.meditrack.ai.domain.model.HistorySummaryCommand;
import com.meditrack.ai.domain.model.LabValue;
import com.meditrack.ai.domain.model.VisitNote;
import jakarta.validation.Valid;

import java.util.List;
import java.util.Optional;

/** Self-contained patient-history summary request; the AI service persists none of it. */
public record HistorySummaryRequest(
        Integer patientAgeYears,
        String patientSex,
        List<String> conditions,
        List<String> medications,
        List<String> allergies,
        @Valid
        List<HistoryLabResultInput> recentLabResults,
        @Valid
        List<VisitNoteInput> pastVisits
) {

    public HistorySummaryCommand toCommand() {
        List<LabValue> labs = Optional.ofNullable(recentLabResults).orElse(List.of())
                .stream()
                .map(r -> new LabValue(r.name(), r.value(), null, null, r.flag()))
                .toList();

        List<VisitNote> visits = Optional.ofNullable(pastVisits).orElse(List.of())
                .stream()
                .map(v -> new VisitNote(v.date(), v.note()))
                .toList();

        return new HistorySummaryCommand(
                patientAgeYears,
                patientSex,
                Optional.ofNullable(conditions).orElse(List.of()),
                Optional.ofNullable(medications).orElse(List.of()),
                Optional.ofNullable(allergies).orElse(List.of()),
                labs,
                visits
        );
    }
}
