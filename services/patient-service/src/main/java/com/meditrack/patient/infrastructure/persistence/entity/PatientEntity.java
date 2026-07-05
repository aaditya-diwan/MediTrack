package com.meditrack.patient.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Maps to the comprehensive {@code patients} schema (Flyway V2). The primary key
 * column is {@code patient_id}; phone maps to {@code phone}.
 *
 * <p>Address and insurance have their own tables in V2 ({@code patient_addresses},
 * {@code patient_insurance}) and are not columns on {@code patients}, so those
 * fields are {@link Transient} here — kept on the entity for in-memory mapping but
 * not persisted. Wiring them to their own tables is a separate enhancement.
 */
@Data
@Entity
@Table(name = "patients")
public class PatientEntity {
    @Id
    @Column(name = "patient_id")
    private UUID id;

    @Column(unique = true)
    private String mrn;

    private String ssn;

    private String firstName;
    private String lastName;
    private LocalDate dateOfBirth;

    @Embedded
    private ContactInfoEntity contactInfo;

    @Transient
    private String insuranceProvider;

    @Transient
    private String insurancePolicyNumber;

    @OneToMany(mappedBy = "patient", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<MedicalRecordEntity> medicalHistory;
}
