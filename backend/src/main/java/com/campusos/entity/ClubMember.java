package com.campusos.entity;

import com.campusos.enums.MembershipStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * ClubMember entity — represents the membership relationship between a User and a Club.
 *
 * Table: club_members
 *
 * Enforces a unique constraint on (club_id, user_id) at the database level so a user
 * cannot have duplicate membership rows for the same club even during concurrent requests.
 */
@Entity
@Table(
    name = "club_members",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_club_user", columnNames = {"club_id", "user_id"})
    },
    indexes = {
        @Index(name = "idx_club_member_club", columnList = "club_id"),
        @Index(name = "idx_club_member_user", columnList = "user_id"),
        @Index(name = "idx_club_member_status", columnList = "status")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClubMember {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "club_id", nullable = false)
    private Club club;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MembershipStatus status;

    /** Populated when status transitions to APPROVED */
    @Column(name = "joined_at")
    private LocalDateTime joinedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
