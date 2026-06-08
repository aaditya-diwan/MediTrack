package com.meditrack.doctor.infrastructure.persistence.mapper;

import com.meditrack.doctor.domain.model.AvailabilitySlot;
import com.meditrack.doctor.domain.model.Doctor;
import com.meditrack.doctor.domain.model.Specialization;
import com.meditrack.doctor.infrastructure.persistence.entity.AvailabilitySlotEntity;
import com.meditrack.doctor.infrastructure.persistence.entity.DoctorEntity;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;

@Component
public class DoctorPersistenceMapper {

    public DoctorEntity toEntity(Doctor doctor) {
        DoctorEntity entity = new DoctorEntity();
        entity.setId(doctor.getId());
        entity.setEmployeeId(doctor.getEmployeeId());
        entity.setFirstName(doctor.getFirstName());
        entity.setLastName(doctor.getLastName());
        entity.setEmail(doctor.getEmail());
        entity.setPhone(doctor.getPhone());
        entity.setSpecialization(doctor.getSpecialization().name());
        entity.setQualifications(doctor.getQualifications());
        entity.setYearsOfExperience(doctor.getYearsOfExperience());
        entity.setBio(doctor.getBio());
        entity.setActive(doctor.isActive());
        return entity;
    }

    public Doctor toDomain(DoctorEntity entity) {
        return Doctor.builder()
                .id(entity.getId())
                .employeeId(entity.getEmployeeId())
                .firstName(entity.getFirstName())
                .lastName(entity.getLastName())
                .email(entity.getEmail())
                .phone(entity.getPhone())
                .specialization(Specialization.valueOf(entity.getSpecialization()))
                .qualifications(entity.getQualifications())
                .yearsOfExperience(entity.getYearsOfExperience())
                .bio(entity.getBio())
                .active(entity.isActive())
                .build();
    }

    public AvailabilitySlot slotToDomain(AvailabilitySlotEntity entity) {
        return AvailabilitySlot.builder()
                .id(entity.getId())
                .doctorId(entity.getDoctor().getId())
                .dayOfWeek(DayOfWeek.valueOf(entity.getDayOfWeek()))
                .startTime(entity.getStartTime())
                .endTime(entity.getEndTime())
                .slotDurationMinutes(entity.getSlotDurationMinutes())
                .available(entity.isAvailable())
                .build();
    }
}
