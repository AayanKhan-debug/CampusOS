package com.campusos.entity;

import com.campusos.enums.ClubStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * Club entity — represents a student organization, society, or interest group.
 *
 * Table: clubs
 *
 * Relationships:
 *   Club (ManyToOne) → User (createdBy)
 *   Club (OneToMany) → ClubMember (members)
 *   Club (OneToMany) → Event (events - Phase 3)
 */
@Entity
@Table(name = "clubs", indexes = {
    @Index(name = "idx_clubs_name", columnList = "name"),
    @Index(name = "idx_clubs_status", columnList = "status"),
    @Index(name = "idx_clubs_category", columnList = "category")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Club {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(length = 50)
    private String category;

    @Column(length = 255)
    private String logo;

    /**
     * The User (CLUB_ADMIN or ADMIN) who founded/manages this club.
     * Lazy fetching avoids loading the entire user entity until explicitly requested.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private ClubStatus status = ClubStatus.ACTIVE;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
