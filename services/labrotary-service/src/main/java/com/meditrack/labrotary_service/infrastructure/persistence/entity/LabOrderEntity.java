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

@Data
@Entity
@Table(name = "lab_orders")
public class LabOrderEntity {

    @Id
    private UUID id;

    private String patientId;

    private String facilityId;

    private String orderingPhysicianId;

    private String preAuthorizationId;

    private OffsetDateTime orderTimestamp;

    @Enumerated(EnumType.STRING)
    private Priority priority;

    @ElementCollection
    @CollectionTable(name = "lab_order_diagnosis_codes", joinColumns = @JoinColumn(name = "order_id"))
    private List<DiagnosisCodeEntity> diagnosisCodes;

    @ElementCollection
    @CollectionTable(name = "lab_order_tests", joinColumns = @JoinColumn(name = "order_id"))
    private List<TestInfoEntity> tests;

    @Enumerated(EnumType.STRING)
    private OrderStatus status;

    @CreationTimestamp
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    private OffsetDateTime updatedAt;
}
