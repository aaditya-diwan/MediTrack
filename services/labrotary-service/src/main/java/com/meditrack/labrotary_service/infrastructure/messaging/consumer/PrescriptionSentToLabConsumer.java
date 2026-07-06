package com.meditrack.labrotary_service.infrastructure.messaging.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.meditrack.labrotary_service.application.dto.LabOrderRequest;
import com.meditrack.labrotary_service.application.dto.TestInfoDto;
import com.meditrack.labrotary_service.application.usecase.CreateLabOrderUseCase;
import com.meditrack.labrotary_service.domain.model.Priority;
import com.meditrack.labrotary_service.domain.repository.LabOrderRepository;
import com.meditrack.labrotary_service.infrastructure.messaging.event.PrescriptionSentToLabEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.List;

/**
 * Kafka consumer that listens to prescription.sent_to_lab.v1 events from
 * prescription-service and creates one LabOrder per lab order item via the
 * existing create-order use case.
 *
 * Idempotency: an item is skipped when a lab order for that
 * prescriptionId + testCode combination already exists (the prescriptionId is
 * stored on the order as its external reference).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PrescriptionSentToLabConsumer {

    /**
     * The event does not carry the patient's MRN or the ordering provider's display name,
     * but both columns are NOT NULL in the lab_orders schema.
     */
    static final String UNKNOWN = "UNKNOWN";

    private final CreateLabOrderUseCase createLabOrderUseCase;
    private final LabOrderRepository labOrderRepository;
    private final ObjectMapper objectMapper;

    /**
     * Consumes prescription-sent-to-lab events from prescription-service.
     * Topic: prescription.sent_to_lab.v1 (configured in application.yml)
     */
    @KafkaListener(
        topics = "${spring.kafka.topics.prescription-sent-to-lab}",
        groupId = "${spring.kafka.consumer.group-id}",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumePrescriptionSentToLabEvent(String eventJson, Acknowledgment acknowledgment) {
        try {
            log.info("Received prescription-sent-to-lab event: {}", eventJson);

            PrescriptionSentToLabEvent event = objectMapper.readValue(eventJson, PrescriptionSentToLabEvent.class);

            List<PrescriptionSentToLabEvent.LabItem> items =
                    event.getLabOrders() == null ? Collections.emptyList() : event.getLabOrders();

            for (PrescriptionSentToLabEvent.LabItem item : items) {
                processLabItem(event, item);
            }

            // Manually acknowledge successful processing
            acknowledgment.acknowledge();
            log.info("Successfully processed prescription-sent-to-lab event: prescriptionId={}, items={}",
                    event.getPrescriptionId(), items.size());

        } catch (Exception e) {
            log.error("Error processing prescription-sent-to-lab event: {}", e.getMessage(), e);
            // Don't acknowledge - message will be reprocessed or sent to DLQ based on config
            throw new RuntimeException("Failed to process prescription-sent-to-lab event", e);
        }
    }

    /**
     * Creates a lab order for a single lab item of the prescription,
     * skipping items that were already turned into an order (idempotency).
     */
    private void processLabItem(PrescriptionSentToLabEvent event, PrescriptionSentToLabEvent.LabItem item) {
        String prescriptionId = event.getPrescriptionId() == null ? null : event.getPrescriptionId().toString();

        if (prescriptionId != null
                && labOrderRepository.existsByExternalReferenceAndTestCode(prescriptionId, item.getTestCode())) {
            log.info("Lab order already exists for prescriptionId={} testCode={}, skipping (idempotent replay)",
                    prescriptionId, item.getTestCode());
            return;
        }

        LabOrderRequest request = new LabOrderRequest();
        request.setPatientId(event.getPatientId() == null ? null : event.getPatientId().toString());
        request.setOrderingPhysicianId(event.getDoctorId() == null ? null : event.getDoctorId().toString());
        request.setMrn(UNKNOWN);
        request.setOrderingProviderName(UNKNOWN);
        request.setOrderTimestamp(event.getOccurredAt() != null
                ? event.getOccurredAt().atOffset(ZoneOffset.UTC)
                : OffsetDateTime.now());
        request.setPriority(mapUrgency(item.getUrgency()));
        request.setExternalReference(prescriptionId);

        TestInfoDto testInfo = new TestInfoDto();
        testInfo.setTestCode(item.getTestCode());
        testInfo.setTestName(item.getTestName());
        testInfo.setClinicalNotes(item.getClinicalIndication());
        request.setTests(Collections.singletonList(testInfo));

        request.setDiagnosisCodes(Collections.emptyList());

        log.info("Creating lab order from prescription event: prescriptionId={}, patientId={}, testCode={}, priority={}",
                prescriptionId, request.getPatientId(), item.getTestCode(), request.getPriority());

        createLabOrderUseCase.createLabOrder(request);
    }

    /**
     * Maps the free-form urgency string from the event to the domain Priority enum leniently;
     * anything unknown falls back to ROUTINE.
     */
    private Priority mapUrgency(String urgency) {
        if (urgency == null || urgency.isBlank()) {
            return Priority.ROUTINE;
        }

        try {
            return Priority.valueOf(urgency.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            log.warn("Unknown urgency '{}', defaulting to ROUTINE", urgency);
            return Priority.ROUTINE;
        }
    }
}
