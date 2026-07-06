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

    @NotBlank(message = "MRN is required")
    private String mrn;

    @NotBlank(message = "Facility ID is required")
    private String facilityId;

    @NotBlank(message = "Ordering physician ID is required")
    private String orderingPhysicianId;

    @NotBlank(message = "Ordering provider name is required")
    private String orderingProviderName;

    private String preAuthorizationId;

    /** Optional reference to the originating record in another service (e.g. prescriptionId). */
    private String externalReference;

    private OffsetDateTime orderTimestamp;

    @NotNull(message = "Priority is required")
    private Priority priority;

    private List<@Valid DiagnosisCodeDto> diagnosisCodes;

    @NotEmpty(message = "At least one test is required")
    private List<@Valid TestInfoDto> tests;
}
