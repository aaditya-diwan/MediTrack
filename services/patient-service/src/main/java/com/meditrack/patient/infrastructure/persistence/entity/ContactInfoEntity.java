package com.meditrack.patient.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Transient;
import lombok.Data;

/**
 * Embedded contact fields on {@code patients}. The V2 schema stores the phone in
 * a column named {@code phone} and has no address column (addresses live in the
 * separate {@code patient_addresses} table), so {@code address} is transient.
 */
@Data
@Embeddable
public class ContactInfoEntity {
    private String email;

    @Column(name = "phone")
    private String phoneNumber;

    @Transient
    private String address;
}
