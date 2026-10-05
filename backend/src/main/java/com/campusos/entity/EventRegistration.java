package com.campusos.entity;

import com.campusos.enums.RegistrationStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * EventRegistration entity — represents a user's registration for an event.
 *
 * Table: event_registrations
 *
 * Enforces a unique constraint on (event_id, user_id) at the database level so a user
 * cannot have duplicate registration rows for the same event even during concurrent requests.
 *
 * Business rules:
 *   - CONFIRMED when capacity is available at the time of registration.
 *   - WAITLISTED when the event is at full capacity.
 *   - CANCELLED when the user cancels their registration.
 *   - When a CONFIRMED user cancels, the earliest WAITLISTED user is promoted to CONFIRMED.
 */
@Entity
@Table(
    name = "event_registrations",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_event_user", columnNames = {"event_id", "user_id"})
    },
    indexes = {
        @Index(name = "idx_event_reg_event", columnList = "event_id"),
        @Index(name = "idx_event_reg_user", columnList = "user_id"),
        @Index(name = "idx_event_reg_status", columnList = "status")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EventRegistration {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RegistrationStatus status;

    @Column(name = "registered_at", nullable = false)
    private LocalDateTime registeredAt;

    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
