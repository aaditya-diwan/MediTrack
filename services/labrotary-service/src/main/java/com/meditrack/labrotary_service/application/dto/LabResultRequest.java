package com.meditrack.labrotary_service.application.dto;

import com.meditrack.labrotary_service.domain.model.AbnormalFlag;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * DTO for submitting lab results
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LabResultRequest {

    @NotNull(message = "Order ID is required")
    private UUID orderId;

    @NotBlank(message = "Test code is required")
    private String testCode;

    @NotBlank(message = "Test name is required")
    private String testName;

    private String loincCode;

    @NotBlank(message = "Result value is required")
    private String resultValue;

    private String resultUnit;

    private String referenceRange;

    private AbnormalFlag abnormalFlag;

    @NotBlank(message = "Performer identification is required")
    private String performedBy;

    private OffsetDateTime performedAt;

    private String notes;
}
