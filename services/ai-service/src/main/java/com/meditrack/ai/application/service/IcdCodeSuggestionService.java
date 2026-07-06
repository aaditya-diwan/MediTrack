package com.meditrack.ai.application.service;

import com.meditrack.ai.application.usecase.SuggestIcdCodesUseCase;
import com.meditrack.ai.domain.model.IcdCodeSuggestionCommand;
import com.meditrack.ai.domain.model.IcdCodeSuggestions;
import com.meditrack.ai.domain.port.ClinicalReasoningPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Suggests ICD-10 codes for a clinical note by delegating to the clinical
 * reasoning port. The suggestion list is capped here so a verbose model can
 * never flood the coder with low-value candidates. Read-only: nothing is
 * persisted.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class IcdCodeSuggestionService implements SuggestIcdCodesUseCase {

    /** The prompt asks for at most 8, but the cap is enforced here regardless. */
    static final int MAX_SUGGESTIONS = 8;

    private final ClinicalReasoningPort reasoningPort;

    @Override
    public IcdCodeSuggestions suggest(IcdCodeSuggestionCommand command) {
        IcdCodeSuggestions result = reasoningPort.suggestIcdCodes(command);

        IcdCodeSuggestions capped = result.suggestions().size() <= MAX_SUGGESTIONS
                ? result
                : new IcdCodeSuggestions(
                        result.suggestions().subList(0, MAX_SUGGESTIONS), result.modelUsed());

        log.info("ICD-10 suggestion complete: {} suggestion(s) returned ({} from model)",
                capped.suggestions().size(), result.suggestions().size());
        return capped;
    }
}
