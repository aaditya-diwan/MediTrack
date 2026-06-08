package com.meditrack.appointment.interfaces.rest;

import com.meditrack.appointment.application.usecase.*;
import com.meditrack.appointment.domain.model.AppointmentStatus;
import com.meditrack.appointment.interfaces.dto.request.BookAppointmentRequest;
import com.meditrack.appointment.interfaces.dto.response.AppointmentResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/appointments")
@RequiredArgsConstructor
public class AppointmentController {

    private final BookAppointmentUseCase bookAppointmentUseCase;
    private final GetAppointmentUseCase getAppointmentUseCase;
    private final UpdateAppointmentStatusUseCase updateStatusUseCase;
    private final GetPatientAppointmentsUseCase getPatientAppointmentsUseCase;
    private final GetDoctorScheduleUseCase getDoctorScheduleUseCase;
    private final CancelAppointmentUseCase cancelAppointmentUseCase;

    @PostMapping
    public ResponseEntity<AppointmentResponse> book(@Valid @RequestBody BookAppointmentRequest request) {
        return new ResponseEntity<>(bookAppointmentUseCase.bookAppointment(request), HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    public ResponseEntity<AppointmentResponse> get(@PathVariable UUID id) {
        return ResponseEntity.ok(getAppointmentUseCase.getAppointmentById(id));
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<AppointmentResponse> updateStatus(
            @PathVariable UUID id, @RequestParam AppointmentStatus status) {
        return ResponseEntity.ok(updateStatusUseCase.updateStatus(id, status));
    }

    @GetMapping("/patient/{patientId}")
    public ResponseEntity<List<AppointmentResponse>> getPatientAppointments(@PathVariable UUID patientId) {
        return ResponseEntity.ok(getPatientAppointmentsUseCase.getPatientAppointments(patientId));
    }

    @GetMapping("/doctor/{doctorId}")
    public ResponseEntity<List<AppointmentResponse>> getDoctorSchedule(@PathVariable UUID doctorId) {
        return ResponseEntity.ok(getDoctorScheduleUseCase.getDoctorSchedule(doctorId));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<AppointmentResponse> cancel(@PathVariable UUID id) {
        return ResponseEntity.ok(cancelAppointmentUseCase.cancelAppointment(id));
    }
}
