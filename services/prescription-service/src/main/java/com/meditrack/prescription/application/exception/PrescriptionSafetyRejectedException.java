package com.meditrack.prescription.application.exception;

import com.meditrack.prescription.domain.port.SafetyScreenResult;
import lombok.Getter;

/**
 * Thrown when the AI drug-safety screen reports a blocking severity
 * (MAJOR / CONTRAINDICATED) and the caller did not request an override.
 * Mapped to HTTP 409 with the structured findings in the body.
 */
@Getter
public class PrescriptionSafetyRejectedException extends RuntimeException {

    private final transient SafetyScreenResult screenResult;

    public PrescriptionSafetyRejectedException(SafetyScreenResult screenResult) {
        super("Prescription issuance blocked by drug-safety screen (severity: "
                + screenResult.highestSeverity() + ")");
        this.screenResult = screenResult;
    }
}
