package com.campusos.config;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * LoginRateLimiter — prevents brute-force attacks on the login endpoint.
 *
 * APPROACH: In-memory token bucket per IP address (using Bucket4j).
 *
 * TOKEN BUCKET algorithm explained:
 * - Imagine a bucket that holds a maximum of N tokens.
 * - The bucket refills at a steady rate (e.g., 5 tokens per minute).
 * - Each login attempt consumes 1 token.
 * - If the bucket is empty, the request is rejected with 429 Too Many Requests.
 *
 * This allows short bursts (a student typing wrong password a few times)
 * while blocking automated brute-force attempts (hundreds per second).
 *
 * Current limit: 5 login attempts per IP per minute.
 *
 * TRADE-OFFS of this in-memory approach:
 * ✅ Simple — no Redis or external service needed
 * ✅ Zero latency — no network call
 * ❌ Not distributed — if you run multiple server instances, each has its own
 *    counter. A determined attacker could bypass by hitting different instances.
 * ❌ Resets on restart — limits are lost when the server restarts.
 * → For production with multiple instances, use Redis-backed Bucket4j.
 *
 * We chose the simple in-memory approach because CampusOS starts as a
 * single-server application. Switching to Redis later is straightforward.
 */
@Component
public class LoginRateLimiter {

    // ConcurrentHashMap is thread-safe for concurrent request handling
    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    /**
     * Returns the token bucket for the given IP address.
     * Creates a new bucket if this IP hasn't been seen before.
     */
    public Bucket resolveBucket(String ipAddress) {
        return buckets.computeIfAbsent(ipAddress, this::createNewBucket);
    }

    private Bucket createNewBucket(String ipAddress) {
        // Allow 5 login attempts per minute per IP
        Bandwidth limit = Bandwidth.builder()
                .capacity(5)
                .refillGreedy(5, Duration.ofMinutes(1))
                .build();
        return Bucket.builder()
                .addLimit(limit)
                .build();
    }

    /**
     * Checks if the given IP can make a login attempt.
     * Returns true if allowed, false if rate limit exceeded.
     */
    public boolean tryConsume(String ipAddress) {
        return resolveBucket(ipAddress).tryConsume(1);
    }
}
