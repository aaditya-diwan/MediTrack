package com.meditrack.patient.infrastructure.persistence.repository;

import com.meditrack.patient.infrastructure.persistence.entity.PatientEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PatientJpaRepository extends JpaRepository<PatientEntity, UUID> {
    Optional<PatientEntity> findByMrn(String mrn);
    List<PatientEntity> findByFirstNameContainingIgnoreCase(String firstName);
    List<PatientEntity> findByLastNameContainingIgnoreCase(String lastName);
    Optional<PatientEntity> findBySsn(String ssn);
}
