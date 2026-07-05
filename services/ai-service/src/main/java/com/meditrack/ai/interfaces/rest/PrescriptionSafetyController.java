package com.meditrack.ai.interfaces.rest;

import com.meditrack.ai.application.usecase.CheckPrescriptionSafetyUseCase;
import com.meditrack.ai.domain.model.SafetyAssessment;
import com.meditrack.ai.interfaces.dto.request.PrescriptionSafetyRequest;
import com.meditrack.ai.interfaces.dto.response.SafetyAssessmentResponse;
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
public class PrescriptionSafetyController {

    private final CheckPrescriptionSafetyUseCase checkPrescriptionSafety;

    @Operation(summary = "Screen a prescription for drug-drug interactions and allergy conflicts",
            description = "Advisory clinical decision support. A high-risk or allergy-conflicting result also "
                    + "emits a prescription.safety.flagged.v1 event on the backbone.")
    @PostMapping("/prescription-safety")
    @PreAuthorize("hasAnyRole('DOCTOR', 'PHARMACIST', 'NURSE', 'ADMIN')")
    public ResponseEntity<SafetyAssessmentResponse> checkPrescriptionSafety(
            @Valid @RequestBody PrescriptionSafetyRequest request) {

        SafetyAssessment assessment = checkPrescriptionSafety.check(request.toCommand());
        return ResponseEntity.ok(SafetyAssessmentResponse.from(assessment));
    }
}
