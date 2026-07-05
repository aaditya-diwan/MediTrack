package com.meditrack.ai.interfaces.dto.request;

import com.meditrack.ai.domain.model.LabResultExplanationCommand;
import com.meditrack.ai.domain.model.LabValue;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;
import java.util.Optional;

/** Self-contained lab-result explanation request; the AI service persists none of it. */
public record LabResultExplanationRequest(
        @NotEmpty(message = "at least one lab result is required")
        @Valid
        List<LabValueInput> results,
        Integer patientAgeYears,
        String patientSex,
        String context
) {

    public LabResultExplanationCommand toCommand() {
        List<LabValue> values = Optional.ofNullable(results).orElse(List.of())
                .stream()
                .map(r -> new LabValue(r.testName(), r.value(), r.unit(), r.referenceRange(), r.flag()))
                .toList();

        return new LabResultExplanationCommand(values, patientAgeYears, patientSex, context);
    }
}
