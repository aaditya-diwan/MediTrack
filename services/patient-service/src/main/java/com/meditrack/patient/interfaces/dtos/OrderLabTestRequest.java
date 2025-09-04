package com.meditrack.patient.interfaces.dtos;

import lombok.Data;

@Data
public class OrderLabTestRequest {
    private String testCode;
    private String priority;
    private String doctorId;
    private String notes;
}