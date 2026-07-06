package com.meditrack.prescription.domain.model;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
public class Prescription {
    private UUID id;
    private UUID patientId;
    private UUID doctorId;
    private UUID appointmentId;
    private PrescriptionStatus status;
    private String consultationNotes;
    private String diagnosisCodes;
    private List<PrescriptionMedication> medications;
    private List<PrescriptionLabOrder> labOrders;
    private LocalDateTime issuedAt;
    private LocalDate validUntil;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // AI drug-safety screen outcome captured at issue time
    private boolean safetyCheckPerformed;
    private String safetySeverity;
    private String safetySummary;
    private boolean safetyOverridden;
    private String safetyOverrideReason;
}
