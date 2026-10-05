package com.campusos.service;

import com.campusos.dto.request.LoginRequest;
import com.campusos.dto.request.RegisterRequest;
import com.campusos.dto.response.AuthResponse;
import com.campusos.dto.response.UserResponse;
import com.campusos.entity.User;
import com.campusos.enums.Role;
import com.campusos.exception.DuplicateResourceException;
import com.campusos.repository.UserRepository;
import com.campusos.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * AuthService — handles user registration and login.
 *
 * This is the business logic layer. Controllers call this; this calls the
 * repository. Never put this logic directly in the controller.
 *
 * KEY DECISIONS:
 * - @Transactional on register() ensures the user is saved atomically.
 *   If anything fails after save (unlikely here, but good habit), the DB
 *   transaction rolls back and no partial user record is left.
 * - Password is hashed with BCrypt BEFORE saving. Never stored plain.
 * - All new users get the STUDENT role by default. ADMIN must manually
 *   elevate a user's role through the admin interface.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final AuthenticationManager authenticationManager;
    private final UserDetailsService userDetailsService;

    /**
     * Registers a new student account.
     *
     * Flow:
     * 1. Check if email is already taken → 409 Conflict if so
     * 2. Hash the password with BCrypt
     * 3. Build User entity with STUDENT role
     * 4. Save to database
     * 5. Generate JWT token
     * 6. Return token + user info
     */
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        // Step 1: Check for duplicate email
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException(
                    "An account with email '" + request.getEmail() + "' already exists"
            );
        }

        // Step 2 & 3: Build and save the user
        User user = User.builder()
                .name(request.getName())
                .email(request.getEmail().toLowerCase().trim())  // normalize email
                .password(passwordEncoder.encode(request.getPassword())) // hash it!
                .role(Role.STUDENT)  // default role
                .department(request.getDepartment())
                .year(request.getYear())
                .rollNumber(request.getRollNumber())
                .phone(request.getPhone())
                .build();

        User savedUser = userRepository.save(user);
        log.info("New student registered: {}", savedUser.getEmail());

        // Step 4: Load UserDetails and generate JWT
        UserDetails userDetails = userDetailsService.loadUserByUsername(savedUser.getEmail());
        String token = jwtUtil.generateToken(userDetails);

        // Step 5: Return the response
        return AuthResponse.builder()
                .token(token)
                .user(UserResponse.fromEntity(savedUser))
                .build();
    }

    /**
     * Authenticates an existing user and returns a JWT.
     *
     * Flow:
     * 1. Use Spring Security's AuthenticationManager to verify email + password
     *    - Internally: loads user via UserDetailsService, verifies BCrypt hash
     *    - Throws BadCredentialsException if wrong (caught in GlobalExceptionHandler)
     * 2. Generate JWT token
     * 3. Return token + user info
     *
     * Why use authenticationManager.authenticate() instead of manual BCrypt check?
     * Because Spring Security handles the full authentication lifecycle including
     * account locking, disabled accounts, etc. — extensible later.
     */
    public AuthResponse login(LoginRequest request) {
        // This line does all the heavy lifting:
        // - Loads user by email via CustomUserDetailsService
        // - Compares the provided password with the stored BCrypt hash
        // - Throws BadCredentialsException if they don't match
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail().toLowerCase().trim(),
                        request.getPassword()
                )
        );

        // If we reach here, authentication succeeded
        UserDetails userDetails = userDetailsService.loadUserByUsername(
                request.getEmail().toLowerCase().trim()
        );
        String token = jwtUtil.generateToken(userDetails);

        // Load the full user entity to include profile info in the response
        User user = userRepository.findByEmail(request.getEmail().toLowerCase().trim())
                .orElseThrow(); // Can't happen — we just authenticated this user

        log.info("User logged in: {}", user.getEmail());

        return AuthResponse.builder()
                .token(token)
                .user(UserResponse.fromEntity(user))
                .build();
    }
}
