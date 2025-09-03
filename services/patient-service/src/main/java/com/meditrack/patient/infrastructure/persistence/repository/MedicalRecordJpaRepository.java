package com.meditrack.patient.infrastructure.persistence.repository;

import com.meditrack.patient.infrastructure.persistence.entity.MedicalRecordEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MedicalRecordJpaRepository extends JpaRepository<MedicalRecordEntity, UUID> {
    List<MedicalRecordEntity> findByPatientId(UUID patientId);
    Optional<MedicalRecordEntity> findById(UUID id);
    void deleteById(UUID id);
}
