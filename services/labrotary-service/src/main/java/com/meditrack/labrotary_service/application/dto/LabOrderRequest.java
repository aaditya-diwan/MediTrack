package com.meditrack.labrotary_service.application.dto;

import com.meditrack.labrotary_service.domain.model.Priority;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.OffsetDateTime;
import java.util.List;

@Data
public class LabOrderRequest {

    @NotBlank(message = "Patient ID is required")
    private String patientId;

    @NotBlank(message = "Facility ID is required")
    private String facilityId;

    @NotBlank(message = "Ordering physician ID is required")
    private String orderingPhysicianId;

    private String preAuthorizationId;

    private OffsetDateTime orderTimestamp;

    @NotNull(message = "Priority is required")
    private Priority priority;

    private List<@Valid DiagnosisCodeDto> diagnosisCodes;

    @NotEmpty(message = "At least one test is required")
    private List<@Valid TestInfoDto> tests;
}
