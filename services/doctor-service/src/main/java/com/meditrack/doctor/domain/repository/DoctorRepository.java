package com.meditrack.doctor.domain.repository;

import com.meditrack.doctor.domain.model.Doctor;
import com.meditrack.doctor.domain.model.Specialization;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DoctorRepository {
    Doctor save(Doctor doctor);
    Optional<Doctor> findById(UUID id);
    Optional<Doctor> findByEmail(String email);
    Optional<Doctor> findByEmployeeId(String employeeId);
    List<Doctor> findAll();
    List<Doctor> findBySpecialization(Specialization specialization);
    List<Doctor> findAllActive();
    boolean existsByEmail(String email);
    boolean existsByEmployeeId(String employeeId);
}
