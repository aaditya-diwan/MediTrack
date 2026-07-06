package com.meditrack.ai.interfaces.rest;

import com.meditrack.ai.application.usecase.SummarizePatientHistoryUseCase;
import com.meditrack.ai.domain.model.PatientHistorySummary;
import com.meditrack.ai.interfaces.dto.request.HistorySummaryRequest;
import com.meditrack.ai.interfaces.dto.response.HistorySummaryResponse;
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
public class PatientHistorySummaryController {

    private final SummarizePatientHistoryUseCase summarizeHistory;

    @Operation(summary = "Summarise a patient's history into a pre-consultation brief",
            description = "Advisory clinical decision support: key conditions, critical allergies, abnormal "
                    + "findings and red flags distilled strictly from the supplied record.")
    @PostMapping("/history-summary")
    @PreAuthorize("hasAnyRole('DOCTOR', 'NURSE', 'ADMIN')")
    public ResponseEntity<HistorySummaryResponse> summarize(
            @Valid @RequestBody HistorySummaryRequest request) {

        PatientHistorySummary summary = summarizeHistory.summarize(request.toCommand());
        return ResponseEntity.ok(HistorySummaryResponse.from(summary));
    }
}
