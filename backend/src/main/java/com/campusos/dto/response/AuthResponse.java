package com.campusos.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * AuthResponse — returned after successful register or login.
 *
 * Contains:
 * - token    : the JWT access token the client must include in every
 *              subsequent request as "Authorization: Bearer <token>"
 * - tokenType: always "Bearer" (OAuth 2.0 convention)
 * - user     : basic user info so the frontend doesn't need an extra /me call
 *
 * IMPORTANT: password is NEVER included here.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {

    private String token;

    @Builder.Default
    private String tokenType = "Bearer";

    private UserResponse user;
}
