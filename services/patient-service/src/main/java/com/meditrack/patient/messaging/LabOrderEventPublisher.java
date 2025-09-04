package com.meditrack.patient.messaging;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.meditrack.patient.events.LabTestOrderedEvent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class LabOrderEventPublisher {

    private final KafkaTemplate<String, String> kafkaTemplate;

    @Value("${spring.kafka.topics.lab-events}")
    private String topic;

    public LabOrderEventPublisher(KafkaTemplate<String, String> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publishLabTestOrder(LabTestOrderedEvent event) throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();
        String eventAsString = objectMapper.writeValueAsString(event);
        kafkaTemplate.send(topic, event.getOrder().getOrderId().toString(), eventAsString);
    }
}