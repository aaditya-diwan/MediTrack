package com.meditrack.prescription.application.usecase;

import com.meditrack.prescription.interfaces.dto.response.PrescriptionResponse;

import java.util.UUID;

public interface SendToPharmacyUseCase {
    PrescriptionResponse sendToPharmacy(UUID prescriptionId);
}
