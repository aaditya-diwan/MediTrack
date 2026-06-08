package com.meditrack.appointment.infrastructure.messaging;

import com.meditrack.appointment.infrastructure.messaging.event.AppointmentBookedEvent;
import com.meditrack.appointment.infrastructure.messaging.event.AppointmentCompletedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class AppointmentEventProducer {

    private static final String BOOKED_TOPIC = "appointment.booked.v1";
    private static final String COMPLETED_TOPIC = "appointment.completed.v1";

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void publishBooked(AppointmentBookedEvent event) {
        log.info("Publishing appointment.booked for appointmentId={}", event.getAppointmentId());
        kafkaTemplate.send(BOOKED_TOPIC, event.getAppointmentId().toString(), event);
    }

    public void publishCompleted(AppointmentCompletedEvent event) {
        log.info("Publishing appointment.completed for appointmentId={}", event.getAppointmentId());
        kafkaTemplate.send(COMPLETED_TOPIC, event.getAppointmentId().toString(), event);
    }
}
