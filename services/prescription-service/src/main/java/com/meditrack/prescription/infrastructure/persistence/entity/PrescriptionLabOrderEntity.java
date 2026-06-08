package com.meditrack.prescription.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.util.UUID;

@Data @Entity @Table(name = "prescription_lab_orders")
public class PrescriptionLabOrderEntity {
    @Id private UUID id;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "prescription_id", nullable = false)
    private PrescriptionEntity prescription;
    @Column(nullable = false) private String testCode;
    @Column(nullable = false) private String testName;
    @Column(columnDefinition = "TEXT") private String clinicalIndication;
    private String urgency;
}
