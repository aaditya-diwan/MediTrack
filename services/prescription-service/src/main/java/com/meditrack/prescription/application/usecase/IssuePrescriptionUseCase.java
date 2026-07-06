package com.meditrack.prescription.application.usecase;

import com.meditrack.prescription.interfaces.dto.response.PrescriptionResponse;

import java.util.UUID;

public interface IssuePrescriptionUseCase {

    /**
     * Issues a prescription after screening it through the AI drug-safety
     * service. Blocking findings (MAJOR / CONTRAINDICATED) reject issuance
     * unless {@code override} is true; an unavailable safety service fails
     * open (the prescription is issued with safetyCheckPerformed=false).
     */
    PrescriptionResponse issuePrescription(UUID prescriptionId, boolean override, String overrideReason);

    default PrescriptionResponse issuePrescription(UUID prescriptionId) {
        return issuePrescription(prescriptionId, false, null);
    }
}
