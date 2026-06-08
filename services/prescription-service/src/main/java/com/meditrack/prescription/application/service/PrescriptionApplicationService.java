package com.meditrack.prescription.application.service;

import com.meditrack.prescription.application.exception.PrescriptionNotFoundException;
import com.meditrack.prescription.application.exception.PrescriptionNotIssuedException;
import com.meditrack.prescription.application.usecase.*;
import com.meditrack.prescription.domain.model.*;
import com.meditrack.prescription.domain.repository.PrescriptionRepository;
import com.meditrack.prescription.infrastructure.messaging.PrescriptionEventProducer;
import com.meditrack.prescription.infrastructure.messaging.event.*;
import com.meditrack.prescription.interfaces.dto.request.CreatePrescriptionRequest;
import com.meditrack.prescription.interfaces.dto.response.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PrescriptionApplicationService implements CreatePrescriptionUseCase, GetPrescriptionUseCase,
        IssuePrescriptionUseCase, SendToPharmacyUseCase, SendToLabUseCase {

    private final PrescriptionRepository prescriptionRepository;
    private final PrescriptionEventProducer eventProducer;

    @Override
    @Transactional
    public PrescriptionResponse createPrescription(CreatePrescriptionRequest request) {
        List<PrescriptionMedication> meds = request.getMedications() == null ? Collections.emptyList() :
                request.getMedications().stream().map(m -> PrescriptionMedication.builder()
                        .id(UUID.randomUUID()).medicationName(m.getMedicationName()).genericName(m.getGenericName())
                        .dosage(m.getDosage()).frequency(m.getFrequency()).duration(m.getDuration())
                        .route(m.getRoute()).instructions(m.getInstructions()).build()).collect(Collectors.toList());

        List<PrescriptionLabOrder> labs = request.getLabOrders() == null ? Collections.emptyList() :
                request.getLabOrders().stream().map(l -> PrescriptionLabOrder.builder()
                        .id(UUID.randomUUID()).testCode(l.getTestCode()).testName(l.getTestName())
                        .clinicalIndication(l.getClinicalIndication())
                        .urgency(l.getUrgency() != null ? LabTestUrgency.valueOf(l.getUrgency()) : LabTestUrgency.ROUTINE)
                        .build()).collect(Collectors.toList());

        Prescription prescription = Prescription.builder()
                .id(UUID.randomUUID()).patientId(request.getPatientId()).doctorId(request.getDoctorId())
                .appointmentId(request.getAppointmentId()).status(PrescriptionStatus.DRAFT)
                .consultationNotes(request.getConsultationNotes()).diagnosisCodes(request.getDiagnosisCodes())
                .medications(meds).labOrders(labs).build();

        return toResponse(prescriptionRepository.save(prescription));
    }

    @Override
    public PrescriptionResponse getPrescriptionById(UUID id) {
        return prescriptionRepository.findById(id).map(this::toResponse)
                .orElseThrow(() -> new PrescriptionNotFoundException(id));
    }

    @Override
    public List<PrescriptionResponse> getPatientPrescriptions(UUID patientId) {
        return prescriptionRepository.findByPatientId(patientId).stream()
                .map(this::toResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public PrescriptionResponse issuePrescription(UUID id) {
        Prescription p = prescriptionRepository.findById(id)
                .orElseThrow(() -> new PrescriptionNotFoundException(id));
        p.setStatus(PrescriptionStatus.ISSUED);
        p.setIssuedAt(LocalDateTime.now());
        p.setValidUntil(LocalDate.now().plusDays(30));
        Prescription saved = prescriptionRepository.save(p);
        eventProducer.publishIssued(PrescriptionIssuedEvent.builder()
                .prescriptionId(saved.getId()).patientId(saved.getPatientId())
                .doctorId(saved.getDoctorId()).appointmentId(saved.getAppointmentId())
                .occurredAt(Instant.now()).build());
        return toResponse(saved);
    }

    @Override
    @Transactional
    public PrescriptionResponse sendToPharmacy(UUID id) {
        Prescription p = prescriptionRepository.findById(id)
                .orElseThrow(() -> new PrescriptionNotFoundException(id));
        if (p.getStatus() == PrescriptionStatus.DRAFT) {
            throw new PrescriptionNotIssuedException("Prescription must be issued before sending to pharmacy.");
        }
        p.setStatus(PrescriptionStatus.SENT_TO_PHARMACY);
        Prescription saved = prescriptionRepository.save(p);
        List<PrescriptionSentToPharmacyEvent.MedicationItem> items = saved.getMedications().stream()
                .map(m -> PrescriptionSentToPharmacyEvent.MedicationItem.builder()
                        .medicationName(m.getMedicationName()).dosage(m.getDosage())
                        .frequency(m.getFrequency()).duration(m.getDuration()).build())
                .collect(Collectors.toList());
        eventProducer.publishSentToPharmacy(PrescriptionSentToPharmacyEvent.builder()
                .prescriptionId(saved.getId()).patientId(saved.getPatientId())
                .medications(items).occurredAt(Instant.now()).build());
        return toResponse(saved);
    }

    @Override
    @Transactional
    public PrescriptionResponse sendToLab(UUID id) {
        Prescription p = prescriptionRepository.findById(id)
                .orElseThrow(() -> new PrescriptionNotFoundException(id));
        if (p.getStatus() == PrescriptionStatus.DRAFT) {
            throw new PrescriptionNotIssuedException("Prescription must be issued before sending to lab.");
        }
        if (p.getLabOrders() == null || p.getLabOrders().isEmpty()) {
            throw new PrescriptionNotIssuedException("Prescription has no lab orders.");
        }
        p.setStatus(PrescriptionStatus.SENT_TO_LAB);
        Prescription saved = prescriptionRepository.save(p);
        List<PrescriptionSentToLabEvent.LabItem> labs = saved.getLabOrders().stream()
                .map(l -> PrescriptionSentToLabEvent.LabItem.builder()
                        .testCode(l.getTestCode()).testName(l.getTestName())
                        .clinicalIndication(l.getClinicalIndication())
                        .urgency(l.getUrgency() != null ? l.getUrgency().name() : "ROUTINE").build())
                .collect(Collectors.toList());
        eventProducer.publishSentToLab(PrescriptionSentToLabEvent.builder()
                .prescriptionId(saved.getId()).patientId(saved.getPatientId()).doctorId(saved.getDoctorId())
                .labOrders(labs).occurredAt(Instant.now()).build());
        return toResponse(saved);
    }

    private PrescriptionResponse toResponse(Prescription p) {
        List<PrescriptionMedicationResponse> meds = p.getMedications() == null ? Collections.emptyList() :
                p.getMedications().stream().map(m -> PrescriptionMedicationResponse.builder()
                        .id(m.getId()).medicationName(m.getMedicationName()).genericName(m.getGenericName())
                        .dosage(m.getDosage()).frequency(m.getFrequency()).duration(m.getDuration())
                        .route(m.getRoute()).instructions(m.getInstructions()).build()).collect(Collectors.toList());
        List<PrescriptionLabOrderResponse> labs = p.getLabOrders() == null ? Collections.emptyList() :
                p.getLabOrders().stream().map(l -> PrescriptionLabOrderResponse.builder()
                        .id(l.getId()).testCode(l.getTestCode()).testName(l.getTestName())
                        .clinicalIndication(l.getClinicalIndication())
                        .urgency(l.getUrgency() != null ? l.getUrgency().name() : "ROUTINE").build())
                        .collect(Collectors.toList());
        return PrescriptionResponse.builder().id(p.getId()).patientId(p.getPatientId())
                .doctorId(p.getDoctorId()).appointmentId(p.getAppointmentId())
                .status(p.getStatus().name()).consultationNotes(p.getConsultationNotes())
                .diagnosisCodes(p.getDiagnosisCodes()).medications(meds).labOrders(labs)
                .issuedAt(p.getIssuedAt()).validUntil(p.getValidUntil()).createdAt(p.getCreatedAt()).build();
    }
}
