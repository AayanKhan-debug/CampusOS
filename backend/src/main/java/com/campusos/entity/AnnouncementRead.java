package com.campusos.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * AnnouncementRead entity — tracks which users have read which announcements.
 *
 * Table: announcement_reads
 *
 * Unique constraint on (announcement_id, user_id) ensures a user cannot
 * create duplicate read records for the same announcement.
 */
@Entity
@Table(
    name = "announcement_reads",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_announcement_user_read", columnNames = {"announcement_id", "user_id"})
    },
    indexes = {
        @Index(name = "idx_ann_read_announcement", columnList = "announcement_id"),
        @Index(name = "idx_ann_read_user", columnList = "user_id")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AnnouncementRead {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "announcement_id", nullable = false)
    private Announcement announcement;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "read_at", nullable = false)
    private LocalDateTime readAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
