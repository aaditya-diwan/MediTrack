package com.meditrack.appointment.domain.model;

import java.util.UUID;

/**
 * Read-only view of a doctor as published by the doctor-service.
 * Only the fields the appointment domain cares about are carried over.
 */
public record DoctorSnapshot(UUID id, String fullName, String specialization, boolean active) {
}
