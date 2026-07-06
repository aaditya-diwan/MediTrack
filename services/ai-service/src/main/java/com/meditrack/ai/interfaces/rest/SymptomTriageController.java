package com.meditrack.ai.interfaces.rest;

import com.meditrack.ai.application.usecase.TriageSymptomsUseCase;
import com.meditrack.ai.domain.model.TriageAssessment;
import com.meditrack.ai.interfaces.dto.request.SymptomTriageRequest;
import com.meditrack.ai.interfaces.dto.response.SymptomTriageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Clinical Decision Support")
@RestController
@RequestMapping("/api/v1/ai")
@RequiredArgsConstructor
public class SymptomTriageController {

    private final TriageSymptomsUseCase triageSymptoms;

    @Operation(summary = "Triage a symptom presentation",
            description = "Advisory clinical decision support: conservative urgency assessment with "
                    + "red flags and a recommended specialty. Never a diagnosis.")
    @PostMapping("/symptom-triage")
    @PreAuthorize("hasAnyRole('DOCTOR', 'NURSE', 'ADMIN', 'PATIENT')")
    public ResponseEntity<SymptomTriageResponse> triage(
            @Valid @RequestBody SymptomTriageRequest request) {

        TriageAssessment assessment = triageSymptoms.triage(request.toCommand());
        return ResponseEntity.ok(SymptomTriageResponse.from(assessment));
    }
}
