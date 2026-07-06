package com.meditrack.ai.application.usecase;

import com.meditrack.ai.domain.model.IcdCodeSuggestionCommand;
import com.meditrack.ai.domain.model.IcdCodeSuggestions;

/** Suggest ICD-10 codes supported by a clinical note. */
public interface SuggestIcdCodesUseCase {

    IcdCodeSuggestions suggest(IcdCodeSuggestionCommand command);
}
