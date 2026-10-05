package com.campusos.service;

import com.campusos.dto.request.CreateEventRequest;
import com.campusos.dto.request.UpdateEventRequest;
import com.campusos.dto.response.EventResponse;
import com.campusos.dto.response.PageResponse;
import com.campusos.entity.Club;
import com.campusos.entity.Event;
import com.campusos.entity.User;
import com.campusos.enums.*;
import com.campusos.exception.BadRequestException;
import com.campusos.exception.ResourceNotFoundException;
import com.campusos.exception.UnauthorizedException;
import com.campusos.repository.ClubRepository;
import com.campusos.repository.EventRegistrationRepository;
import com.campusos.repository.EventRepository;
import com.campusos.repository.UserRepository;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * EventService — handles event lifecycle: creation, querying, updating, and cancellation.
 *
 * Authorization rules:
 *   - ADMIN can create any event.
 *   - CLUB_ADMIN can create events for clubs they manage.
 *   - STUDENT cannot create events.
 *   - Only the event creator, their club's admin, or platform ADMIN can update/cancel an event.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EventService {

    private final EventRepository eventRepository;
    private final EventRegistrationRepository registrationRepository;
    private final ClubRepository clubRepository;
    private final UserRepository userRepository;

    /**
     * Helper to get the currently authenticated User entity from the SecurityContext.
     */
    public User getCurrentAuthenticatedUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));
    }

    /**
     * Checks if the user is authorized to manage an event.
     * Must be ADMIN, the event creator, or the creator of the event's club.
     */
    public boolean canManageEvent(User user, Event event) {
        if (user.getRole() == Role.ADMIN) {
            return true;
        }
        // Event creator
        if (event.getCreatedBy() != null && event.getCreatedBy().getId().equals(user.getId())) {
            return true;
        }
        // Club admin (creator of the associated club)
        if (event.getClub() != null && event.getClub().getCreatedBy() != null
                && event.getClub().getCreatedBy().getId().equals(user.getId())) {
            return true;
        }
        return false;
    }

    /**
     * Creates a new Event.
     * POST /api/events
     *
     * Business rules:
     * - STUDENT cannot create events.
     * - If clubId is provided, user must be the club's creator/admin or platform ADMIN.
     * - endTime must be after startTime.
     * - registrationDeadline must be before event date (if provided).
     */
    @Transactional
    public EventResponse createEvent(CreateEventRequest request) {
        User currentUser = getCurrentAuthenticatedUser();

        // Students cannot create events
        if (currentUser.getRole() == Role.STUDENT) {
            throw new UnauthorizedException("Students are not allowed to create events");
        }

        // Validate time constraints
        if (!request.getEndTime().isAfter(request.getStartTime())) {
            throw new BadRequestException("End time must be after start time");
        }

        // Validate registration deadline
        if (request.getRegistrationDeadline() != null) {
            LocalDate deadlineDate = request.getRegistrationDeadline().toLocalDate();
            if (!deadlineDate.isBefore(request.getDate()) && !deadlineDate.isEqual(request.getDate())) {
                // Deadline can be on or before the event date
            }
            if (request.getRegistrationDeadline().toLocalDate().isAfter(request.getDate())) {
                throw new BadRequestException("Registration deadline must be on or before the event date");
            }
        }

        Club club = null;
        if (request.getClubId() != null) {
            club = clubRepository.findById(request.getClubId())
                    .orElseThrow(() -> new ResourceNotFoundException("Club", "id", request.getClubId()));

            if (club.getStatus() == ClubStatus.INACTIVE) {
                throw new BadRequestException("Cannot create events for an inactive club");
            }

            // CLUB_ADMIN can only create events for their own club
            if (currentUser.getRole() == Role.CLUB_ADMIN) {
                if (club.getCreatedBy() == null || !club.getCreatedBy().getId().equals(currentUser.getId())) {
                    throw new UnauthorizedException("You can only create events for clubs you manage");
                }
            }
        }

        Event event = Event.builder()
                .title(request.getTitle().trim())
                .description(request.getDescription())
                .category(request.getCategory())
                .date(request.getDate())
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .location(request.getLocation().trim())
                .capacity(request.getCapacity())
                .registrationDeadline(request.getRegistrationDeadline())
                .image(request.getImage())
                .status(EventStatus.UPCOMING)
                .club(club)
                .createdBy(currentUser)
                .build();

        Event savedEvent = eventRepository.save(event);
        log.info("Event '{}' created by user {}", savedEvent.getTitle(), currentUser.getEmail());

        return EventResponse.fromEntity(savedEvent, 0, 0);
    }

    /**
     * Returns paginated events with optional filters.
     * GET /api/events
     *
     * Filters: category, status, location, date, search (across title and description).
     */
    @Transactional(readOnly = true)
    public PageResponse<EventResponse> getAllEvents(
            String search, EventCategory category, EventStatus status,
            String location, LocalDate date, Pageable pageable) {

        Specification<Event> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Search across title and description
            if (search != null && !search.isBlank()) {
                String searchPattern = "%" + search.trim().toLowerCase() + "%";
                Predicate titlePredicate = cb.like(cb.lower(root.get("title")), searchPattern);
                Predicate descPredicate = cb.like(cb.lower(root.get("description")), searchPattern);
                predicates.add(cb.or(titlePredicate, descPredicate));
            }

            // Filter by category
            if (category != null) {
                predicates.add(cb.equal(root.get("category"), category));
            }

            // Filter by status
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }

            // Filter by location
            if (location != null && !location.isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("location")), "%" + location.trim().toLowerCase() + "%"));
            }

            // Filter by date
            if (date != null) {
                predicates.add(cb.equal(root.get("date"), date));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<Event> eventPage = eventRepository.findAll(spec, pageable);

        List<EventResponse> content = eventPage.getContent().stream()
                .map(event -> {
                    long confirmedCount = registrationRepository.countByEventIdAndStatus(event.getId(), RegistrationStatus.CONFIRMED);
                    long waitlistedCount = registrationRepository.countByEventIdAndStatus(event.getId(), RegistrationStatus.WAITLISTED);
                    return EventResponse.fromEntity(event, confirmedCount, waitlistedCount);
                })
                .toList();

        return PageResponse.of(eventPage, content);
    }

    /**
     * Returns details of a specific event by ID.
     * GET /api/events/{id}
     */
    @Transactional(readOnly = true)
    public EventResponse getEventById(Long id) {
        Event event = eventRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Event", "id", id));
        long confirmedCount = registrationRepository.countByEventIdAndStatus(event.getId(), RegistrationStatus.CONFIRMED);
        long waitlistedCount = registrationRepository.countByEventIdAndStatus(event.getId(), RegistrationStatus.WAITLISTED);
        return EventResponse.fromEntity(event, confirmedCount, waitlistedCount);
    }

    /**
     * Updates an existing event's details.
     * PUT /api/events/{id}
     *
     * Only the event creator, club admin, or platform ADMIN can update.
     * Null fields in the request are ignored (patch-style).
     */
    @Transactional
    public EventResponse updateEvent(Long id, UpdateEventRequest request) {
        User currentUser = getCurrentAuthenticatedUser();
        Event event = eventRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Event", "id", id));

        if (!canManageEvent(currentUser, event)) {
            throw new UnauthorizedException("You do not have permission to update this event");
        }

        if (event.getStatus() == EventStatus.CANCELLED) {
            throw new BadRequestException("Cannot update a cancelled event");
        }

        // Apply non-null fields
        if (request.getTitle() != null && !request.getTitle().isBlank()) {
            event.setTitle(request.getTitle().trim());
        }
        if (request.getDescription() != null) {
            event.setDescription(request.getDescription());
        }
        if (request.getCategory() != null) {
            event.setCategory(request.getCategory());
        }
        if (request.getDate() != null) {
            event.setDate(request.getDate());
        }
        if (request.getStartTime() != null) {
            event.setStartTime(request.getStartTime());
        }
        if (request.getEndTime() != null) {
            event.setEndTime(request.getEndTime());
        }
        if (request.getLocation() != null && !request.getLocation().isBlank()) {
            event.setLocation(request.getLocation().trim());
        }
        if (request.getCapacity() != null) {
            event.setCapacity(request.getCapacity());
        }
        if (request.getRegistrationDeadline() != null) {
            event.setRegistrationDeadline(request.getRegistrationDeadline());
        }
        if (request.getImage() != null) {
            event.setImage(request.getImage());
        }
        if (request.getStatus() != null) {
            event.setStatus(request.getStatus());
        }

        // Validate time constraints after update
        if (event.getEndTime() != null && event.getStartTime() != null
                && !event.getEndTime().isAfter(event.getStartTime())) {
            throw new BadRequestException("End time must be after start time");
        }

        Event updatedEvent = eventRepository.save(event);
        long confirmedCount = registrationRepository.countByEventIdAndStatus(updatedEvent.getId(), RegistrationStatus.CONFIRMED);
        long waitlistedCount = registrationRepository.countByEventIdAndStatus(updatedEvent.getId(), RegistrationStatus.WAITLISTED);
        log.info("Event '{}' (id: {}) updated by user {}", updatedEvent.getTitle(), id, currentUser.getEmail());

        return EventResponse.fromEntity(updatedEvent, confirmedCount, waitlistedCount);
    }

    /**
     * Cancels an event (soft-delete by setting status to CANCELLED).
     * DELETE /api/events/{id}
     *
     * All CONFIRMED and WAITLISTED registrations are also marked as CANCELLED.
     */
    @Transactional
    public void cancelEvent(Long id) {
        User currentUser = getCurrentAuthenticatedUser();
        Event event = eventRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Event", "id", id));

        if (!canManageEvent(currentUser, event)) {
            throw new UnauthorizedException("You do not have permission to cancel this event");
        }

        if (event.getStatus() == EventStatus.CANCELLED) {
            throw new BadRequestException("Event is already cancelled");
        }

        // Soft cancel the event
        event.setStatus(EventStatus.CANCELLED);
        eventRepository.save(event);

        // Cancel all active registrations for this event
        List<com.campusos.entity.EventRegistration> activeRegistrations = new ArrayList<>();
        activeRegistrations.addAll(registrationRepository.findByEventIdAndStatus(id, RegistrationStatus.CONFIRMED));
        activeRegistrations.addAll(registrationRepository.findByEventIdAndStatus(id, RegistrationStatus.WAITLISTED));

        for (com.campusos.entity.EventRegistration reg : activeRegistrations) {
            reg.setStatus(RegistrationStatus.CANCELLED);
            reg.setCancelledAt(java.time.LocalDateTime.now());
        }
        registrationRepository.saveAll(activeRegistrations);

        log.info("Event '{}' (id: {}) cancelled by user {}. {} registrations cancelled.",
                event.getTitle(), id, currentUser.getEmail(), activeRegistrations.size());
    }
}
