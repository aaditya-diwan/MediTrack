package com.meditrack.appointment.application.service;

import com.meditrack.appointment.application.exception.DoctorInactiveException;
import com.meditrack.appointment.application.exception.DoctorNotFoundException;
import com.meditrack.appointment.application.exception.OutsideAvailabilityException;
import com.meditrack.appointment.application.exception.SlotAlreadyBookedException;
import com.meditrack.appointment.domain.model.Appointment;
import com.meditrack.appointment.domain.model.AppointmentStatus;
import com.meditrack.appointment.domain.model.AppointmentType;
import com.meditrack.appointment.domain.model.DoctorSnapshot;
import com.meditrack.appointment.domain.port.DoctorDirectoryPort;
import com.meditrack.appointment.domain.port.DoctorDirectoryUnavailableException;
import com.meditrack.appointment.domain.repository.AppointmentRepository;
import com.meditrack.appointment.interfaces.dto.request.BookAppointmentRequest;
import com.meditrack.appointment.interfaces.dto.response.AppointmentResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class AppointmentApplicationServiceTest {

    @Mock
    private AppointmentRepository appointmentRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private DoctorDirectoryPort doctorDirectoryPort;

    private AppointmentApplicationService service;

    private UUID doctorId;
    private UUID patientId;
    private LocalDateTime scheduledAt;
    private BookAppointmentRequest request;
    private DoctorSnapshot activeDoctor;

    @BeforeEach
    void setUp() {
        service = new AppointmentApplicationService(appointmentRepository, eventPublisher, doctorDirectoryPort);

        doctorId = UUID.randomUUID();
        patientId = UUID.randomUUID();
        scheduledAt = LocalDateTime.now().plusDays(1).withHour(10).withMinute(0).withSecond(0).withNano(0);

        request = new BookAppointmentRequest();
        request.setPatientId(patientId);
        request.setDoctorId(doctorId);
        request.setType(AppointmentType.FIRST_VISIT);
        request.setScheduledAt(scheduledAt);
        request.setReasonForVisit("Routine check-up");

        activeDoctor = new DoctorSnapshot(doctorId, "Dr. Jane Smith", "CARDIOLOGY", true);
    }

    @Test
    void bookAppointment_shouldSucceed_whenDoctorIsActiveAndTimeIsWithinAvailability() {
        when(doctorDirectoryPort.findDoctor(doctorId)).thenReturn(Optional.of(activeDoctor));
        when(doctorDirectoryPort.isWithinAvailability(eq(doctorId), eq(scheduledAt), anyInt())).thenReturn(true);
        when(appointmentRepository.existsByDoctorIdAndScheduledAt(doctorId, scheduledAt)).thenReturn(false);
        when(appointmentRepository.save(any(Appointment.class))).thenAnswer(inv -> inv.getArgument(0));

        AppointmentResponse response = service.bookAppointment(request);

        assertNotNull(response);
        assertEquals(doctorId, response.getDoctorId());
        assertEquals(patientId, response.getPatientId());
        assertEquals(AppointmentStatus.CONFIRMED, response.getStatus());
        verify(appointmentRepository).save(any(Appointment.class));
        verify(eventPublisher).publishEvent(any(Object.class));
    }

    @Test
    void bookAppointment_shouldThrowDoctorNotFound_whenDoctorDoesNotExist() {
        when(doctorDirectoryPort.findDoctor(doctorId)).thenReturn(Optional.empty());

        DoctorNotFoundException ex = assertThrows(DoctorNotFoundException.class,
                () -> service.bookAppointment(request));

        assertTrue(ex.getMessage().contains("Doctor not found"));
        verify(appointmentRepository, never()).save(any(Appointment.class));
    }

    @Test
    void bookAppointment_shouldThrowDoctorInactive_whenDoctorIsNotActive() {
        DoctorSnapshot inactiveDoctor = new DoctorSnapshot(doctorId, "Dr. Jane Smith", "CARDIOLOGY", false);
        when(doctorDirectoryPort.findDoctor(doctorId)).thenReturn(Optional.of(inactiveDoctor));

        DoctorInactiveException ex = assertThrows(DoctorInactiveException.class,
                () -> service.bookAppointment(request));

        assertTrue(ex.getMessage().contains("not active"));
        verify(appointmentRepository, never()).save(any(Appointment.class));
    }

    @Test
    void bookAppointment_shouldThrowOutsideAvailability_whenTimeOutsidePublishedWindows() {
        when(doctorDirectoryPort.findDoctor(doctorId)).thenReturn(Optional.of(activeDoctor));
        when(doctorDirectoryPort.isWithinAvailability(eq(doctorId), eq(scheduledAt), anyInt())).thenReturn(false);
        when(doctorDirectoryPort.getAvailableWindowsForDay(doctorId, scheduledAt.getDayOfWeek()))
                .thenReturn(List.of("09:00-12:00", "14:00-17:00"));

        OutsideAvailabilityException ex = assertThrows(OutsideAvailabilityException.class,
                () -> service.bookAppointment(request));

        assertTrue(ex.getMessage().contains("outside the published availability"));
        assertTrue(ex.getMessage().contains("09:00-12:00"));
        assertTrue(ex.getMessage().contains("14:00-17:00"));
        verify(appointmentRepository, never()).save(any(Appointment.class));
    }

    @Test
    void bookAppointment_shouldFailOpen_whenDoctorServiceIsDownOnLookup() {
        when(doctorDirectoryPort.findDoctor(doctorId))
                .thenThrow(new DoctorDirectoryUnavailableException("connection refused", new RuntimeException()));
        when(appointmentRepository.existsByDoctorIdAndScheduledAt(doctorId, scheduledAt)).thenReturn(false);
        when(appointmentRepository.save(any(Appointment.class))).thenAnswer(inv -> inv.getArgument(0));

        AppointmentResponse response = service.bookAppointment(request);

        assertNotNull(response);
        assertEquals(AppointmentStatus.CONFIRMED, response.getStatus());
        verify(appointmentRepository).save(any(Appointment.class));
    }

    @Test
    void bookAppointment_shouldFailOpen_whenDoctorServiceIsDownOnAvailabilityCheck() {
        when(doctorDirectoryPort.findDoctor(doctorId)).thenReturn(Optional.of(activeDoctor));
        when(doctorDirectoryPort.isWithinAvailability(eq(doctorId), eq(scheduledAt), anyInt()))
                .thenThrow(new DoctorDirectoryUnavailableException("read timeout", new RuntimeException()));
        when(appointmentRepository.existsByDoctorIdAndScheduledAt(doctorId, scheduledAt)).thenReturn(false);
        when(appointmentRepository.save(any(Appointment.class))).thenAnswer(inv -> inv.getArgument(0));

        AppointmentResponse response = service.bookAppointment(request);

        assertNotNull(response);
        verify(appointmentRepository).save(any(Appointment.class));
        verify(doctorDirectoryPort, never()).getAvailableWindowsForDay(any(UUID.class), any(DayOfWeek.class));
    }

    @Test
    void bookAppointment_shouldStillRejectDoubleBooking_whenSlotAlreadyTaken() {
        when(doctorDirectoryPort.findDoctor(doctorId)).thenReturn(Optional.of(activeDoctor));
        when(doctorDirectoryPort.isWithinAvailability(eq(doctorId), eq(scheduledAt), anyInt())).thenReturn(true);
        when(appointmentRepository.existsByDoctorIdAndScheduledAt(doctorId, scheduledAt)).thenReturn(true);

        assertThrows(SlotAlreadyBookedException.class, () -> service.bookAppointment(request));

        verify(appointmentRepository, never()).save(any(Appointment.class));
    }
}
