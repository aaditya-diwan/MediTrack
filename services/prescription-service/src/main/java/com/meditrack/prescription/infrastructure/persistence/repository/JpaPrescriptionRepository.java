package com.meditrack.prescription.infrastructure.persistence.repository;

import com.meditrack.prescription.infrastructure.persistence.entity.PrescriptionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface JpaPrescriptionRepository extends JpaRepository<PrescriptionEntity, UUID> {
    List<PrescriptionEntity> findByPatientId(UUID patientId);
    List<PrescriptionEntity> findByDoctorId(UUID doctorId);
    List<PrescriptionEntity> findByAppointmentId(UUID appointmentId);
}
