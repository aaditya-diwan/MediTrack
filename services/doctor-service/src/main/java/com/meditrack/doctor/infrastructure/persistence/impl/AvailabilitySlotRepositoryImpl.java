package com.meditrack.doctor.infrastructure.persistence.impl;

import com.meditrack.doctor.domain.model.AvailabilitySlot;
import com.meditrack.doctor.domain.repository.AvailabilitySlotRepository;
import com.meditrack.doctor.infrastructure.persistence.entity.AvailabilitySlotEntity;
import com.meditrack.doctor.infrastructure.persistence.entity.DoctorEntity;
import com.meditrack.doctor.infrastructure.persistence.mapper.DoctorPersistenceMapper;
import com.meditrack.doctor.infrastructure.persistence.repository.JpaAvailabilitySlotRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.DayOfWeek;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class AvailabilitySlotRepositoryImpl implements AvailabilitySlotRepository {

    private final JpaAvailabilitySlotRepository jpaRepository;
    private final DoctorPersistenceMapper mapper;

    @Override
    public AvailabilitySlot save(AvailabilitySlot slot) {
        AvailabilitySlotEntity entity = new AvailabilitySlotEntity();
        entity.setId(slot.getId());
        DoctorEntity doctorRef = new DoctorEntity();
        doctorRef.setId(slot.getDoctorId());
        entity.setDoctor(doctorRef);
        entity.setDayOfWeek(slot.getDayOfWeek().name());
        entity.setStartTime(slot.getStartTime());
        entity.setEndTime(slot.getEndTime());
        entity.setSlotDurationMinutes(slot.getSlotDurationMinutes());
        entity.setAvailable(slot.isAvailable());
        return mapper.slotToDomain(jpaRepository.save(entity));
    }

    @Override
    public List<AvailabilitySlot> findByDoctorId(UUID doctorId) {
        return jpaRepository.findByDoctorId(doctorId).stream()
                .map(mapper::slotToDomain).collect(Collectors.toList());
    }

    @Override
    public List<AvailabilitySlot> findByDoctorIdAndDayOfWeek(UUID doctorId, DayOfWeek dayOfWeek) {
        return jpaRepository.findByDoctorIdAndDayOfWeek(doctorId, dayOfWeek.name()).stream()
                .map(mapper::slotToDomain).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void deleteByDoctorId(UUID doctorId) {
        jpaRepository.deleteByDoctorId(doctorId);
    }
}
