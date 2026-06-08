package com.meditrack.prescription.infrastructure.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {
    @Bean
    public OpenAPI prescriptionServiceOpenAPI() {
        return new OpenAPI().info(new Info()
                .title("MediTrack Prescription Service API")
                .description("Prescription management and dispatch")
                .version("1.0.0"));
    }
}
