package com.meditrack.appointment.infrastructure.messaging;

import com.meditrack.appointment.infrastructure.messaging.event.AppointmentBookedEvent;
import com.meditrack.appointment.infrastructure.messaging.event.AppointmentCompletedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Publishes appointment domain events only after the business transaction commits, on a separate
 * thread, so that broker unavailability can never roll back or block the originating request.
 * Delivery failures are logged rather than propagated (at-most-once / graceful degradation).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AppointmentEventProducer {

    private static final String BOOKED_TOPIC = "appointment.booked.v1";
    private static final String COMPLETED_TOPIC = "appointment.completed.v1";

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void publishBooked(AppointmentBookedEvent event) {
        log.info("Publishing appointment.booked for appointmentId={}", event.getAppointmentId());
        send(BOOKED_TOPIC, event.getAppointmentId().toString(), event);
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void publishCompleted(AppointmentCompletedEvent event) {
        log.info("Publishing appointment.completed for appointmentId={}", event.getAppointmentId());
        send(COMPLETED_TOPIC, event.getAppointmentId().toString(), event);
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
