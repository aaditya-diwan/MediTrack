package com.meditrack.ai.interfaces.rest;

import com.meditrack.ai.application.usecase.SuggestIcdCodesUseCase;
import com.meditrack.ai.domain.model.IcdCodeSuggestions;
import com.meditrack.ai.interfaces.dto.request.IcdCodeSuggestionRequest;
import com.meditrack.ai.interfaces.dto.response.IcdCodeSuggestionResponse;
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
public class IcdCodeSuggestionController {

    private final SuggestIcdCodesUseCase suggestIcdCodes;

    @Operation(summary = "Suggest ICD-10 codes for a clinical note",
            description = "Advisory coding support: up to 8 ICD-10 code suggestions with confidence and "
                    + "rationale, derived strictly from the documented findings.")
    @PostMapping("/icd-codes")
    @PreAuthorize("hasAnyRole('DOCTOR', 'ADMIN')")
    public ResponseEntity<IcdCodeSuggestionResponse> suggest(
            @Valid @RequestBody IcdCodeSuggestionRequest request) {

        IcdCodeSuggestions suggestions = suggestIcdCodes.suggest(request.toCommand());
        return ResponseEntity.ok(IcdCodeSuggestionResponse.from(suggestions));
    }
}
