package com.meditrack.patient.application.usecase;

import com.meditrack.patient.interfaces.dto.request.UpdatePatientRequest;
import com.meditrack.patient.interfaces.dto.response.PatientResponse;

import java.util.UUID;

public interface UpdatePatientUseCase {
    PatientResponse updatePatient(UUID patientId, UpdatePatientRequest request);
}
