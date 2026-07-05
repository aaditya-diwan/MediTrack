package com.meditrack.labrotary_service.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.util.UUID;

/**
 * A diagnosis code attached to a lab order — maps to the {@code order_diagnoses}
 * child table. {@code code} and {@code code_system} are NOT NULL; code_system
 * defaults to ICD-10.
 */
@Data
@Entity
@Table(name = "order_diagnoses")
public class OrderDiagnosisEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "diagnosis_id")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private LabOrderEntity order;

    @Column(name = "code", nullable = false)
    private String code;

    @Column(name = "code_system", nullable = false)
    private String codeSystem = "ICD-10";

    @Column(name = "description")
    private String description;
}
