package com.meditrack.labrotary_service.application.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.UUID;

@Data
@AllArgsConstructor
public class LabOrderResponse {
    private UUID id;
}
