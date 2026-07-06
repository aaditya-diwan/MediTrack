package com.meditrack.appointment.infrastructure.client;

import com.meditrack.appointment.domain.model.DoctorSnapshot;
import com.meditrack.appointment.domain.port.DoctorDirectoryPort;
import com.meditrack.appointment.domain.port.DoctorDirectoryUnavailableException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.ClientHttpRequestFactories;
import org.springframework.boot.web.client.ClientHttpRequestFactorySettings;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * HTTP adapter for {@link DoctorDirectoryPort} backed by the doctor-service.
 *
 * <p>The doctor-service secures its GET endpoints with a shared-secret HS256 JWT
 * (see its SecurityConfig/JwtUtil), so this client mints a short-lived service
 * token per call using the same {@code jwt.secret}.
 */
@Slf4j
@Component
public class DoctorServiceClient implements DoctorDirectoryPort {

    private static final Duration TIMEOUT = Duration.ofSeconds(5);
    private static final long SERVICE_TOKEN_TTL_SECONDS = 120;

    private final RestClient restClient;
    private final SecretKey signingKey;

    public DoctorServiceClient(
            RestClient.Builder restClientBuilder,
            @Value("${meditrack.doctor-service.base-url:http://localhost:8084}") String baseUrl,
            @Value("${jwt.secret}") String jwtSecret) {
        this.restClient = restClientBuilder
                .baseUrl(baseUrl)
                .requestFactory(ClientHttpRequestFactories.get(ClientHttpRequestFactorySettings.DEFAULTS
                        .withConnectTimeout(TIMEOUT)
                        .withReadTimeout(TIMEOUT)))
                .build();
        this.signingKey = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public Optional<DoctorSnapshot> findDoctor(UUID doctorId) {
        try {
            DoctorProfileDto dto = restClient.get()
                    .uri("/api/v1/doctors/{id}", doctorId)
                    .header(HttpHeaders.AUTHORIZATION, bearerToken())
                    .retrieve()
                    .body(DoctorProfileDto.class);
            if (dto == null) {
                return Optional.empty();
            }
            return Optional.of(new DoctorSnapshot(dto.id(), dto.fullName(), dto.specialization(), dto.active()));
        } catch (HttpClientErrorException.NotFound e) {
            // The directory answered authoritatively: this doctor does not exist.
            return Optional.empty();
        } catch (RestClientException e) {
            throw new DoctorDirectoryUnavailableException(
                    "doctor-service lookup failed for doctor " + doctorId + ": " + e.getMessage(), e);
        }
    }

    @Override
    public boolean isWithinAvailability(UUID doctorId, LocalDateTime scheduledAt, int assumedDurationMinutes) {
        List<AvailabilitySlotDto> slots = fetchSlots(doctorId);
        DayOfWeek day = scheduledAt.getDayOfWeek();
        LocalTime start = scheduledAt.toLocalTime();
        LocalTime end = start.plusMinutes(assumedDurationMinutes);
        if (end.isBefore(start)) {
            // Appointment would wrap past midnight; no published window can contain it.
            return false;
        }
        return slots.stream().anyMatch(slot -> slot.available()
                && day.name().equalsIgnoreCase(slot.dayOfWeek())
                && !start.isBefore(LocalTime.parse(slot.startTime()))
                && !end.isAfter(LocalTime.parse(slot.endTime())));
    }

    @Override
    public List<String> getAvailableWindowsForDay(UUID doctorId, DayOfWeek dayOfWeek) {
        try {
            return fetchSlots(doctorId).stream()
                    .filter(slot -> slot.available() && dayOfWeek.name().equalsIgnoreCase(slot.dayOfWeek()))
                    .map(slot -> slot.startTime() + "-" + slot.endTime())
                    .sorted()
                    .toList();
        } catch (RuntimeException e) {
            // Best-effort enrichment only; never let it mask the real rejection reason.
            log.debug("Could not fetch availability windows for doctor {}: {}", doctorId, e.getMessage());
            return List.of();
        }
    }

    private List<AvailabilitySlotDto> fetchSlots(UUID doctorId) {
        try {
            List<AvailabilitySlotDto> slots = restClient.get()
                    .uri("/api/v1/doctors/{id}/slots", doctorId)
                    .header(HttpHeaders.AUTHORIZATION, bearerToken())
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {
                    });
            return slots != null ? slots : List.of();
        } catch (RestClientException e) {
            throw new DoctorDirectoryUnavailableException(
                    "doctor-service slots lookup failed for doctor " + doctorId + ": " + e.getMessage(), e);
        }
    }

    /**
     * Mints a short-lived HS256 service token compatible with the doctor-service's
     * JwtRequestFilter (subject + "roles" claim), signed with the shared JWT secret.
     */
    private String bearerToken() {
        Instant now = Instant.now();
        String token = Jwts.builder()
                .subject("appointment-service")
                .claim("roles", List.of("ROLE_ADMIN"))
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(SERVICE_TOKEN_TTL_SECONDS)))
                .signWith(signingKey)
                .compact();
        return "Bearer " + token;
    }

    /** Subset of doctor-service's DoctorResponse that this service needs. */
    private record DoctorProfileDto(UUID id, String fullName, String specialization, boolean active) {
    }

    /** Mirror of doctor-service's AvailabilitySlotResponse. */
    private record AvailabilitySlotDto(UUID id, UUID doctorId, String dayOfWeek, String startTime,
                                       String endTime, int slotDurationMinutes, boolean available) {
    }
}
