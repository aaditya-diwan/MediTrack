package com.meditrack.labrotary_service.infrastructure.messaging;

import com.meditrack.labrotary_service.infrastructure.messaging.event.LabOrderEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LabOrderEventPublisher {

    private static final String TOPIC = "lab.test.ordered.v1";
    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void publishLabOrderCreatedEvent(LabOrderEvent event) {
        kafkaTemplate.send(TOPIC, event.getOrderId().toString(), event);
    }
}
