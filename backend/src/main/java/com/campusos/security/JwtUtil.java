package com.campusos.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * JwtUtil — handles all JWT operations: create, parse, validate.
 *
 * HOW JWT WORKS (quick summary for learning):
 * A JWT has three parts separated by dots:  header.payload.signature
 *
 * header   : {"alg":"HS256","typ":"JWT"}  → base64-encoded
 * payload  : {"sub":"user@email.com","iat":...,"exp":...} → base64-encoded
 * signature: HMACSHA256(base64(header) + "." + base64(payload), secret)
 *
 * The server issues the token at login. The client stores it and sends it
 * back in the Authorization header on every request:
 *   Authorization: Bearer eyJhbGci...
 *
 * The server verifies the SIGNATURE to prove the token was not tampered with.
 * If the signature is valid, the server trusts the payload (email, role, etc.)
 * without needing to query the database on every request.
 *
 * Algorithm chosen: HS256 (HMAC-SHA256)
 *   - Symmetric: same key signs and verifies
 *   - Simpler than RS256 (asymmetric) for single-server deployments
 *   - Trade-off: if the secret leaks, all tokens can be forged
 *   - RS256 would be better for multi-service/microservice scenarios
 */
@Component
@Slf4j
public class JwtUtil {

    @Value("${app.jwt.secret}")
    private String jwtSecret;

    @Value("${app.jwt.expiration-ms}")
    private long jwtExpirationMs;

    // ── Key derivation ────────────────────────────────────────────────────

    /**
     * Converts the raw string secret (from application.properties) into a
     * cryptographic SecretKey object that jjwt can use for signing.
     * The secret is decoded from base64 if it's base64-encoded, otherwise
     * we encode it first to meet the minimum key-length requirement.
     */
    private SecretKey getSigningKey() {
        byte[] keyBytes;
        try {
            keyBytes = Decoders.BASE64.decode(jwtSecret);
        } catch (Exception e) {
            // If the secret is a plain string (not base64), use its raw bytes
            keyBytes = jwtSecret.getBytes();
        }
        return Keys.hmacShaKeyFor(keyBytes);
    }

    // ── Token generation ──────────────────────────────────────────────────

    /**
     * Generates a JWT token for the given user.
     * The subject (sub) is set to the user's email.
     */
    public String generateToken(UserDetails userDetails) {
        return generateToken(new HashMap<>(), userDetails);
    }

    /**
     * Generates a token with additional custom claims (key-value pairs
     * embedded in the payload). Useful for adding role, userId, etc.
     */
    public String generateToken(Map<String, Object> extraClaims, UserDetails userDetails) {
        return Jwts.builder()
                .claims(extraClaims)
                .subject(userDetails.getUsername())     // username = email in our app
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + jwtExpirationMs))
                .signWith(getSigningKey())
                .compact();
    }

    // ── Token parsing ─────────────────────────────────────────────────────

    /**
     * Extracts the subject (email) from the JWT payload.
     */
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    /**
     * Extracts the expiration date from the JWT payload.
     */
    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    /**
     * Generic claim extractor. Uses a Function<Claims, T> to pull any field
     * from the parsed JWT claims object.
     */
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    /**
     * Parses and verifies the token signature. Throws JwtException subtypes if:
     * - signature is invalid (token tampered)
     * - token is expired
     * - token is malformed
     */
    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    // ── Token validation ──────────────────────────────────────────────────

    /**
     * Validates the token:
     * 1. Checks the username in the token matches the UserDetails loaded from DB
     * 2. Checks the token is not expired
     *
     * Why check both? The signature check already verifies tampering.
     * The username check ensures the token is for THIS user.
     * The expiry check ensures old tokens can't be reused.
     */
    public boolean isTokenValid(String token, UserDetails userDetails) {
        try {
            final String username = extractUsername(token);
            return username.equals(userDetails.getUsername()) && !isTokenExpired(token);
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("JWT validation failed: {}", e.getMessage());
            return false;
        }
    }

    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }
}
