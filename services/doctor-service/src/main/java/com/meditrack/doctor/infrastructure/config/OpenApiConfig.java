package com.meditrack.doctor.infrastructure.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {
    @Bean
    public OpenAPI doctorServiceOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("MediTrack Doctor Service API")
                        .description("Doctor management and availability scheduling")
                        .version("1.0.0"));
    }
}
