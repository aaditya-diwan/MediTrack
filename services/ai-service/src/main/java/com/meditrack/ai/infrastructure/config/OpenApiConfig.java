package com.meditrack.ai.infrastructure.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {
    @Bean
    public OpenAPI aiServiceOpenAPI() {
        return new OpenAPI().info(new Info()
                .title("MediTrack AI Service API")
                .description("Clinical decision support powered by TensorX open-weight inference. "
                        + "Advisory only — a licensed clinician or pharmacist must verify every result.")
                .version("1.0.0"));
    }
}
