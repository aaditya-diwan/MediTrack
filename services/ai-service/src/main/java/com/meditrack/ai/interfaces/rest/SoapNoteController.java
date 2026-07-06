package com.meditrack.ai.interfaces.rest;

import com.meditrack.ai.application.usecase.GenerateSoapNoteUseCase;
import com.meditrack.ai.domain.model.SoapNote;
import com.meditrack.ai.interfaces.dto.request.SoapNoteRequest;
import com.meditrack.ai.interfaces.dto.response.SoapNoteResponse;
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
public class SoapNoteController {

    private final GenerateSoapNoteUseCase generateSoapNote;

    @Operation(summary = "Generate a SOAP note from consultation notes",
            description = "Structures the clinician's free text into Subjective/Objective/Assessment/Plan "
                    + "without inventing findings; undocumented sections read \"Not documented.\"")
    @PostMapping("/soap-note")
    @PreAuthorize("hasAnyRole('DOCTOR', 'NURSE', 'ADMIN')")
    public ResponseEntity<SoapNoteResponse> generate(
            @Valid @RequestBody SoapNoteRequest request) {

        SoapNote note = generateSoapNote.generate(request.toCommand());
        return ResponseEntity.ok(SoapNoteResponse.from(note));
    }
}
