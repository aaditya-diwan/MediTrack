package com.meditrack.prescription.interfaces.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Optional body for POST /api/v1/prescriptions/{id}/issue. Omitting the body
 * entirely is equivalent to {@code override=false} (backward compatible).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IssuePrescriptionRequest {

    /** When true, issue the prescription despite a blocking safety finding. */
    private Boolean override;

    /** Clinical justification for overriding a blocking safety finding. */
    private String overrideReason;
}
