package com.meditrack.patient.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Data
@Entity
@Table(name = "patients")
public class PatientEntity {
    @Id
    private UUID id;

    @Column(unique = true)
    private String mrn;

    @Column(unique = true)
    private String ssn;

    private String firstName;
    private String lastName;
    private LocalDate dateOfBirth;

    @Embedded
    private ContactInfoEntity contactInfo;

    private String insuranceProvider;
    private String insurancePolicyNumber;

    @OneToMany(mappedBy = "patient", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<MedicalRecordEntity> medicalHistory;
}
