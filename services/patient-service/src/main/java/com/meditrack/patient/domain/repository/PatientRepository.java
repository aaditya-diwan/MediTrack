package com.meditrack.patient.domain.repository;

import com.meditrack.patient.domain.model.Patient;
import com.meditrack.patient.domain.model.valueobjects.MRN;
import com.meditrack.patient.domain.model.valueobjects.PatientId;
import com.meditrack.patient.domain.model.valueobjects.SSN;

import java.util.List;
import java.util.Optional;

public interface PatientRepository {
    Patient save(Patient patient);
    Optional<Patient> findById(PatientId patientId);
    void deleteById(PatientId patientId);
    Optional<Patient> findByMrn(MRN mrn);
    List<Patient> findByFirstName(String firstName);
    List<Patient> findByLastName(String lastName);
    Optional<Patient> findBySsn(SSN ssn);
}
