package com.meditrack.labrotary_service.domain.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DiagnosisCode {
    private String system;
    private String code;
    private String description;
}
