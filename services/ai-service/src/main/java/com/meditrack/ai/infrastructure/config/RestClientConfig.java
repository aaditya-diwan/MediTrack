package com.meditrack.ai.infrastructure.config;

import com.meditrack.ai.infrastructure.ai.TensorXProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Duration;

/**
 * Builds the synchronous {@link RestClient} used to reach TensorX. RestClient
 * (Spring 6.1 / Boot 3.2) keeps this a plain servlet service — no reactive stack.
 * The bearer token is attached per-request in the adapter so a blank key never
 * breaks bean creation (the service still boots for health checks and CI).
 */
@Configuration
@EnableConfigurationProperties(TensorXProperties.class)
public class RestClientConfig {

    @Bean
    public RestClient tensorxRestClient(TensorXProperties props) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(10));
        factory.setReadTimeout(Duration.ofSeconds(props.timeoutSeconds()));

        return RestClient.builder()
                .requestFactory(factory)
                .baseUrl(props.baseUrl())
                .build();
    }
}
