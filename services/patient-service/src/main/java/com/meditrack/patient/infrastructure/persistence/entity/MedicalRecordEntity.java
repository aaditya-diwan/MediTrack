package com.meditrack.patient.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Maps to the comprehensive {@code medical_records} schema (Flyway V2): PK column
 * {@code record_id}, and the simplified domain fields map onto V2 columns
 * (diagnosis → {@code chief_complaint}, treatment → {@code notes}, date →
 * {@code record_date}).
 *
 * <p>V2 requires {@code record_type} and {@code provider_id} (NOT NULL) which the
 * simplified domain model does not carry, so safe placeholders are persisted to
 * satisfy the schema until the richer record model is wired.
 */
@Data
@Entity
@Table(name = "medical_records")
public class MedicalRecordEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "record_id")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_id", nullable = false)
    private PatientEntity patient;

    @Column(name = "record_type", nullable = false)
    private String recordType = "VISIT";

    @Column(name = "provider_id", nullable = false)
    private String providerId = "UNKNOWN";

    @Column(name = "chief_complaint")
    private String diagnosis;

    @Column(name = "notes")
    private String treatment;

    @Column(name = "record_date")
    private LocalDate date;
}
