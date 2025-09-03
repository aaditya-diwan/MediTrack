package com.meditrack.patient.domain.model.valueobjects;

import lombok.Value;

import java.util.UUID;

@Value
public class PatientId {
    UUID id;

    public static PatientId generate() {
        return new PatientId(UUID.randomUUID());
    }
}
