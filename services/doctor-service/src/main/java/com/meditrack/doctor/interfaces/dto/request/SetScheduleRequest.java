package com.meditrack.doctor.interfaces.dto.request;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class SetScheduleRequest {
    @NotEmpty
    private List<SlotRequest> slots;

    @Data
    public static class SlotRequest {
        private String dayOfWeek;
        private String startTime;
        private String endTime;
        private int slotDurationMinutes = 30;
    }
}
