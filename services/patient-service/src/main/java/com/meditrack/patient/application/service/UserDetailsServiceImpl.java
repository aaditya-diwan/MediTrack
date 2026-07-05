package com.meditrack.patient.application.service;

import com.meditrack.patient.infrastructure.security.Role;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * In-memory UserDetailsService for development/demo purposes.
 *
 * TODO: replace with a proper database-backed implementation once a users table
 *       and user-management API are added.
 */
@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // Dev/demo credentials. Each hash below genuinely verifies against the
        // documented password (previously every account shared one hash for the
        // string "password", which contradicted these comments). Replace this
        // in-memory store with a real user system before any production use.
        return switch (username) {
            case "admin" -> buildUser("admin",
                    // BCrypt hash of "admin123"
                    "$2a$10$URFPkTCi4TjU49E/1FNGNO2ZdOjRB4sKlA4LWgcOYJjdtoFz5Khg.",
                    Role.ROLE_ADMIN);
            case "doctor" -> buildUser("doctor",
                    // BCrypt hash of "doctor123"
                    "$2a$10$U0dDmr9Km/YbDu7MPeNW9OlplkkJs/XQ6H2Y8nfopkWOoReNt/dKm",
                    Role.ROLE_DOCTOR);
            case "nurse" -> buildUser("nurse",
                    // BCrypt hash of "nurse123"
                    "$2a$10$YMtDGrGp6oLYAASMm0/svOl0Iu0VPEHLXg6Lu.jQ4SN4xECMsIudW",
                    Role.ROLE_NURSE);
            case "labtech" -> buildUser("labtech",
                    // BCrypt hash of "labtech123"
                    "$2a$10$B20.B18rCayR8W/BeNSyDuBHxLcL5fNi3ZUr8aLjh0R6wXoaJf1cO",
                    Role.ROLE_LAB_TECH);
            default -> throw new UsernameNotFoundException("User not found: " + username);
        };
    }

    private User buildUser(String username, String encodedPassword, Role role) {
        return new User(username, encodedPassword,
                List.of(new SimpleGrantedAuthority(role.name())));
    }
}
