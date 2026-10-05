package com.campusos.dto.response;

import com.campusos.entity.User;
import com.campusos.enums.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * UserResponse — safe public representation of a User.
 *
 * This is what we send back in API responses.
 * The raw User entity is NEVER returned directly because:
 *   1. It would expose the password hash.
 *   2. It couples API contracts to the DB schema.
 *   3. Jackson could accidentally serialize lazy-loaded collections.
 *
 * The static fromEntity() factory method converts a User → UserResponse.
 * This keeps mapping logic in one place.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {

    private Long id;
    private String name;
    private String email;
    private Role role;
    private String department;
    private Integer year;
    private String rollNumber;
    private String phone;
    private String bio;
    private String skills;
    private String interests;
    private String profilePicture;
    private LocalDateTime createdAt;

    /**
     * Converts a User entity to a UserResponse DTO.
     * Call this instead of manually mapping fields everywhere.
     */
    public static UserResponse fromEntity(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .role(user.getRole())
                .department(user.getDepartment())
                .year(user.getYear())
                .rollNumber(user.getRollNumber())
                .phone(user.getPhone())
                .bio(user.getBio())
                .skills(user.getSkills())
                .interests(user.getInterests())
                .profilePicture(user.getProfilePicture())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
