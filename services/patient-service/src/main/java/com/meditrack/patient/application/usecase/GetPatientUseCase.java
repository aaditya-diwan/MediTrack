package com.meditrack.patient.application.usecase;

import com.meditrack.patient.domain.model.Patient;
import com.meditrack.patient.domain.model.valueobjects.SSN;
import com.meditrack.patient.interfaces.dto.response.PatientResponse;

import java.util.Optional;
import java.util.UUID;

public interface GetPatientUseCase {
    PatientResponse getPatientById(UUID id);
    Optional<Patient> getPatientBySSN(SSN ssn);
}
