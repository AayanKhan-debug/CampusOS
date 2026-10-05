package com.campusos.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * JwtAuthFilter — intercepts every HTTP request and authenticates the user
 * based on the JWT token in the Authorization header.
 *
 * FLOW for each request:
 * 1. Extract "Authorization" header
 * 2. If it starts with "Bearer ", parse the token
 * 3. Extract the email (username) from the token
 * 4. Load UserDetails from the database using that email
 * 5. If the token is valid, set the Authentication in SecurityContextHolder
 * 6. Spring Security sees the user is authenticated and allows the request
 *
 * Extends OncePerRequestFilter → guarantees this filter runs exactly once
 * per request (not multiple times for async/forwarded requests).
 *
 * This filter is added BEFORE UsernamePasswordAuthenticationFilter in
 * the security filter chain (configured in SecurityConfig).
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final CustomUserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        final String authHeader = request.getHeader("Authorization");

        // If no Authorization header or it doesn't start with "Bearer ", skip JWT processing
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        // Extract token: "Bearer eyJhbGci..." → "eyJhbGci..."
        final String jwt = authHeader.substring(7);

        try {
            final String userEmail = jwtUtil.extractUsername(jwt);

            /*
             * Only proceed if:
             * - email was extracted from the token successfully
             * - no authentication is already set in this request context
             *   (avoids re-authenticating if filter runs multiple times)
             */
            if (userEmail != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                UserDetails userDetails = userDetailsService.loadUserByUsername(userEmail);

                if (jwtUtil.isTokenValid(jwt, userDetails)) {
                    /*
                     * Create an Authentication token.
                     * This is what Spring Security uses to identify the current user.
                     *
                     * UsernamePasswordAuthenticationToken(principal, credentials, authorities)
                     * - principal   : UserDetails object (who is logged in)
                     * - credentials : null (we don't need the password after authentication)
                     * - authorities : the user's roles (ROLE_STUDENT, etc.)
                     */
                    UsernamePasswordAuthenticationToken authToken =
                            new UsernamePasswordAuthenticationToken(
                                    userDetails,
                                    null,
                                    userDetails.getAuthorities()
                            );

                    // Attach request details (IP address, session) to the auth object
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                    // Store the authentication in the SecurityContext for this request
                    SecurityContextHolder.getContext().setAuthentication(authToken);

                    log.debug("JWT authenticated user: {}", userEmail);
                }
            }
        } catch (Exception e) {
            // Token parsing failed (expired, malformed, bad signature)
            // Don't throw — just let the request continue unauthenticated.
            // Spring Security will then reject it with 401 if the endpoint requires auth.
            log.warn("JWT processing failed for request to {}: {}", request.getRequestURI(), e.getMessage());
        }

        // Always pass the request down the filter chain
        filterChain.doFilter(request, response);
    }
}
