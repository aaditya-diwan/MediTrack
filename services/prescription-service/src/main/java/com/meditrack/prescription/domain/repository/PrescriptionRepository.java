package com.meditrack.prescription.domain.repository;

import com.meditrack.prescription.domain.model.Prescription;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PrescriptionRepository {
    Prescription save(Prescription prescription);
    Optional<Prescription> findById(UUID id);
    List<Prescription> findByPatientId(UUID patientId);
    List<Prescription> findByDoctorId(UUID doctorId);
    List<Prescription> findByAppointmentId(UUID appointmentId);
}
