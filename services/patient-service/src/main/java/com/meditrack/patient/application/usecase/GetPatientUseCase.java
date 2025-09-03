package com.meditrack.patient.application.usecase;

import com.meditrack.patient.interfaces.dto.response.PatientResponse;
import java.util.UUID;

public interface GetPatientUseCase {
    PatientResponse getPatientById(UUID id);
}
