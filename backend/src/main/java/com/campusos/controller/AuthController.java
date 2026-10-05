package com.campusos.controller;

import com.campusos.config.LoginRateLimiter;
import com.campusos.dto.request.LoginRequest;
import com.campusos.dto.request.RegisterRequest;
import com.campusos.dto.response.AuthResponse;
import com.campusos.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * AuthController — handles authentication endpoints.
 *
 * This controller is deliberately thin. It:
 * 1. Validates the request body (@Valid)
 * 2. Checks the rate limit (login only)
 * 3. Delegates to AuthService
 * 4. Returns the response
 *
 * NO business logic lives here. That's all in AuthService.
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Register and login endpoints")
public class AuthController {

    private final AuthService authService;
    private final LoginRateLimiter rateLimiter;

    /**
     * POST /api/auth/register
     *
     * Registers a new student account.
     * Returns 201 Created with a JWT token on success.
     *
     * @Valid triggers Bean Validation on RegisterRequest before this method runs.
     * If validation fails, GlobalExceptionHandler returns 422 with field errors.
     */
    @PostMapping("/register")
    @Operation(
        summary = "Register a new student",
        description = "Creates a new student account. Returns a JWT token on success."
    )
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        AuthResponse response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * POST /api/auth/login
     *
     * Authenticates a user and returns a JWT token.
     * Rate limited: 5 attempts per minute per IP address.
     *
     * Returns:
     * - 200 OK with JWT token on success
     * - 429 Too Many Requests if rate limit exceeded
     * - 400 Bad Request with wrong credentials (handled by GlobalExceptionHandler)
     */
    @PostMapping("/login")
    @Operation(
        summary = "Login",
        description = "Authenticate with email and password. Returns a JWT token. Rate limited to 5 attempts per minute per IP."
    )
    public ResponseEntity<?> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest
    ) {
        // Get the client's IP address for rate limiting
        // X-Forwarded-For header is used when behind a proxy/load balancer
        String clientIp = getClientIp(httpRequest);

        if (!rateLimiter.tryConsume(clientIp)) {
            return ResponseEntity
                    .status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(Map.of(
                        "error", "Too Many Requests",
                        "message", "Too many login attempts. Please wait 1 minute before trying again.",
                        "status", 429
                    ));
        }

        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }

    /**
     * Extracts the real client IP from the request.
     * Handles the case where the app is behind a reverse proxy.
     */
    private String getClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
