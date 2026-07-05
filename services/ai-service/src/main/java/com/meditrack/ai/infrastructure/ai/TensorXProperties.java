package com.meditrack.ai.infrastructure.ai;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration for the TensorX OpenAI-compatible inference endpoint.
 *
 * @param baseUrl        e.g. {@code https://api.tensorx.ai/v1}
 * @param apiKey         bearer token; when blank the service starts but calls fail fast with a clear message
 * @param model          open-weight model id, e.g. {@code deepseek/deepseek-chat-v3.1}
 * @param temperature    low (~0.1) for conservative, repeatable clinical output
 * @param timeoutSeconds read timeout for a single inference call
 */
@ConfigurationProperties(prefix = "tensorx")
public record TensorXProperties(
        String baseUrl,
        String apiKey,
        String model,
        double temperature,
        int timeoutSeconds
) {
}
