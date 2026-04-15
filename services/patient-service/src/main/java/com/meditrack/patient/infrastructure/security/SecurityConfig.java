package com.meditrack.patient.infrastructure.security;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity          // enables @PreAuthorize / @PostAuthorize on controllers
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtRequestFilter jwtRequestFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // CSRF is disabled because this API is stateless (JWT-based); no session cookies are used
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth
                // Public endpoints
                .requestMatchers("/api/v1/auth/**").permitAll()
                .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                .requestMatchers("/swagger-ui/**", "/v3/api-docs/**", "/swagger-ui.html").permitAll()

                // Patient write operations — doctors, nurses, and admins only
                .requestMatchers(HttpMethod.POST,   "/api/v1/patients/**").hasAnyRole("ADMIN", "DOCTOR", "NURSE")
                .requestMatchers(HttpMethod.PUT,    "/api/v1/patients/**").hasAnyRole("ADMIN", "DOCTOR", "NURSE")
                .requestMatchers(HttpMethod.PATCH,  "/api/v1/patients/**").hasAnyRole("ADMIN", "DOCTOR", "NURSE")
                .requestMatchers(HttpMethod.DELETE, "/api/v1/patients/**").hasRole("ADMIN")

                // Patient reads — all authenticated staff
                .requestMatchers(HttpMethod.GET, "/api/v1/patients/**").hasAnyRole("ADMIN", "DOCTOR", "NURSE", "LAB_TECH")

                // Medical records — same as patients
                .requestMatchers(HttpMethod.POST,   "/api/v1/medical-records/**").hasAnyRole("ADMIN", "DOCTOR", "NURSE")
                .requestMatchers(HttpMethod.PUT,    "/api/v1/medical-records/**").hasAnyRole("ADMIN", "DOCTOR", "NURSE")
                .requestMatchers(HttpMethod.GET,    "/api/v1/medical-records/**").hasAnyRole("ADMIN", "DOCTOR", "NURSE", "LAB_TECH")

                // Lab order submission from patient service — doctors and admins
                .requestMatchers("/api/v1/lab-orders/**").hasAnyRole("ADMIN", "DOCTOR")

                // Everything else requires authentication
                .anyRequest().authenticated()
            )
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .addFilterBefore(jwtRequestFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}
