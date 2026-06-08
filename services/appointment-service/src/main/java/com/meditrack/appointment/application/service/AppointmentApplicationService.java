package com.meditrack.appointment.application.service;

import com.meditrack.appointment.application.exception.AppointmentNotFoundException;
import com.meditrack.appointment.application.exception.SlotAlreadyBookedException;
import com.meditrack.appointment.application.usecase.*;
import com.meditrack.appointment.domain.model.Appointment;
import com.meditrack.appointment.domain.model.AppointmentStatus;
import com.meditrack.appointment.domain.repository.AppointmentRepository;
import com.meditrack.appointment.infrastructure.messaging.AppointmentEventProducer;
import com.meditrack.appointment.infrastructure.messaging.event.AppointmentBookedEvent;
import com.meditrack.appointment.infrastructure.messaging.event.AppointmentCompletedEvent;
import com.meditrack.appointment.interfaces.dto.request.BookAppointmentRequest;
import com.meditrack.appointment.interfaces.dto.response.AppointmentResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AppointmentApplicationService implements BookAppointmentUseCase, GetAppointmentUseCase,
        UpdateAppointmentStatusUseCase, GetPatientAppointmentsUseCase, GetDoctorScheduleUseCase,
        CancelAppointmentUseCase {

    private final AppointmentRepository appointmentRepository;
    private final AppointmentEventProducer eventProducer;

    @Override
    @Transactional
    public AppointmentResponse bookAppointment(BookAppointmentRequest request) {
        if (appointmentRepository.existsByDoctorIdAndScheduledAt(request.getDoctorId(), request.getScheduledAt())) {
            throw new SlotAlreadyBookedException("Slot already booked for this doctor at: " + request.getScheduledAt());
        }
        Appointment appointment = Appointment.builder()
                .id(UUID.randomUUID())
                .patientId(request.getPatientId())
                .doctorId(request.getDoctorId())
                .status(AppointmentStatus.CONFIRMED)
                .type(request.getType())
                .reasonForVisit(request.getReasonForVisit())
                .scheduledAt(request.getScheduledAt())
                .build();
        Appointment saved = appointmentRepository.save(appointment);
        log.info("Booked appointment id={}", saved.getId());
        eventProducer.publishBooked(AppointmentBookedEvent.builder()
                .appointmentId(saved.getId())
                .patientId(saved.getPatientId())
                .doctorId(saved.getDoctorId())
                .scheduledAt(saved.getScheduledAt())
                .type(saved.getType().name())
                .occurredAt(Instant.now())
                .build());
        return toResponse(saved);
    }

    @Override
    public AppointmentResponse getAppointmentById(UUID id) {
        return appointmentRepository.findById(id).map(this::toResponse)
                .orElseThrow(() -> new AppointmentNotFoundException(id));
    }

    @Override
    @Transactional
    public AppointmentResponse updateStatus(UUID id, AppointmentStatus status) {
        Appointment a = appointmentRepository.findById(id)
                .orElseThrow(() -> new AppointmentNotFoundException(id));
        a.setStatus(status);
        Appointment updated = appointmentRepository.save(a);
        if (status == AppointmentStatus.COMPLETED) {
            eventProducer.publishCompleted(AppointmentCompletedEvent.builder()
                    .appointmentId(updated.getId()).patientId(updated.getPatientId())
                    .doctorId(updated.getDoctorId()).occurredAt(Instant.now()).build());
        }
        return toResponse(updated);
    }

    @Override
    public List<AppointmentResponse> getPatientAppointments(UUID patientId) {
        return appointmentRepository.findByPatientId(patientId).stream()
                .map(this::toResponse).collect(Collectors.toList());
    }

    @Override
    public List<AppointmentResponse> getDoctorSchedule(UUID doctorId) {
        return appointmentRepository.findByDoctorId(doctorId).stream()
                .map(this::toResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public AppointmentResponse cancelAppointment(UUID id) {
        return updateStatus(id, AppointmentStatus.CANCELLED);
    }

    private AppointmentResponse toResponse(Appointment a) {
        return AppointmentResponse.builder().id(a.getId()).patientId(a.getPatientId())
                .doctorId(a.getDoctorId()).status(a.getStatus()).type(a.getType())
                .reasonForVisit(a.getReasonForVisit()).notes(a.getNotes())
                .scheduledAt(a.getScheduledAt()).actualStartAt(a.getActualStartAt())
                .actualEndAt(a.getActualEndAt()).createdAt(a.getCreatedAt()).build();
    }
}
