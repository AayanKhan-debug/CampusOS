package com.campusos.entity;

import com.campusos.enums.Role;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * User entity — represents every person who has an account in CampusOS.
 *
 * Table: users
 *
 * Relationships (Phase 1 — others added in later phases):
 *   User → ClubMember (Phase 2)
 *   User → EventRegistration (Phase 3)
 *   User → LostFoundItem, CampusIssue, Notification (Phase 5/7)
 *
 * Key design decisions:
 * - Password is stored as a BCrypt hash — never plain text.
 * - Role is stored as a STRING enum (@Enumerated(EnumType.STRING)) so adding
 *   new roles later doesn't break existing data (ordinal-based enums are fragile).
 * - @CreationTimestamp / @UpdateTimestamp are Hibernate annotations that
 *   automatically populate createdAt/updatedAt when the entity is saved/updated.
 */
@Entity
@Table(name = "users", indexes = {
    @Index(name = "idx_users_email", columnList = "email")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Full display name */
    @Column(nullable = false, length = 100)
    private String name;

    /** Unique email — used as the login identifier */
    @Column(nullable = false, unique = true, length = 150)
    private String email;

    /** BCrypt-hashed password. NEVER expose this in API responses. */
    @Column(nullable = false)
    private String password;

    /**
     * Role stored as a string in the DB, not an integer.
     * EnumType.ORDINAL is dangerous because inserting a new enum value
     * in the middle would corrupt existing data.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role;

    // ── Optional profile fields (filled in after registration) ──

    @Column(length = 100)
    private String department;

    private Integer year;

    @Column(name = "roll_number", length = 50)
    private String rollNumber;

    @Column(length = 20)
    private String phone;

    @Column(columnDefinition = "TEXT")
    private String bio;

    /** Comma-separated list of skills (simple approach for Phase 1) */
    @Column(columnDefinition = "TEXT")
    private String skills;

    /** Comma-separated list of interests */
    @Column(columnDefinition = "TEXT")
    private String interests;

    @Column(name = "profile_picture", length = 255)
    private String profilePicture;

    /** Automatically set to current time when the entity is first persisted */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /** Automatically updated whenever the entity is saved */
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
