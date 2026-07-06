package com.meditrack.labrotary_service.infrastructure.messaging.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.meditrack.labrotary_service.application.dto.LabOrderRequest;
import com.meditrack.labrotary_service.application.usecase.CreateLabOrderUseCase;
import com.meditrack.labrotary_service.domain.model.Priority;
import com.meditrack.labrotary_service.domain.repository.LabOrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.support.Acknowledgment;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PrescriptionSentToLabConsumerTest {

    @Mock
    private CreateLabOrderUseCase createLabOrderUseCase;

    @Mock
    private LabOrderRepository labOrderRepository;

    @Mock
    private Acknowledgment acknowledgment;

    private PrescriptionSentToLabConsumer consumer;

    private final UUID prescriptionId = UUID.randomUUID();
    private final UUID patientId = UUID.randomUUID();
    private final UUID doctorId = UUID.randomUUID();
    private final Instant occurredAt = Instant.parse("2026-07-06T10:15:30Z");

    @BeforeEach
    void setUp() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        consumer = new PrescriptionSentToLabConsumer(createLabOrderUseCase, labOrderRepository, objectMapper);
    }

    private String eventJson(String urgency) {
        return """
                {
                  "prescriptionId": "%s",
                  "patientId": "%s",
                  "doctorId": "%s",
                  "labOrders": [
                    {
                      "testCode": "CBC",
                      "testName": "Complete Blood Count",
                      "clinicalIndication": "Anemia workup",
                      "urgency": "%s"
                    },
                    {
                      "testCode": "HBA1C",
                      "testName": "Hemoglobin A1c",
                      "clinicalIndication": "Diabetes monitoring",
                      "urgency": "STAT"
                    }
                  ],
                  "occurredAt": "%s"
                }
                """.formatted(prescriptionId, patientId, doctorId, urgency, occurredAt);
    }

    @Test
    void createsOneLabOrderPerLabItemWithMappedFields() {
        when(labOrderRepository.existsByExternalReferenceAndTestCode(anyString(), anyString())).thenReturn(false);

        consumer.consumePrescriptionSentToLabEvent(eventJson("URGENT"), acknowledgment);

        ArgumentCaptor<LabOrderRequest> captor = ArgumentCaptor.forClass(LabOrderRequest.class);
        verify(createLabOrderUseCase, times(2)).createLabOrder(captor.capture());

        LabOrderRequest first = captor.getAllValues().get(0);
        assertThat(first.getPatientId()).isEqualTo(patientId.toString());
        assertThat(first.getOrderingPhysicianId()).isEqualTo(doctorId.toString());
        assertThat(first.getExternalReference()).isEqualTo(prescriptionId.toString());
        assertThat(first.getPriority()).isEqualTo(Priority.URGENT);
        assertThat(first.getOrderTimestamp().toInstant()).isEqualTo(occurredAt);
        assertThat(first.getTests()).hasSize(1);
        assertThat(first.getTests().get(0).getTestCode()).isEqualTo("CBC");
        assertThat(first.getTests().get(0).getTestName()).isEqualTo("Complete Blood Count");
        assertThat(first.getTests().get(0).getClinicalNotes()).isEqualTo("Anemia workup");

        LabOrderRequest second = captor.getAllValues().get(1);
        assertThat(second.getTests().get(0).getTestCode()).isEqualTo("HBA1C");
        assertThat(second.getPriority()).isEqualTo(Priority.STAT);

        verify(acknowledgment).acknowledge();
    }

    @Test
    void mapsUnknownUrgencyToRoutine() {
        when(labOrderRepository.existsByExternalReferenceAndTestCode(anyString(), anyString())).thenReturn(false);

        consumer.consumePrescriptionSentToLabEvent(eventJson("SUPER_FAST"), acknowledgment);

        ArgumentCaptor<LabOrderRequest> captor = ArgumentCaptor.forClass(LabOrderRequest.class);
        verify(createLabOrderUseCase, times(2)).createLabOrder(captor.capture());

        assertThat(captor.getAllValues().get(0).getPriority()).isEqualTo(Priority.ROUTINE);
    }

    @Test
    void skipsItemsThatWereAlreadyProcessed() {
        // The CBC order already exists for this prescription; only HBA1C should be created
        when(labOrderRepository.existsByExternalReferenceAndTestCode(prescriptionId.toString(), "CBC"))
                .thenReturn(true);
        when(labOrderRepository.existsByExternalReferenceAndTestCode(prescriptionId.toString(), "HBA1C"))
                .thenReturn(false);

        consumer.consumePrescriptionSentToLabEvent(eventJson("ROUTINE"), acknowledgment);

        ArgumentCaptor<LabOrderRequest> captor = ArgumentCaptor.forClass(LabOrderRequest.class);
        verify(createLabOrderUseCase, times(1)).createLabOrder(captor.capture());
        assertThat(captor.getValue().getTests().get(0).getTestCode()).isEqualTo("HBA1C");

        verify(acknowledgment).acknowledge();
    }

    @Test
    void fullyDuplicateEventIsAcknowledgedWithoutCreatingOrders() {
        when(labOrderRepository.existsByExternalReferenceAndTestCode(eq(prescriptionId.toString()), anyString()))
                .thenReturn(true);

        consumer.consumePrescriptionSentToLabEvent(eventJson("ROUTINE"), acknowledgment);

        verify(createLabOrderUseCase, never()).createLabOrder(any());
        verify(acknowledgment).acknowledge();
    }

    @Test
    void invalidPayloadIsNotAcknowledgedAndPropagates() {
        assertThatThrownBy(() -> consumer.consumePrescriptionSentToLabEvent("{not-json", acknowledgment))
                .isInstanceOf(RuntimeException.class);

        verify(acknowledgment, never()).acknowledge();
        verify(createLabOrderUseCase, never()).createLabOrder(any());
    }
}
