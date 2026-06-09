package com.meditrack.doctor.infrastructure.messaging;

import com.meditrack.doctor.infrastructure.messaging.event.DoctorCreatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class DoctorEventProducer {

    private static final String DOCTOR_CREATED_TOPIC = "doctor.created.v1";

    private final KafkaTemplate<String, Object> kafkaTemplate;

    /**
     * Publishes the doctor.created event only after the business transaction has committed, and on a
     * separate thread, so that broker unavailability can never roll back or block doctor creation.
     * Delivery failures are logged rather than propagated (at-most-once / graceful degradation).
     */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void publishDoctorCreated(DoctorCreatedEvent event) {
        log.info("Publishing doctor.created event for doctorId={}", event.getDoctorId());
        try {
            kafkaTemplate.send(DOCTOR_CREATED_TOPIC, event.getDoctorId().toString(), event)
                    .whenComplete((result, ex) -> {
                        if (ex != null) {
                            log.error("Failed to publish doctor.created event for doctorId={}: {}",
                                    event.getDoctorId(), ex.getMessage());
                        } else {
                            log.debug("Published doctor.created event for doctorId={}", event.getDoctorId());
                        }
                    });
        } catch (Exception ex) {
            log.error("Error publishing doctor.created event for doctorId={}: {}",
                    event.getDoctorId(), ex.getMessage());
        }
    }
}
