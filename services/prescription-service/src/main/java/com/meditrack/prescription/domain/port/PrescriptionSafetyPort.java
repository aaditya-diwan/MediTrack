package com.meditrack.prescription.domain.port;

import com.meditrack.prescription.domain.model.Prescription;

/**
 * Outbound port for screening a prescription against a drug-safety service
 * (interactions, allergy conflicts) before it is issued.
 *
 * <p>Implementations must NEVER throw for infrastructure failures — the issue
 * flow fails open. On any transport/auth error they return a result with
 * {@code checked == false} so the caller can proceed and record that no
 * screen was performed.
 */
public interface PrescriptionSafetyPort {

    SafetyScreenResult screen(Prescription prescription, PatientSafetyContext context);
}
