package com.meditrack.doctor.infrastructure.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Validates the Bearer JWT on each request and populates the SecurityContext
 * with the username and the authorities carried in the token's "roles" claim.
 *
 * <p>Unlike the patient-service filter, there is no UserDetailsService lookup:
 * the doctor-service has no local user store, so the signed token alone
 * establishes identity and authorities.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtRequestFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {

        final String header = request.getHeader("Authorization");

        if (header != null && header.startsWith("Bearer ")
                && SecurityContextHolder.getContext().getAuthentication() == null) {

            final String token = header.substring(7);

            if (jwtUtil.isTokenValid(token)) {
                final String username = jwtUtil.getUsernameFromToken(token);
                final List<SimpleGrantedAuthority> authorities = jwtUtil.getRolesFromToken(token)
                        .stream()
                        .map(SimpleGrantedAuthority::new)
                        .collect(Collectors.toList());

                UsernamePasswordAuthenticationToken authToken =
                        new UsernamePasswordAuthenticationToken(username, null, authorities);
                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authToken);
            } else {
                log.warn("Rejected invalid or expired JWT");
            }
        } else if (header != null && !header.startsWith("Bearer ")) {
            log.warn("Authorization header does not begin with Bearer");
        }

        chain.doFilter(request, response);
    }
}
