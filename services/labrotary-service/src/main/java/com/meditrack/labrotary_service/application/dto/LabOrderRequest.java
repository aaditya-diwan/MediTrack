package com.meditrack.labrotary_service.application.dto;

import com.meditrack.labrotary_service.domain.model.Priority;
import lombok.Data;

import java.time.OffsetDateTime;
import java.util.List;

@Data
public class LabOrderRequest {
    private String patientId;
    private String facilityId;
    private String orderingPhysicianId;
    private String preAuthorizationId;
    private OffsetDateTime orderTimestamp;
    private Priority priority;
    private List<DiagnosisCodeDto> diagnosisCodes;
    private List<TestInfoDto> tests;
}
