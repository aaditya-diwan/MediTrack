package com.meditrack.ai.interfaces.rest;

import com.meditrack.ai.application.usecase.ExplainLabResultUseCase;
import com.meditrack.ai.domain.model.LabResultExplanation;
import com.meditrack.ai.interfaces.dto.request.LabResultExplanationRequest;
import com.meditrack.ai.interfaces.dto.response.LabResultExplanationResponse;
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
public class LabExplanationController {

    private final ExplainLabResultUseCase explainLabResult;

    @Operation(summary = "Explain a panel of lab results",
            description = "Advisory clinical decision support: plain-language interpretation of lab results "
                    + "with per-test detail and an overall urgency.")
    @PostMapping("/lab-result-explanation")
    @PreAuthorize("hasAnyRole('DOCTOR', 'NURSE', 'LAB_TECH', 'ADMIN')")
    public ResponseEntity<LabResultExplanationResponse> explain(
            @Valid @RequestBody LabResultExplanationRequest request) {

        LabResultExplanation explanation = explainLabResult.explain(request.toCommand());
        return ResponseEntity.ok(LabResultExplanationResponse.from(explanation));
    }
}
