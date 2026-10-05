package com.campusos.repository;

import com.campusos.entity.EventRegistration;
import com.campusos.enums.RegistrationStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * EventRegistrationRepository — data access for EventRegistration entities.
 *
 * Includes a pessimistic-locking confirmed-count query to safely handle
 * concurrent registration requests against a capacity-limited event.
 */
@Repository
public interface EventRegistrationRepository extends JpaRepository<EventRegistration, Long> {

    /** Find a user's registration for a specific event */
    Optional<EventRegistration> findByEventIdAndUserId(Long eventId, Long userId);

    /** Check if a registration exists for a user in an event */
    boolean existsByEventIdAndUserId(Long eventId, Long userId);

    /**
     * Count CONFIRMED registrations for an event — with pessimistic write lock.
     * This prevents race conditions when multiple users register concurrently
     * for a capacity-limited event.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT COUNT(er) FROM EventRegistration er WHERE er.event.id = :eventId AND er.status = 'CONFIRMED'")
    long countConfirmedWithLock(@Param("eventId") Long eventId);

    /** Count registrations for an event with a specific status (no lock) */
    long countByEventIdAndStatus(Long eventId, RegistrationStatus status);

    /**
     * Find the earliest WAITLISTED registration for an event.
     * Ordered by registeredAt ASC so the person who registered first gets promoted first.
     */
    @Query("SELECT er FROM EventRegistration er WHERE er.event.id = :eventId AND er.status = 'WAITLISTED' ORDER BY er.registeredAt ASC")
    List<EventRegistration> findWaitlistedByEventOrderByRegisteredAt(@Param("eventId") Long eventId);

    /** Find all registrations for an event with pagination */
    Page<EventRegistration> findByEventId(Long eventId, Pageable pageable);

    /** Find all registrations for a user with pagination */
    Page<EventRegistration> findByUserId(Long userId, Pageable pageable);

    /** Find registrations for a user filtered by status */
    Page<EventRegistration> findByUserIdAndStatus(Long userId, RegistrationStatus status, Pageable pageable);

    /**
     * Find a user's active (non-cancelled) registration for an event.
     * Used to prevent duplicate active registrations.
     */
    @Query("SELECT er FROM EventRegistration er WHERE er.event.id = :eventId AND er.user.id = :userId AND er.status <> 'CANCELLED'")
    Optional<EventRegistration> findActiveRegistration(@Param("eventId") Long eventId, @Param("userId") Long userId);

    /** Find all registrations for an event with a specific status */
    List<EventRegistration> findByEventIdAndStatus(Long eventId, RegistrationStatus status);
}
