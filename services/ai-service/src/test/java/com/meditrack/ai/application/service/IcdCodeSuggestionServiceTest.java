package com.meditrack.ai.application.service;

import com.meditrack.ai.domain.model.IcdCodeSuggestion;
import com.meditrack.ai.domain.model.IcdCodeSuggestionCommand;
import com.meditrack.ai.domain.model.IcdCodeSuggestions;
import com.meditrack.ai.domain.model.IcdConfidence;
import com.meditrack.ai.domain.port.ClinicalReasoningPort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IcdCodeSuggestionServiceTest {

    @Mock
    private ClinicalReasoningPort reasoningPort;

    @InjectMocks
    private IcdCodeSuggestionService service;

    private IcdCodeSuggestionCommand command() {
        return new IcdCodeSuggestionCommand(
                "Type 2 diabetes, poorly controlled. HbA1c 9.1%. Also treated for essential hypertension.",
                "T2DM with hypertension");
    }

    private IcdCodeSuggestion suggestion(int i) {
        return new IcdCodeSuggestion("E11." + i, "desc " + i, IcdConfidence.MODERATE, "rationale " + i);
    }

    @Test
    void returnsSuggestions_fromPort() {
        IcdCodeSuggestions fromModel = new IcdCodeSuggestions(
                List.of(new IcdCodeSuggestion("E11.9", "Type 2 diabetes mellitus without complications",
                                IcdConfidence.HIGH, "note states type 2 diabetes"),
                        new IcdCodeSuggestion("I10", "Essential (primary) hypertension",
                                IcdConfidence.HIGH, "note states essential hypertension")),
                "deepseek/deepseek-chat-v3.1");
        when(reasoningPort.suggestIcdCodes(any())).thenReturn(fromModel);

        IcdCodeSuggestions result = service.suggest(command());

        assertThat(result.suggestions()).hasSize(2);
        assertThat(result.suggestions().get(0).code()).isEqualTo("E11.9");
        verify(reasoningPort).suggestIcdCodes(any(IcdCodeSuggestionCommand.class));
    }

    @Test
    void capsSuggestionsAtEight_whenModelReturnsMore() {
        List<IcdCodeSuggestion> twelve = IntStream.range(0, 12)
                .mapToObj(this::suggestion)
                .toList();
        when(reasoningPort.suggestIcdCodes(any()))
                .thenReturn(new IcdCodeSuggestions(twelve, "deepseek/deepseek-chat-v3.1"));

        IcdCodeSuggestions result = service.suggest(command());

        assertThat(result.suggestions()).hasSize(IcdCodeSuggestionService.MAX_SUGGESTIONS);
        // Best-supported first: capping must keep the head of the list.
        assertThat(result.suggestions().get(0).code()).isEqualTo("E11.0");
        assertThat(result.suggestions().get(7).code()).isEqualTo("E11.7");
    }

    @Test
    void returnsEmptyList_whenModelHasNoSupportedCodes() {
        when(reasoningPort.suggestIcdCodes(any()))
                .thenReturn(new IcdCodeSuggestions(List.of(), "deepseek/deepseek-chat-v3.1"));

        IcdCodeSuggestions result = service.suggest(command());

        assertThat(result.suggestions()).isEmpty();
    }
}
