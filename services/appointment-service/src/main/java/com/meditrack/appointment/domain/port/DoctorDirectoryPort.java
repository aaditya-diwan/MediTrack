package com.meditrack.appointment.domain.port;

import com.meditrack.appointment.domain.model.DoctorSnapshot;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Outbound port to the doctor directory (implemented by an HTTP client
 * against the doctor-service in infrastructure).
 */
public interface DoctorDirectoryPort {

    /**
     * Look up a doctor's profile.
     *
     * @return the doctor snapshot, or {@link Optional#empty()} when the doctor
     *         directory confirms the doctor does not exist
     * @throws DoctorDirectoryUnavailableException when the directory cannot be reached
     */
    Optional<DoctorSnapshot> findDoctor(UUID doctorId);

    /**
     * Check whether an appointment starting at {@code scheduledAt} and lasting
     * {@code assumedDurationMinutes} fits inside one of the doctor's published
     * availability windows.
     *
     * @throws DoctorDirectoryUnavailableException when the directory cannot be reached
     */
    boolean isWithinAvailability(UUID doctorId, LocalDateTime scheduledAt, int assumedDurationMinutes);

    /**
     * Best-effort, human-readable availability windows (e.g. "09:00-12:00") for a
     * given day, used to enrich error messages. Returns an empty list on any failure
     * instead of throwing.
     */
    List<String> getAvailableWindowsForDay(UUID doctorId, DayOfWeek dayOfWeek);
}
