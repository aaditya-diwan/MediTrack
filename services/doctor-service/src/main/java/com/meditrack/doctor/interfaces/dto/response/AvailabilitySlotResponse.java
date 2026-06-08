package com.meditrack.doctor.interfaces.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class AvailabilitySlotResponse {
    private UUID id;
    private UUID doctorId;
    private String dayOfWeek;
    private String startTime;
    private String endTime;
    private int slotDurationMinutes;
    private boolean available;
}
