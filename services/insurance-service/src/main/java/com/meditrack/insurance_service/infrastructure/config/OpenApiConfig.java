package com.meditrack.insurance_service.infrastructure.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI insuranceServiceOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("MediTrack Insurance Service API")
                        .version("1.0")
                        .description("Manages insurance policies, eligibility verification, and coverage details."));
    }
}
