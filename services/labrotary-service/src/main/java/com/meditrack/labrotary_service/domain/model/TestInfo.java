package com.meditrack.labrotary_service.domain.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TestInfo {
    private String testCode;
    private String testName;
    private String specimenType;
    private String clinicalNotes;
}
