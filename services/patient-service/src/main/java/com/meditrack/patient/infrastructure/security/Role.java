package com.meditrack.patient.infrastructure.security;

/**
 * Application roles used for RBAC.
 *
 * Spring Security expects role names to have the ROLE_ prefix when using hasRole().
 * These constants include the prefix so they can be used directly as GrantedAuthority values.
 */
public enum Role {
    ROLE_ADMIN,
    ROLE_DOCTOR,
    ROLE_NURSE,
    ROLE_LAB_TECH;

    /** Returns the role name without the ROLE_ prefix (for JWT claims etc.) */
    public String shortName() {
        return name().substring(5); // strip "ROLE_"
    }
}
