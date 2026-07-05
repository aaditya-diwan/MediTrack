package com.meditrack.patient.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.meditrack.patient.events.LabTestOrderedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Slf4j
@Service
public class LabOrderEventPublisher {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    // Lab order events must go to patient-events; LabOrderEventConsumer in lab-service
    // listens on that topic — publishing to lab-events would be silently dropped.
    @Value("${spring.kafka.topics.patient-events}")
    private String topic;

    public LabOrderEventPublisher(KafkaTemplate<String, String> kafkaTemplate,
                                   ObjectMapper objectMapper) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    /**
     * Publishes on a separate thread and never propagates failures: a broker outage or serialization
     * error is logged, not thrown, so ordering a lab test cannot be blocked or 500'd by Kafka being
     * down (at-most-once / graceful degradation, matching the platform-wide publishing pattern).
     */
    @Async
    public void publishLabTestOrder(LabTestOrderedEvent event) {
        UUID orderId = event.getOrder().getOrderId();
        try {
            String payload = objectMapper.writeValueAsString(event);
            kafkaTemplate.send(topic, orderId.toString(), payload)
                    .whenComplete((result, ex) -> {
                        if (ex != null) {
                            log.error("Failed to publish lab-test-ordered event [orderId={}]: {}",
                                    orderId, ex.getMessage());
                        } else {
                            log.info("Published lab-test-ordered event [orderId={}, topic={}]", orderId, topic);
                        }
                    });
        } catch (Exception ex) {
            log.error("Error publishing lab-test-ordered event [orderId={}]: {}", orderId, ex.getMessage());
        }
    }
}
