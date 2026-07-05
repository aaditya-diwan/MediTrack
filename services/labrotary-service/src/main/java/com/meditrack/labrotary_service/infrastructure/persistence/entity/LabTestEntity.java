package com.meditrack.labrotary_service.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.util.UUID;

/**
 * A test line on a lab order — maps to the {@code lab_tests} child table.
 * {@code test_code}, {@code test_name} and {@code status} are NOT NULL; status
 * defaults to PENDING at creation.
 */
@Data
@Entity
@Table(name = "lab_tests")
public class LabTestEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "test_id")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private LabOrderEntity order;

    @Column(name = "test_code", nullable = false)
    private String testCode;

    @Column(name = "test_name", nullable = false)
    private String testName;

    @Column(name = "status", nullable = false)
    private String status = "PENDING";
}
