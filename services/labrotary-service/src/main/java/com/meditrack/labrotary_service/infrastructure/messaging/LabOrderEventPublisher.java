package com.meditrack.labrotary_service.infrastructure.messaging;

import com.meditrack.labrotary_service.infrastructure.messaging.event.EventTopics;
import com.meditrack.labrotary_service.infrastructure.messaging.event.LabOrderEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class LabOrderEventPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void publishLabOrderCreatedEvent(LabOrderEvent event) {
        log.info("Publishing lab-order-created event [orderId={}, topic={}]",
                event.getOrderId(), EventTopics.LAB_ORDER_CREATED);
        kafkaTemplate.send(EventTopics.LAB_ORDER_CREATED, event.getOrderId().toString(), event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish lab-order-created event [orderId={}]", event.getOrderId(), ex);
                    }
                });
    }
}
