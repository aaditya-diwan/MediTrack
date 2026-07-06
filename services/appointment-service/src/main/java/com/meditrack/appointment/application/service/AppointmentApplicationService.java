package com.meditrack.appointment.application.service;

import com.meditrack.appointment.application.exception.AppointmentNotFoundException;
import com.meditrack.appointment.application.exception.DoctorInactiveException;
import com.meditrack.appointment.application.exception.DoctorNotFoundException;
import com.meditrack.appointment.application.exception.OutsideAvailabilityException;
import com.meditrack.appointment.application.exception.SlotAlreadyBookedException;
import com.meditrack.appointment.application.usecase.*;
import com.meditrack.appointment.domain.model.Appointment;
import com.meditrack.appointment.domain.model.AppointmentStatus;
import com.meditrack.appointment.domain.model.DoctorSnapshot;
import com.meditrack.appointment.domain.port.DoctorDirectoryPort;
import com.meditrack.appointment.domain.port.DoctorDirectoryUnavailableException;
import com.meditrack.appointment.domain.repository.AppointmentRepository;
import com.meditrack.appointment.infrastructure.messaging.event.AppointmentBookedEvent;
import com.meditrack.appointment.infrastructure.messaging.event.AppointmentCompletedEvent;
import com.meditrack.appointment.interfaces.dto.request.BookAppointmentRequest;
import com.meditrack.appointment.interfaces.dto.response.AppointmentResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AppointmentApplicationService implements BookAppointmentUseCase, GetAppointmentUseCase,
        UpdateAppointmentStatusUseCase, GetPatientAppointmentsUseCase, GetDoctorScheduleUseCase,
        CancelAppointmentUseCase {

    /**
     * Assumed appointment length used when checking the requested time against the
     * doctor's published availability windows.
     */
    static final int ASSUMED_APPOINTMENT_DURATION_MINUTES = 30;

    private final AppointmentRepository appointmentRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final DoctorDirectoryPort doctorDirectoryPort;

    @Override
    @Transactional
    public AppointmentResponse bookAppointment(BookAppointmentRequest request) {
        validateDoctor(request.getDoctorId(), request.getScheduledAt());
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
        eventPublisher.publishEvent(AppointmentBookedEvent.builder()
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
            eventPublisher.publishEvent(AppointmentCompletedEvent.builder()
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

    /**
     * Validates against the doctor directory that the doctor exists, is active,
     * and that the requested time falls within their published availability.
     *
     * <p>If the doctor-service is unreachable, validation FAILS OPEN: we log a
     * warning and let the booking proceed, because blocking every booking during
     * a directory outage is worse than accepting an occasionally unvalidated one.
     */
    private void validateDoctor(UUID doctorId, LocalDateTime scheduledAt) {
        DoctorSnapshot doctor;
        try {
            doctor = doctorDirectoryPort.findDoctor(doctorId)
                    .orElseThrow(() -> new DoctorNotFoundException(doctorId));
        } catch (DoctorDirectoryUnavailableException e) {
            log.warn("doctor-service unreachable; proceeding without doctor validation (fail-open) for doctor {}: {}",
                    doctorId, e.getMessage());
            return;
        }
        if (!doctor.active()) {
            throw new DoctorInactiveException(doctorId, doctor.fullName());
        }
        boolean withinAvailability;
        try {
            withinAvailability = doctorDirectoryPort.isWithinAvailability(
                    doctorId, scheduledAt, ASSUMED_APPOINTMENT_DURATION_MINUTES);
        } catch (DoctorDirectoryUnavailableException e) {
            log.warn("doctor-service unreachable; proceeding without availability validation (fail-open) for doctor {}: {}",
                    doctorId, e.getMessage());
            return;
        }
        if (!withinAvailability) {
            List<String> windows = doctorDirectoryPort.getAvailableWindowsForDay(doctorId, scheduledAt.getDayOfWeek());
            String message = "Requested time " + scheduledAt + " is outside the published availability of doctor "
                    + (doctor.fullName() != null ? doctor.fullName() : doctorId) + ". "
                    + (windows.isEmpty()
                        ? "No availability published for " + scheduledAt.getDayOfWeek() + "."
                        : "Available windows on " + scheduledAt.getDayOfWeek() + ": " + String.join(", ", windows));
            throw new OutsideAvailabilityException(message);
        }
    }

    private AppointmentResponse toResponse(Appointment a) {
        return AppointmentResponse.builder().id(a.getId()).patientId(a.getPatientId())
                .doctorId(a.getDoctorId()).status(a.getStatus()).type(a.getType())
                .reasonForVisit(a.getReasonForVisit()).notes(a.getNotes())
                .scheduledAt(a.getScheduledAt()).actualStartAt(a.getActualStartAt())
                .actualEndAt(a.getActualEndAt()).createdAt(a.getCreatedAt()).build();
    }
}
