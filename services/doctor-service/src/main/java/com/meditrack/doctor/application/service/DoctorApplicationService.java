package com.meditrack.doctor.application.service;

import com.meditrack.doctor.application.exception.DoctorNotFoundException;
import com.meditrack.doctor.application.exception.DuplicateDoctorException;
import com.meditrack.doctor.application.usecase.*;
import com.meditrack.doctor.domain.model.AvailabilitySlot;
import com.meditrack.doctor.domain.model.Doctor;
import com.meditrack.doctor.domain.model.Specialization;
import com.meditrack.doctor.domain.repository.AvailabilitySlotRepository;
import com.meditrack.doctor.domain.repository.DoctorRepository;
import com.meditrack.doctor.infrastructure.messaging.DoctorEventProducer;
import com.meditrack.doctor.infrastructure.messaging.event.DoctorCreatedEvent;
import com.meditrack.doctor.interfaces.dto.request.CreateDoctorRequest;
import com.meditrack.doctor.interfaces.dto.request.SetScheduleRequest;
import com.meditrack.doctor.interfaces.dto.response.AvailabilitySlotResponse;
import com.meditrack.doctor.interfaces.dto.response.DoctorResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DoctorApplicationService implements CreateDoctorUseCase, GetDoctorUseCase, ListDoctorsUseCase,
        SetDoctorScheduleUseCase, GetAvailableSlotsUseCase {

    private final DoctorRepository doctorRepository;
    private final AvailabilitySlotRepository slotRepository;
    private final DoctorEventProducer eventProducer;

    @Override
    @Transactional
    public DoctorResponse createDoctor(CreateDoctorRequest request) {
        if (doctorRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateDoctorException("Doctor already exists with email: " + request.getEmail());
        }
        if (doctorRepository.existsByEmployeeId(request.getEmployeeId())) {
            throw new DuplicateDoctorException("Doctor already exists with employee ID: " + request.getEmployeeId());
        }

        Doctor doctor = Doctor.builder()
                .id(UUID.randomUUID())
                .employeeId(request.getEmployeeId())
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .email(request.getEmail())
                .phone(request.getPhone())
                .specialization(request.getSpecialization())
                .qualifications(request.getQualifications())
                .yearsOfExperience(request.getYearsOfExperience() != null ? request.getYearsOfExperience() : 0)
                .bio(request.getBio())
                .active(true)
                .build();

        Doctor saved = doctorRepository.save(doctor);
        log.info("Created doctor id={} employeeId={}", saved.getId(), saved.getEmployeeId());

        eventProducer.publishDoctorCreated(DoctorCreatedEvent.builder()
                .doctorId(saved.getId())
                .employeeId(saved.getEmployeeId())
                .fullName(saved.getFullName())
                .specialization(saved.getSpecialization().name())
                .occurredAt(Instant.now())
                .build());

        return toResponse(saved);
    }

    @Override
    public DoctorResponse getDoctorById(UUID id) {
        return doctorRepository.findById(id)
                .map(this::toResponse)
                .orElseThrow(() -> new DoctorNotFoundException(id));
    }

    @Override
    public List<DoctorResponse> listAllDoctors() {
        return doctorRepository.findAllActive().stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Override
    public List<DoctorResponse> listBySpecialization(Specialization specialization) {
        return doctorRepository.findBySpecialization(specialization).stream()
                .map(this::toResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public List<AvailabilitySlotResponse> setSchedule(UUID doctorId, SetScheduleRequest request) {
        doctorRepository.findById(doctorId).orElseThrow(() -> new DoctorNotFoundException(doctorId));
        slotRepository.deleteByDoctorId(doctorId);

        List<AvailabilitySlot> slots = request.getSlots().stream()
                .map(s -> AvailabilitySlot.builder()
                        .id(UUID.randomUUID())
                        .doctorId(doctorId)
                        .dayOfWeek(DayOfWeek.valueOf(s.getDayOfWeek().toUpperCase()))
                        .startTime(LocalTime.parse(s.getStartTime()))
                        .endTime(LocalTime.parse(s.getEndTime()))
                        .slotDurationMinutes(s.getSlotDurationMinutes())
                        .available(true)
                        .build())
                .collect(Collectors.toList());

        return slots.stream()
                .map(slotRepository::save)
                .map(this::toSlotResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<AvailabilitySlotResponse> getSlots(UUID doctorId) {
        doctorRepository.findById(doctorId).orElseThrow(() -> new DoctorNotFoundException(doctorId));
        return slotRepository.findByDoctorId(doctorId).stream()
                .map(this::toSlotResponse).collect(Collectors.toList());
    }

    private DoctorResponse toResponse(Doctor d) {
        return DoctorResponse.builder()
                .id(d.getId())
                .employeeId(d.getEmployeeId())
                .firstName(d.getFirstName())
                .lastName(d.getLastName())
                .fullName(d.getFullName())
                .email(d.getEmail())
                .phone(d.getPhone())
                .specialization(d.getSpecialization())
                .qualifications(d.getQualifications())
                .yearsOfExperience(d.getYearsOfExperience())
                .bio(d.getBio())
                .active(d.isActive())
                .build();
    }

    private AvailabilitySlotResponse toSlotResponse(AvailabilitySlot s) {
        return AvailabilitySlotResponse.builder()
                .id(s.getId())
                .doctorId(s.getDoctorId())
                .dayOfWeek(s.getDayOfWeek().name())
                .startTime(s.getStartTime().toString())
                .endTime(s.getEndTime().toString())
                .slotDurationMinutes(s.getSlotDurationMinutes())
                .available(s.isAvailable())
                .build();
    }
}
