package com.campusos.controller;

import com.campusos.dto.request.UpdateProfileRequest;
import com.campusos.dto.response.UserResponse;
import com.campusos.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * UserController — handles user profile endpoints.
 *
 * All endpoints here require a valid JWT token (configured in SecurityConfig).
 * The @SecurityRequirement annotation adds the "Bearer" lock icon in Swagger UI.
 */
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Tag(name = "Users", description = "User profile endpoints")
@SecurityRequirement(name = "Bearer Authentication")
public class UserController {

    private final UserService userService;

    /**
     * GET /api/users/me
     *
     * Returns the currently authenticated user's profile.
     * The "currently authenticated user" is determined from the JWT token
     * by JwtAuthFilter → SecurityContextHolder → UserService.getCurrentUserEmail()
     */
    @GetMapping("/me")
    @Operation(summary = "Get my profile", description = "Returns the logged-in user's profile")
    public ResponseEntity<UserResponse> getMyProfile() {
        return ResponseEntity.ok(userService.getMyProfile());
    }

    /**
     * PUT /api/users/me
     *
     * Updates the currently authenticated user's profile.
     * All fields are optional — only provided fields are updated.
     * Email and role cannot be changed through this endpoint.
     */
    @PutMapping("/me")
    @Operation(summary = "Update my profile", description = "Updates the logged-in user's profile. All fields optional.")
    public ResponseEntity<UserResponse> updateMyProfile(
            @Valid @RequestBody UpdateProfileRequest request
    ) {
        return ResponseEntity.ok(userService.updateMyProfile(request));
    }
}
