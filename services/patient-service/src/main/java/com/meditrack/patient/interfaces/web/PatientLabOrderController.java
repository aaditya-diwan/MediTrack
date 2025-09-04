package com.meditrack.patient.interfaces.web;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.meditrack.patient.domain.model.Patient;
import com.meditrack.patient.domain.model.valueobjects.SSN;
import com.meditrack.patient.domain.repository.PatientRepository;
import com.meditrack.patient.events.LabTestOrderedEvent;
import com.meditrack.patient.interfaces.dtos.OrderLabTestRequest;
import com.meditrack.patient.messaging.LabOrderEventPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.ZoneOffset;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/patients/{ssn}/order-labs")
@RequiredArgsConstructor
public class PatientLabOrderController {

    private final PatientRepository patientRepository;
    private final LabOrderEventPublisher labOrderEventPublisher;

    @PostMapping
    public ResponseEntity<String> orderLabTest(@PathVariable String ssn, @RequestBody OrderLabTestRequest request) throws JsonProcessingException {
        Patient patient = patientRepository.findBySsn(new SSN(ssn))
                .orElseThrow(() -> new RuntimeException("Patient not found"));

        UUID orderId = UUID.randomUUID();

        LabTestOrderedEvent event = LabTestOrderedEvent.builder()
                .eventId(UUID.randomUUID())
                .eventType("lab.test.ordered.v1")
                .timestamp(System.currentTimeMillis())
                .source("patient-service")
                .order(LabTestOrderedEvent.Order.builder()
                        .orderId(orderId)
                        .patientId(patient.getId().toString())
                        .doctorId(request.getDoctorId())
                        .testCode(request.getTestCode())
                        .priority(request.getPriority())
                        .notes(request.getNotes())
                        .build())
                .patientSnapshot(LabTestOrderedEvent.PatientSnapshot.builder()
                        .mrn(patient.getMrn().toString())
                        .firstName(patient.getFirstName())
                        .lastName(patient.getLastName())
                        .dateOfBirth(patient.getDateOfBirth().toString())
                        .build())
                .build();

        labOrderEventPublisher.publishLabTestOrder(event);

        return ResponseEntity.ok("Lab order event published successfully");
    }
}