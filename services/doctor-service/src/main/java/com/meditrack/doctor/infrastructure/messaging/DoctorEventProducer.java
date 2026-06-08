package com.meditrack.doctor.infrastructure.messaging;

import com.meditrack.doctor.infrastructure.messaging.event.DoctorCreatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DoctorEventProducer {

    private static final String DOCTOR_CREATED_TOPIC = "doctor.created.v1";

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void publishDoctorCreated(DoctorCreatedEvent event) {
        log.info("Publishing doctor.created event for doctorId={}", event.getDoctorId());
        kafkaTemplate.send(DOCTOR_CREATED_TOPIC, event.getDoctorId().toString(), event);
    }
}
