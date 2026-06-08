package com.meditrack.prescription.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.util.UUID;

@Data @Entity @Table(name = "prescription_medications")
public class PrescriptionMedicationEntity {
    @Id private UUID id;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "prescription_id", nullable = false)
    private PrescriptionEntity prescription;
    @Column(nullable = false) private String medicationName;
    private String genericName;
    @Column(nullable = false) private String dosage;
    @Column(nullable = false) private String frequency;
    private String duration;
    private String route;
    @Column(columnDefinition = "TEXT") private String instructions;
}
