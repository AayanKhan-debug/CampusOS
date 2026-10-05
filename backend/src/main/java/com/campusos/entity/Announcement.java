package com.campusos.entity;

import com.campusos.enums.AnnouncementCategory;
import com.campusos.enums.AnnouncementStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * Announcement entity — represents a campus-wide notice published to students.
 *
 * Table: announcements
 *
 * Relationships:
 *   Announcement (ManyToOne) → User (createdBy)
 *   Announcement (OneToMany) → AnnouncementRead (read tracking)
 *
 * Business rules:
 *   - Pinned announcements appear before non-pinned in the default feed.
 *   - EXPIRED status is set when expiresAt < now.
 *   - ARCHIVED status is set manually by ADMIN.
 *   - Default feed shows only ACTIVE announcements.
 */
@Entity
@Table(name = "announcements", indexes = {
    @Index(name = "idx_announcement_category", columnList = "category"),
    @Index(name = "idx_announcement_status", columnList = "status"),
    @Index(name = "idx_announcement_pinned", columnList = "is_pinned"),
    @Index(name = "idx_announcement_published_at", columnList = "published_at"),
    @Index(name = "idx_announcement_expires_at", columnList = "expires_at"),
    @Index(name = "idx_announcement_created_by", columnList = "created_by")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Announcement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AnnouncementCategory category;

    @Column(length = 255)
    private String image;

    @Column(name = "is_pinned", nullable = false)
    @Builder.Default
    private boolean isPinned = false;

    @Column(name = "published_at", nullable = false)
    private LocalDateTime publishedAt;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private AnnouncementStatus status = AnnouncementStatus.ACTIVE;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
