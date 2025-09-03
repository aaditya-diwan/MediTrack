package com.meditrack.labrotary_service.application.dto;

import lombok.Data;

@Data
public class TestInfoDto {
    private String testCode;
    private String testName;
    private String specimenType;
    private String clinicalNotes;
}
