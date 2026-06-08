package com.meditrack.doctor.interfaces.rest;

import com.meditrack.doctor.application.usecase.*;
import com.meditrack.doctor.domain.model.Specialization;
import com.meditrack.doctor.interfaces.dto.request.CreateDoctorRequest;
import com.meditrack.doctor.interfaces.dto.request.SetScheduleRequest;
import com.meditrack.doctor.interfaces.dto.response.AvailabilitySlotResponse;
import com.meditrack.doctor.interfaces.dto.response.DoctorResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/doctors")
@RequiredArgsConstructor
public class DoctorController {

    private final CreateDoctorUseCase createDoctorUseCase;
    private final GetDoctorUseCase getDoctorUseCase;
    private final ListDoctorsUseCase listDoctorsUseCase;
    private final SetDoctorScheduleUseCase setDoctorScheduleUseCase;
    private final GetAvailableSlotsUseCase getAvailableSlotsUseCase;

    @PostMapping
    public ResponseEntity<DoctorResponse> createDoctor(@Valid @RequestBody CreateDoctorRequest request) {
        return new ResponseEntity<>(createDoctorUseCase.createDoctor(request), HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    public ResponseEntity<DoctorResponse> getDoctor(@PathVariable UUID id) {
        return ResponseEntity.ok(getDoctorUseCase.getDoctorById(id));
    }

    @GetMapping
    public ResponseEntity<List<DoctorResponse>> listDoctors(
            @RequestParam(required = false) Specialization specialization) {
        if (specialization != null) {
            return ResponseEntity.ok(listDoctorsUseCase.listBySpecialization(specialization));
        }
        return ResponseEntity.ok(listDoctorsUseCase.listAllDoctors());
    }

    @PostMapping("/{id}/schedule")
    public ResponseEntity<List<AvailabilitySlotResponse>> setSchedule(
            @PathVariable UUID id,
            @Valid @RequestBody SetScheduleRequest request) {
        return ResponseEntity.ok(setDoctorScheduleUseCase.setSchedule(id, request));
    }

    @GetMapping("/{id}/slots")
    public ResponseEntity<List<AvailabilitySlotResponse>> getSlots(@PathVariable UUID id) {
        return ResponseEntity.ok(getAvailableSlotsUseCase.getSlots(id));
    }
}
