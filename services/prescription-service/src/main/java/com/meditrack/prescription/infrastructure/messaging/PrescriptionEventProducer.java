package com.meditrack.prescription.infrastructure.messaging;

import com.meditrack.prescription.infrastructure.messaging.event.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Publishes prescription domain events only after the business transaction commits, on a separate
 * thread, so that broker unavailability can never roll back or block the originating request.
 * Delivery failures are logged rather than propagated (at-most-once / graceful degradation).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PrescriptionEventProducer {

    private static final String ISSUED_TOPIC = "prescription.issued.v1";
    private static final String PHARMACY_TOPIC = "prescription.sent_to_pharmacy.v1";
    private static final String LAB_TOPIC = "prescription.sent_to_lab.v1";

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void publishIssued(PrescriptionIssuedEvent event) {
        log.info("Publishing prescription.issued for id={}", event.getPrescriptionId());
        send(ISSUED_TOPIC, event.getPrescriptionId().toString(), event);
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void publishSentToPharmacy(PrescriptionSentToPharmacyEvent event) {
        log.info("Publishing prescription.sent_to_pharmacy for id={}", event.getPrescriptionId());
        send(PHARMACY_TOPIC, event.getPrescriptionId().toString(), event);
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void publishSentToLab(PrescriptionSentToLabEvent event) {
        log.info("Publishing prescription.sent_to_lab for id={}", event.getPrescriptionId());
        send(LAB_TOPIC, event.getPrescriptionId().toString(), event);
    }

    private void send(String topic, String key, Object event) {
        try {
            kafkaTemplate.send(topic, key, event)
                    .whenComplete((result, ex) -> {
                        if (ex != null) {
                            log.error("Failed to publish event to topic={} key={}: {}", topic, key, ex.getMessage());
                        } else {
                            log.debug("Published event to topic={} key={}", topic, key);
                        }
                    });
        } catch (Exception ex) {
            log.error("Error publishing event to topic={} key={}: {}", topic, key, ex.getMessage());
        }
    }
}
