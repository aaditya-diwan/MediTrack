package com.meditrack.labrotary_service.infrastructure.persistence.entity;

import com.meditrack.labrotary_service.domain.model.OrderStatus;
import com.meditrack.labrotary_service.domain.model.Priority;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Maps to the comprehensive {@code lab_orders} schema (Flyway V1): PK {@code order_id},
 * {@code patient_id} is a UUID, the ordering provider is {@code ordering_provider_id} +
 * {@code ordering_provider_name}, and the order time is {@code order_date}. Tests and
 * diagnoses are child tables ({@code lab_tests}, {@code order_diagnoses}), not element
 * collections.
 */
@Data
@Entity
@Table(name = "lab_orders")
public class LabOrderEntity {

    @Id
    @Column(name = "order_id")
    private UUID id;

    @Column(name = "patient_id", nullable = false)
    private UUID patientId;

    @Column(name = "mrn", nullable = false)
    private String mrn;

    @Column(name = "ordering_provider_id", nullable = false)
    private String orderingProviderId;

    @Column(name = "ordering_provider_name", nullable = false)
    private String orderingProviderName;

    @Column(name = "ordering_facility_id")
    private String orderingFacilityId;

    @Column(name = "order_date", nullable = false)
    private OffsetDateTime orderDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "priority", nullable = false)
    private Priority priority;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private OrderStatus status;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<LabTestEntity> tests;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderDiagnosisEntity> diagnoses;

    @CreationTimestamp
    @Column(name = "created_at")
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

    // The lab_order_audit trigger records NEW.updated_by as changed_by (NOT NULL),
    // so these must always carry a value. Defaulted until a real actor is threaded through.
    @Column(name = "created_by")
    private String createdBy = "system";

    @Column(name = "updated_by")
    private String updatedBy = "system";
}
