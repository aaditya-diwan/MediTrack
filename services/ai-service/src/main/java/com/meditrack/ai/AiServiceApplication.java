package com.meditrack.ai;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * MediTrack AI / Clinical Decision Support service.
 *
 * <p>Stateless by design: it holds no PHI at rest. Each request is forwarded to
 * TensorX (EU-sovereign, OpenAI-compatible, zero-retention open-weight inference)
 * and the assessment is returned to the caller and optionally announced on Kafka.
 */
@SpringBootApplication
public class AiServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(AiServiceApplication.class, args);
    }
}
