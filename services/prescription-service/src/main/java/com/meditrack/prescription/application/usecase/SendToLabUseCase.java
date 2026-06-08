package com.meditrack.prescription.application.usecase;

import com.meditrack.prescription.interfaces.dto.response.PrescriptionResponse;

import java.util.UUID;

public interface SendToLabUseCase {
    PrescriptionResponse sendToLab(UUID prescriptionId);
}
