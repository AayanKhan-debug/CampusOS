package com.campusos.service;

import com.campusos.dto.request.UpdateProfileRequest;
import com.campusos.dto.response.UserResponse;
import com.campusos.entity.User;
import com.campusos.exception.ResourceNotFoundException;
import com.campusos.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * UserService — handles user profile operations.
 *
 * getCurrentUser() is a utility used by both getMyProfile() and updateMyProfile().
 * It reads the currently authenticated user's email from Spring Security's
 * SecurityContextHolder, which was populated by JwtAuthFilter.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;

    /**
     * Gets the currently authenticated user's email from Spring Security context.
     * JwtAuthFilter placed the UserDetails into SecurityContextHolder,
     * and the username (principal) is the user's email.
     */
    private String getCurrentUserEmail() {
        return SecurityContextHolder.getContext()
                .getAuthentication()
                .getName();
    }

    /**
     * Returns the logged-in user's profile.
     * GET /api/users/me
     */
    public UserResponse getMyProfile() {
        String email = getCurrentUserEmail();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));
        return UserResponse.fromEntity(user);
    }

    /**
     * Updates the logged-in user's profile.
     * PUT /api/users/me
     *
     * Uses a "patch" style update: only non-null fields in the request are updated.
     * This means: if you send {"bio": "Hello"}, only bio changes. Other fields stay.
     *
     * @Transactional ensures that if anything goes wrong mid-update, the
     * entire operation is rolled back. Also, Hibernate will automatically
     * detect changes to the managed entity and issue an UPDATE statement.
     */
    @Transactional
    public UserResponse updateMyProfile(UpdateProfileRequest request) {
        String email = getCurrentUserEmail();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));

        // Only update fields that were provided (not null)
        if (request.getName() != null) {
            user.setName(request.getName());
        }
        if (request.getDepartment() != null) {
            user.setDepartment(request.getDepartment());
        }
        if (request.getYear() != null) {
            user.setYear(request.getYear());
        }
        if (request.getRollNumber() != null) {
            user.setRollNumber(request.getRollNumber());
        }
        if (request.getPhone() != null) {
            user.setPhone(request.getPhone());
        }
        if (request.getBio() != null) {
            user.setBio(request.getBio());
        }
        if (request.getSkills() != null) {
            user.setSkills(request.getSkills());
        }
        if (request.getInterests() != null) {
            user.setInterests(request.getInterests());
        }
        if (request.getProfilePicture() != null) {
            user.setProfilePicture(request.getProfilePicture());
        }

        // No explicit save() needed — @Transactional + managed entity = auto-save
        // But we call save() explicitly for clarity
        User updatedUser = userRepository.save(user);
        log.info("Profile updated for user: {}", email);

        return UserResponse.fromEntity(updatedUser);
    }
}
