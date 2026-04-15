package com.meditrack.patient.messaging;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.meditrack.patient.events.LabTestOrderedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

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

    public void publishLabTestOrder(LabTestOrderedEvent event) throws JsonProcessingException {
        String payload = objectMapper.writeValueAsString(event);
        kafkaTemplate.send(topic, event.getOrder().getOrderId().toString(), payload)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish lab-test-ordered event [orderId={}]",
                                event.getOrder().getOrderId(), ex);
                    } else {
                        log.info("Published lab-test-ordered event [orderId={}, topic={}]",
                                event.getOrder().getOrderId(), topic);
                    }
                });
    }
}
