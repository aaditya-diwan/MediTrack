package com.meditrack.labrotary_service.infrastructure.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI labServiceOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("MediTrack Lab Service API")
                        .version("1.0")
                        .description("Manages lab orders, lab results, and laboratory event publishing."));
    }
}
