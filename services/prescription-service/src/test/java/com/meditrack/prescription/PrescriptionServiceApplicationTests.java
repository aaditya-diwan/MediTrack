package com.meditrack.prescription;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class PrescriptionServiceApplicationTests {

    @MockBean
    KafkaTemplate<String, Object> kafkaTemplate;

    @Test
    void contextLoads() {
    }
}
