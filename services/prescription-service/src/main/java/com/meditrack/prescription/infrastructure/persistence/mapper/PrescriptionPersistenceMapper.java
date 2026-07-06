package com.meditrack.prescription.infrastructure.persistence.mapper;

import com.meditrack.prescription.domain.model.*;
import com.meditrack.prescription.infrastructure.persistence.entity.*;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class PrescriptionPersistenceMapper {

    public PrescriptionEntity toEntity(Prescription p) {
        PrescriptionEntity e = new PrescriptionEntity();
        e.setId(p.getId());
        e.setPatientId(p.getPatientId());
        e.setDoctorId(p.getDoctorId());
        e.setAppointmentId(p.getAppointmentId());
        e.setStatus(p.getStatus().name());
        e.setConsultationNotes(p.getConsultationNotes());
        e.setDiagnosisCodes(p.getDiagnosisCodes());
        e.setIssuedAt(p.getIssuedAt());
        e.setValidUntil(p.getValidUntil());
        e.setSafetyCheckPerformed(p.isSafetyCheckPerformed());
        e.setSafetySeverity(p.getSafetySeverity());
        e.setSafetySummary(p.getSafetySummary());
        e.setSafetyOverridden(p.isSafetyOverridden());
        e.setSafetyOverrideReason(p.getSafetyOverrideReason());

        if (p.getMedications() != null) {
            List<PrescriptionMedicationEntity> meds = p.getMedications().stream().map(m -> {
                PrescriptionMedicationEntity me = new PrescriptionMedicationEntity();
                me.setId(m.getId() != null ? m.getId() : UUID.randomUUID());
                me.setPrescription(e);
                me.setMedicationName(m.getMedicationName());
                me.setGenericName(m.getGenericName());
                me.setDosage(m.getDosage());
                me.setFrequency(m.getFrequency());
                me.setDuration(m.getDuration());
                me.setRoute(m.getRoute());
                me.setInstructions(m.getInstructions());
                return me;
            }).collect(Collectors.toList());
            e.setMedications(meds);
        }

        if (p.getLabOrders() != null) {
            List<PrescriptionLabOrderEntity> labs = p.getLabOrders().stream().map(l -> {
                PrescriptionLabOrderEntity le = new PrescriptionLabOrderEntity();
                le.setId(l.getId() != null ? l.getId() : UUID.randomUUID());
                le.setPrescription(e);
                le.setTestCode(l.getTestCode());
                le.setTestName(l.getTestName());
                le.setClinicalIndication(l.getClinicalIndication());
                le.setUrgency(l.getUrgency() != null ? l.getUrgency().name() : "ROUTINE");
                return le;
            }).collect(Collectors.toList());
            e.setLabOrders(labs);
        }

        return e;
    }

    public Prescription toDomain(PrescriptionEntity e) {
        List<PrescriptionMedication> meds = e.getMedications() == null ? Collections.emptyList() :
                e.getMedications().stream().map(m -> PrescriptionMedication.builder()
                        .id(m.getId()).medicationName(m.getMedicationName()).genericName(m.getGenericName())
                        .dosage(m.getDosage()).frequency(m.getFrequency()).duration(m.getDuration())
                        .route(m.getRoute()).instructions(m.getInstructions()).build())
                        .collect(Collectors.toList());

        List<PrescriptionLabOrder> labs = e.getLabOrders() == null ? Collections.emptyList() :
                e.getLabOrders().stream().map(l -> PrescriptionLabOrder.builder()
                        .id(l.getId()).testCode(l.getTestCode()).testName(l.getTestName())
                        .clinicalIndication(l.getClinicalIndication())
                        .urgency(l.getUrgency() != null ? LabTestUrgency.valueOf(l.getUrgency()) : LabTestUrgency.ROUTINE)
                        .build()).collect(Collectors.toList());

        return Prescription.builder()
                .id(e.getId()).patientId(e.getPatientId()).doctorId(e.getDoctorId())
                .appointmentId(e.getAppointmentId()).status(PrescriptionStatus.valueOf(e.getStatus()))
                .consultationNotes(e.getConsultationNotes()).diagnosisCodes(e.getDiagnosisCodes())
                .medications(meds).labOrders(labs).issuedAt(e.getIssuedAt()).validUntil(e.getValidUntil())
                .createdAt(e.getCreatedAt()).updatedAt(e.getUpdatedAt())
                .safetyCheckPerformed(e.isSafetyCheckPerformed()).safetySeverity(e.getSafetySeverity())
                .safetySummary(e.getSafetySummary()).safetyOverridden(e.isSafetyOverridden())
                .safetyOverrideReason(e.getSafetyOverrideReason()).build();
    }
}
