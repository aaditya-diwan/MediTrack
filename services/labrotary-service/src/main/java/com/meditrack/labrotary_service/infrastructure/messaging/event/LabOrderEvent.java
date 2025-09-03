package com.meditrack.labrotary_service.infrastructure.messaging.event;

import com.meditrack.labrotary_service.domain.model.LabOrder;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LabOrderEvent {
    private UUID orderId;
    private String patientId;
    private String eventType;
}
