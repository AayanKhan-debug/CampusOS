package com.campusos.entity;

import com.campusos.enums.EventCategory;
import com.campusos.enums.EventStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * Event entity — represents a campus event such as a workshop, hackathon, or cultural fest.
 *
 * Table: events
 *
 * Relationships:
 *   Event (ManyToOne) → Club (optional — only for club-organised events)
 *   Event (ManyToOne) → User (createdBy — the organiser)
 *   Event (OneToMany) → EventRegistration (registrations)
 *
 * Capacity management:
 *   - capacity field stores the max number of CONFIRMED registrations.
 *   - null capacity means unlimited.
 *   - Capacity checks use pessimistic locking in the service layer.
 */
@Entity
@Table(name = "events", indexes = {
    @Index(name = "idx_events_date", columnList = "date"),
    @Index(name = "idx_events_category", columnList = "category"),
    @Index(name = "idx_events_status", columnList = "status"),
    @Index(name = "idx_events_club", columnList = "club_id"),
    @Index(name = "idx_events_created_by", columnList = "created_by")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Event {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EventCategory category;

    /** Event date (the day the event happens) */
    @Column(nullable = false)
    private LocalDate date;

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;

    @Column(nullable = false, length = 200)
    private String location;

    /**
     * Maximum number of CONFIRMED registrations.
     * null → unlimited capacity (no cap).
     */
    private Integer capacity;

    /**
     * Deadline after which new registrations are not accepted.
     * null → registrations accepted until the event date.
     */
    @Column(name = "registration_deadline")
    private LocalDateTime registrationDeadline;

    @Column(length = 255)
    private String image;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private EventStatus status = EventStatus.UPCOMING;

    /**
     * Optional association with a club.
     * null → this is a general campus event, not tied to any club.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "club_id")
    private Club club;

    /**
     * The User (CLUB_ADMIN or ADMIN) who created/organises this event.
     */
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
