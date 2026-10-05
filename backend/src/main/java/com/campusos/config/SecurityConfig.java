package com.campusos.config;

import com.campusos.security.JwtAuthFilter;
import com.campusos.security.CustomUserDetailsService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * SecurityConfig — the heart of Spring Security configuration for CampusOS.
 *
 * Key decisions made here:
 *
 * 1. STATELESS sessions: We use JWT, so the server never stores session state.
 *    Every request must carry a valid JWT. This scales better than sessions.
 *
 * 2. CSRF disabled: CSRF attacks exploit session cookies. Since we use JWT in
 *    the Authorization header (not cookies), CSRF doesn't apply.
 *
 * 3. BCrypt with strength 12: BCrypt is a slow, adaptive hashing algorithm
 *    designed for passwords. Strength 12 means 2^12 = 4096 hashing rounds.
 *    This makes brute-force attacks very slow.
 *
 * 4. @EnableMethodSecurity: Enables @PreAuthorize and @PostAuthorize on methods
 *    for fine-grained authorization (e.g., @PreAuthorize("hasRole('ADMIN')")).
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity   // Enables @PreAuthorize on controller methods
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;
    private final CustomUserDetailsService userDetailsService;

    /**
     * The main security filter chain.
     * This method defines which endpoints are public, which require auth,
     * and configures our JWT filter.
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // ── Disable CSRF (not needed with JWT in Authorization header) ──
            .csrf(AbstractHttpConfigurer::disable)

            // ── CORS: allow requests from the React frontend ──
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))

            // ── URL-based authorization rules ──
            .authorizeHttpRequests(auth -> auth
                // Public endpoints — no JWT needed
                .requestMatchers("/api/auth/**").permitAll()
                .requestMatchers("/actuator/health").permitAll()
                .requestMatchers("/swagger-ui/**", "/api-docs/**", "/swagger-ui.html").permitAll()

                // Everything else requires authentication
                .anyRequest().authenticated()
            )

            // ── Stateless session management (no server-side sessions) ──
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )

            // ── Use our custom UserDetailsService for authentication ──
            .authenticationProvider(authenticationProvider())

            // ── Add JWT filter BEFORE Spring's default username/password filter ──
            // This ensures JWT authentication is checked first on every request
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * BCryptPasswordEncoder — used to hash passwords on registration
     * and verify them on login.
     *
     * Strength 12 is a good balance between security and performance.
     * (Strength 10 is Spring's default; 12 adds ~4x more computation)
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    /**
     * DaoAuthenticationProvider — connects Spring Security's authentication
     * mechanism to OUR UserDetailsService and password encoder.
     *
     * When AuthService calls authenticationManager.authenticate(),
     * Spring uses this provider to:
     * 1. Load the user via UserDetailsService.loadUserByUsername()
     * 2. Verify the password using BCrypt
     */
    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    /**
     * AuthenticationManager — needed in AuthService to manually trigger
     * Spring Security's authentication flow (email + password verification).
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config)
            throws Exception {
        return config.getAuthenticationManager();
    }

    /**
     * CORS configuration — allows the React frontend (running on localhost:5173)
     * to make API calls to this backend (running on localhost:8080).
     *
     * Without this, browsers block cross-origin requests.
     * In production, replace localhost:5173 with your actual domain.
     */
    @org.springframework.beans.factory.annotation.Value("${app.cors.allowed-origins:http://localhost:5173,http://localhost:3000}")
    private String[] allowedOrigins;

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of(allowedOrigins));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", configuration);
        return source;
    }
}
