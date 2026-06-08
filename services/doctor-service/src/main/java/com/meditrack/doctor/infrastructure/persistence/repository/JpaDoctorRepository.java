package com.meditrack.doctor.infrastructure.persistence.repository;

import com.meditrack.doctor.infrastructure.persistence.entity.DoctorEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface JpaDoctorRepository extends JpaRepository<DoctorEntity, UUID> {
    Optional<DoctorEntity> findByEmail(String email);
    Optional<DoctorEntity> findByEmployeeId(String employeeId);
    List<DoctorEntity> findBySpecialization(String specialization);
    List<DoctorEntity> findByActiveTrue();
    boolean existsByEmail(String email);
    boolean existsByEmployeeId(String employeeId);
}
