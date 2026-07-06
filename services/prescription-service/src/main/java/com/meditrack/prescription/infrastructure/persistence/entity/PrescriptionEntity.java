package com.meditrack.prescription.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Entity
@Table(name = "prescriptions")
public class PrescriptionEntity {
    @Id private UUID id;
    @Column(nullable = false) private UUID patientId;
    @Column(nullable = false) private UUID doctorId;
    private UUID appointmentId;
    @Column(nullable = false) private String status;
    @Column(columnDefinition = "TEXT") private String consultationNotes;
    @Column(columnDefinition = "TEXT") private String diagnosisCodes;
    private LocalDateTime issuedAt;
    private LocalDate validUntil;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Column(name = "safety_check_performed", nullable = false)
    private boolean safetyCheckPerformed;
    @Column(name = "safety_severity") private String safetySeverity;
    @Column(name = "safety_summary", columnDefinition = "TEXT") private String safetySummary;
    @Column(name = "safety_overridden", nullable = false)
    private boolean safetyOverridden;
    @Column(name = "safety_override_reason", columnDefinition = "TEXT") private String safetyOverrideReason;

    @OneToMany(mappedBy = "prescription", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PrescriptionMedicationEntity> medications;

    @OneToMany(mappedBy = "prescription", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PrescriptionLabOrderEntity> labOrders;

    @PrePersist void onCreate() { createdAt = LocalDateTime.now(); updatedAt = LocalDateTime.now(); }
    @PreUpdate void onUpdate() { updatedAt = LocalDateTime.now(); }
}
