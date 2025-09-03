package com.meditrack.labrotary_service.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LabOrder {
    private UUID id;
    private String patientId;
    private String facilityId;
    private String orderingPhysicianId;
    private String preAuthorizationId;
    private OffsetDateTime orderTimestamp;
    private Priority priority;
    private List<DiagnosisCode> diagnosisCodes;
    private List<TestInfo> tests;
    private OrderStatus status;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    public void initialize() {
        this.id = UUID.randomUUID();
        this.status = OrderStatus.RECEIVED;
        this.createdAt = OffsetDateTime.now();
        this.updatedAt = OffsetDateTime.now();
    }
}
