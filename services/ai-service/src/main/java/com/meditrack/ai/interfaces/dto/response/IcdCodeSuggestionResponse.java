package com.meditrack.ai.interfaces.dto.response;

import com.meditrack.ai.domain.model.IcdCodeSuggestions;

import java.time.Instant;
import java.util.List;

/**
 * API response for ICD-10 code suggestions. The caveat is set server-side and
 * never comes from the model.
 */
public record IcdCodeSuggestionResponse(
        List<SuggestionView> suggestions,
        String caveat,
        String modelUsed,
        Instant generatedAt
) {

    private static final String CAVEAT =
            "AI-suggested ICD-10 codes for coding support only. Verify each code against the official "
            + "ICD-10 index and the full documentation before billing or filing.";

    public record SuggestionView(String code, String description, String confidence, String rationale) {
    }

    public static IcdCodeSuggestionResponse from(IcdCodeSuggestions s) {
        List<SuggestionView> views = s.suggestions().stream()
                .map(v -> new SuggestionView(v.code(), v.description(), v.confidence().name(), v.rationale()))
                .toList();

        return new IcdCodeSuggestionResponse(views, CAVEAT, s.modelUsed(), Instant.now());
    }
}
