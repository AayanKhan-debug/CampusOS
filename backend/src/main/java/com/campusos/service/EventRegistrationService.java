package com.campusos.service;

import com.campusos.dto.response.EventRegistrationResponse;
import com.campusos.dto.response.EventRegistrationStatusResponse;
import com.campusos.dto.response.PageResponse;
import com.campusos.entity.Event;
import com.campusos.entity.EventRegistration;
import com.campusos.entity.User;
import com.campusos.enums.EventStatus;
import com.campusos.enums.RegistrationStatus;
import com.campusos.enums.Role;
import com.campusos.exception.BadRequestException;
import com.campusos.exception.DuplicateResourceException;
import com.campusos.exception.ResourceNotFoundException;
import com.campusos.exception.UnauthorizedException;
import com.campusos.repository.EventRegistrationRepository;
import com.campusos.repository.EventRepository;
import com.campusos.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * EventRegistrationService — handles event registration, cancellation, and waitlist promotion.
 *
 * Concurrency strategy:
 *   Registration uses @Transactional + pessimistic locking (PESSIMISTIC_WRITE) on the
 *   confirmed-count query to prevent race conditions when multiple users register
 *   simultaneously for a capacity-limited event.
 *
 * Waitlist promotion:
 *   When a CONFIRMED user cancels, the earliest WAITLISTED user (by registeredAt ASC)
 *   is automatically promoted to CONFIRMED.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EventRegistrationService {

    private final EventRegistrationRepository registrationRepository;
    private final EventRepository eventRepository;
    private final UserRepository userRepository;

    /**
     * Helper to get the currently authenticated User entity.
     */
    private User getCurrentAuthenticatedUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));
    }

    /**
     * Registers the current user for an event.
     * POST /api/events/{eventId}/register
     *
     * Business rules:
     * 1. Event must exist and NOT be CANCELLED or COMPLETED.
     * 2. Registration deadline must not have passed (if set).
     * 3. Event date must not be in the past.
     * 4. User must not already have an active (non-cancelled) registration.
     * 5. If capacity is available → CONFIRMED.
     * 6. If capacity is full → WAITLISTED.
     * 7. Uses pessimistic locking on confirmed count to prevent race conditions.
     */
    @Transactional
    public EventRegistrationResponse registerForEvent(Long eventId) {
        User currentUser = getCurrentAuthenticatedUser();

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event", "id", eventId));

        // Validate event is open for registration
        if (event.getStatus() == EventStatus.CANCELLED) {
            throw new BadRequestException("Cannot register for a cancelled event");
        }
        if (event.getStatus() == EventStatus.COMPLETED) {
            throw new BadRequestException("Cannot register for a completed event");
        }

        // Check registration deadline
        if (event.getRegistrationDeadline() != null && LocalDateTime.now().isAfter(event.getRegistrationDeadline())) {
            throw new BadRequestException("Registration deadline has passed");
        }

        // Check event date is not in the past
        if (event.getDate().isBefore(LocalDate.now())) {
            throw new BadRequestException("Cannot register for a past event");
        }

        // Check for duplicate active registration
        Optional<EventRegistration> activeReg = registrationRepository.findActiveRegistration(eventId, currentUser.getId());
        if (activeReg.isPresent()) {
            throw new DuplicateResourceException("You already have an active registration for this event");
        }

        // Determine status: CONFIRMED or WAITLISTED
        RegistrationStatus registrationStatus;
        if (event.getCapacity() == null) {
            // Unlimited capacity → always CONFIRMED
            registrationStatus = RegistrationStatus.CONFIRMED;
        } else {
            // Use pessimistic lock to safely count confirmed registrations
            long confirmedCount = registrationRepository.countConfirmedWithLock(eventId);
            if (confirmedCount < event.getCapacity()) {
                registrationStatus = RegistrationStatus.CONFIRMED;
            } else {
                registrationStatus = RegistrationStatus.WAITLISTED;
            }
        }

        EventRegistration registration = EventRegistration.builder()
                .event(event)
                .user(currentUser)
                .status(registrationStatus)
                .registeredAt(LocalDateTime.now())
                .build();

        EventRegistration savedRegistration = registrationRepository.save(registration);

        log.info("User {} registered for event '{}' with status {}",
                currentUser.getEmail(), event.getTitle(), registrationStatus);

        return EventRegistrationResponse.fromEntity(savedRegistration);
    }

    /**
     * Cancels the current user's registration for an event.
     * DELETE /api/events/{eventId}/register
     *
     * Business rules:
     * 1. Registration must exist and not already be CANCELLED.
     * 2. If the cancelled registration was CONFIRMED → promote earliest WAITLISTED.
     * 3. If the cancelled registration was WAITLISTED → no promotion needed.
     */
    @Transactional
    public EventRegistrationStatusResponse cancelRegistration(Long eventId) {
        User currentUser = getCurrentAuthenticatedUser();

        EventRegistration registration = registrationRepository.findByEventIdAndUserId(eventId, currentUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Registration not found for event ID " + eventId));

        if (registration.getStatus() == RegistrationStatus.CANCELLED) {
            throw new BadRequestException("Registration is already cancelled");
        }

        boolean wasConfirmed = registration.getStatus() == RegistrationStatus.CONFIRMED;

        // Cancel the registration
        registration.setStatus(RegistrationStatus.CANCELLED);
        registration.setCancelledAt(LocalDateTime.now());
        registrationRepository.save(registration);

        log.info("User {} cancelled registration for event '{}' (was {})",
                currentUser.getEmail(), registration.getEvent().getTitle(),
                wasConfirmed ? "CONFIRMED" : "WAITLISTED");

        // Waitlist promotion: if a CONFIRMED spot was freed up, promote the earliest waitlisted user
        if (wasConfirmed) {
            promoteFromWaitlist(eventId);
        }

        return EventRegistrationStatusResponse.builder()
                .eventId(eventId)
                .status(RegistrationStatus.CANCELLED)
                .message("Registration cancelled successfully")
                .build();
    }

    /**
     * Promotes the earliest WAITLISTED user to CONFIRMED for a given event.
     * Called after a CONFIRMED user cancels their registration.
     */
    private void promoteFromWaitlist(Long eventId) {
        List<EventRegistration> waitlisted = registrationRepository
                .findWaitlistedByEventOrderByRegisteredAt(eventId);

        if (!waitlisted.isEmpty()) {
            EventRegistration toPromote = waitlisted.get(0);
            toPromote.setStatus(RegistrationStatus.CONFIRMED);
            registrationRepository.save(toPromote);

            log.info("Promoted user {} from WAITLISTED to CONFIRMED for event ID {}",
                    toPromote.getUser().getEmail(), eventId);
            // TODO: [Phase 7 Notification] Notify the promoted user
        }
    }

    /**
     * Returns all registrations for a specific event (admin view).
     * GET /api/events/{eventId}/registrations
     *
     * Only the event creator, club admin, or platform ADMIN can view.
     */
    @Transactional(readOnly = true)
    public PageResponse<EventRegistrationResponse> getEventRegistrations(Long eventId, Pageable pageable) {
        User currentUser = getCurrentAuthenticatedUser();

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event", "id", eventId));

        // Authorization: only event creator, club admin, or ADMIN
        if (!canManageEvent(currentUser, event)) {
            throw new UnauthorizedException("You do not have permission to view registrations for this event");
        }

        Page<EventRegistration> registrationPage = registrationRepository.findByEventId(eventId, pageable);
        List<EventRegistrationResponse> content = registrationPage.getContent().stream()
                .map(EventRegistrationResponse::fromEntity)
                .toList();

        return PageResponse.of(registrationPage, content);
    }

    /**
     * Returns the current user's registered events with optional status filter.
     * GET /api/users/me/events
     */
    @Transactional(readOnly = true)
    public PageResponse<EventRegistrationResponse> getMyRegisteredEvents(
            RegistrationStatus status, Pageable pageable) {
        User currentUser = getCurrentAuthenticatedUser();

        Page<EventRegistration> registrationPage;
        if (status != null) {
            registrationPage = registrationRepository.findByUserIdAndStatus(currentUser.getId(), status, pageable);
        } else {
            registrationPage = registrationRepository.findByUserId(currentUser.getId(), pageable);
        }

        List<EventRegistrationResponse> content = registrationPage.getContent().stream()
                .map(EventRegistrationResponse::fromEntity)
                .toList();

        return PageResponse.of(registrationPage, content);
    }

    /**
     * Checks if the user can manage the event (mirrors EventService.canManageEvent).
     */
    private boolean canManageEvent(User user, Event event) {
        if (user.getRole() == Role.ADMIN) {
            return true;
        }
        if (event.getCreatedBy() != null && event.getCreatedBy().getId().equals(user.getId())) {
            return true;
        }
        if (event.getClub() != null && event.getClub().getCreatedBy() != null
                && event.getClub().getCreatedBy().getId().equals(user.getId())) {
            return true;
        }
        return false;
    }
}
