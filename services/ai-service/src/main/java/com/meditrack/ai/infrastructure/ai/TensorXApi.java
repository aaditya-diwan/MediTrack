package com.meditrack.ai.infrastructure.ai;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Wire records for the TensorX (OpenAI-compatible) chat-completions API.
 * Kept internal to the infrastructure layer — the domain never sees these.
 */
final class TensorXApi {

    private TensorXApi() {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    record ChatRequest(
            String model,
            List<Message> messages,
            double temperature,
            @JsonProperty("response_format") ResponseFormat responseFormat
    ) {
    }

    record Message(String role, String content) {
    }

    record ResponseFormat(String type) {
        static ResponseFormat jsonObject() {
            return new ResponseFormat("json_object");
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record ChatResponse(List<Choice> choices) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Choice(Message message) {
    }
}
