package com.meditrack.labrotary_service.application.dto;

import lombok.Data;

@Data
public class DiagnosisCodeDto {
    private String system;
    private String code;
    private String description;
}
