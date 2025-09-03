package com.meditrack.patient.infrastructure.persistence.entity;

import jakarta.persistence.Embeddable;
import lombok.Data;

@Data
@Embeddable
public class ContactInfoEntity {
    private String email;
    private String phoneNumber;
    private String address;
}
