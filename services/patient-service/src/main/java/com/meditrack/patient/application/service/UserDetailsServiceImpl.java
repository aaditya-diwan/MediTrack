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
        return switch (username) {
            case "admin" -> buildUser("admin",
                    // BCrypt hash of "admin123" — change before production
                    "$2a$10$slYQmyNdGzTn7ZLBXBChFOC9f6kFjAqPhccnP6DxlWXx2lPk1C3G6",
                    Role.ROLE_ADMIN);
            case "doctor" -> buildUser("doctor",
                    // BCrypt hash of "doctor123"
                    "$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy",
                    Role.ROLE_DOCTOR);
            case "nurse" -> buildUser("nurse",
                    // BCrypt hash of "nurse123"
                    "$2a$10$Dow3WUimGaWZV5TZo0wsTunqoiP7TbQjYhbUFJMTJW7BuU/BFvUhK",
                    Role.ROLE_NURSE);
            case "labtech" -> buildUser("labtech",
                    // BCrypt hash of "labtech123"
                    "$2a$10$2gPBFfKt5kKOhWiHiJwGSe5BQ9Gv2Vu2TbgPgO3Y6RVnb9qFIz2e",
                    Role.ROLE_LAB_TECH);
            default -> throw new UsernameNotFoundException("User not found: " + username);
        };
    }

    private User buildUser(String username, String encodedPassword, Role role) {
        return new User(username, encodedPassword,
                List.of(new SimpleGrantedAuthority(role.name())));
    }
}
