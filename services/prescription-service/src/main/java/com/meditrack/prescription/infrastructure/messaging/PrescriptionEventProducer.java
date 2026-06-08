package com.meditrack.prescription.infrastructure.messaging;

import com.meditrack.prescription.infrastructure.messaging.event.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PrescriptionEventProducer {

    private static final String ISSUED_TOPIC = "prescription.issued.v1";
    private static final String PHARMACY_TOPIC = "prescription.sent_to_pharmacy.v1";
    private static final String LAB_TOPIC = "prescription.sent_to_lab.v1";

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void publishIssued(PrescriptionIssuedEvent event) {
        log.info("Publishing prescription.issued for id={}", event.getPrescriptionId());
        kafkaTemplate.send(ISSUED_TOPIC, event.getPrescriptionId().toString(), event);
    }

    public void publishSentToPharmacy(PrescriptionSentToPharmacyEvent event) {
        log.info("Publishing prescription.sent_to_pharmacy for id={}", event.getPrescriptionId());
        kafkaTemplate.send(PHARMACY_TOPIC, event.getPrescriptionId().toString(), event);
    }

    public void publishSentToLab(PrescriptionSentToLabEvent event) {
        log.info("Publishing prescription.sent_to_lab for id={}", event.getPrescriptionId());
        kafkaTemplate.send(LAB_TOPIC, event.getPrescriptionId().toString(), event);
    }
}
