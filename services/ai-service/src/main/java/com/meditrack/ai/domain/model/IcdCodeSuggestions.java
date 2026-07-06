package com.meditrack.ai.domain.model;

import java.util.List;

/**
 * The set of ICD-10 code suggestions derived from a clinical note.
 *
 * @param suggestions ordered best-first; the application layer caps the size
 * @param modelUsed   the open-weight model that produced this
 */
public record IcdCodeSuggestions(
        List<IcdCodeSuggestion> suggestions,
        String modelUsed
) {
}
