package com.campusos.controller;

import com.campusos.dto.request.CreateEventRequest;
import com.campusos.dto.request.UpdateEventRequest;
import com.campusos.dto.response.*;
import com.campusos.enums.EventCategory;
import com.campusos.enums.EventStatus;
import com.campusos.enums.RegistrationStatus;
import com.campusos.service.EventRegistrationService;
import com.campusos.service.EventService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

/**
 * EventController — REST endpoints for Events and Event Registration.
 *
 * Event CRUD: /api/events
 * Event Registration: /api/events/{id}/register, /api/events/{id}/registrations
 * User's events: /api/users/me/events (handled by UserController or here)
 */
@RestController
@RequiredArgsConstructor
@Tag(name = "Events", description = "Endpoints for campus event management and registration")
@SecurityRequirement(name = "Bearer Authentication")
public class EventController {

    private final EventService eventService;
    private final EventRegistrationService registrationService;

    // ══════════════════════════════════════════════════════════════
    //  EVENT CRUD
    // ══════════════════════════════════════════════════════════════

    /**
     * POST /api/events
     * Creates a new event. CLUB_ADMIN (for their clubs) and ADMIN only.
     * STUDENT cannot create events — enforced in EventService.
     */
    @PostMapping("/api/events")
    @PreAuthorize("hasAnyRole('CLUB_ADMIN', 'ADMIN')")
    @Operation(summary = "Create an event", description = "Creates a new campus event. Restricted to CLUB_ADMIN and ADMIN.")
    public ResponseEntity<EventResponse> createEvent(@Valid @RequestBody CreateEventRequest request) {
        EventResponse response = eventService.createEvent(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * GET /api/events
     * Returns a paginated list of events with optional filters.
     */
    @GetMapping("/api/events")
    @Operation(summary = "Get events", description = "Returns a paginated list of events with search, category, status, location, and date filters.")
    public ResponseEntity<PageResponse<EventResponse>> getAllEvents(
            @RequestParam(required = false) @Parameter(description = "Search term in title or description") String search,
            @RequestParam(required = false) @Parameter(description = "Category filter") EventCategory category,
            @RequestParam(required = false) @Parameter(description = "Status filter") EventStatus status,
            @RequestParam(required = false) @Parameter(description = "Location filter") String location,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) @Parameter(description = "Date filter (yyyy-MM-dd)") LocalDate date,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "date") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir
    ) {
        Sort sort = sortDir.equalsIgnoreCase("desc") ? Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);
        return ResponseEntity.ok(eventService.getAllEvents(search, category, status, location, date, pageable));
    }

    /**
     * GET /api/events/{id}
     * Returns details of a specific event.
     */
    @GetMapping("/api/events/{id}")
    @Operation(summary = "Get event by ID", description = "Returns details for a specific event.")
    public ResponseEntity<EventResponse> getEventById(@PathVariable Long id) {
        return ResponseEntity.ok(eventService.getEventById(id));
    }

    /**
     * PUT /api/events/{id}
     * Updates an existing event. Only the event creator, club admin, or platform ADMIN.
     */
    @PutMapping("/api/events/{id}")
    @PreAuthorize("hasAnyRole('CLUB_ADMIN', 'ADMIN')")
    @Operation(summary = "Update event", description = "Updates event details. Restricted to event creator, club admin, and ADMIN.")
    public ResponseEntity<EventResponse> updateEvent(
            @PathVariable Long id,
            @Valid @RequestBody UpdateEventRequest request
    ) {
        return ResponseEntity.ok(eventService.updateEvent(id, request));
    }

    /**
     * DELETE /api/events/{id}
     * Cancels an event (soft cancel — sets status to CANCELLED and cancels all registrations).
     */
    @DeleteMapping("/api/events/{id}")
    @PreAuthorize("hasAnyRole('CLUB_ADMIN', 'ADMIN')")
    @Operation(summary = "Cancel event", description = "Soft-cancels an event and all its registrations. Restricted to event creator, club admin, and ADMIN.")
    public ResponseEntity<Void> cancelEvent(@PathVariable Long id) {
        eventService.cancelEvent(id);
        return ResponseEntity.noContent().build();
    }

    // ══════════════════════════════════════════════════════════════
    //  EVENT REGISTRATION
    // ══════════════════════════════════════════════════════════════

    /**
     * POST /api/events/{id}/register
     * Registers the authenticated user for an event.
     * CONFIRMED if capacity available, WAITLISTED if full.
     */
    @PostMapping("/api/events/{id}/register")
    @Operation(summary = "Register for event", description = "Registers the current user for an event. Returns CONFIRMED or WAITLISTED status.")
    public ResponseEntity<EventRegistrationResponse> registerForEvent(@PathVariable Long id) {
        EventRegistrationResponse response = registrationService.registerForEvent(id);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * DELETE /api/events/{id}/register
     * Cancels the authenticated user's registration.
     * If the user was CONFIRMED, the earliest WAITLISTED user is promoted.
     */
    @DeleteMapping("/api/events/{id}/register")
    @Operation(summary = "Cancel registration", description = "Cancels the current user's event registration. Promotes waitlisted users if applicable.")
    public ResponseEntity<EventRegistrationStatusResponse> cancelRegistration(@PathVariable Long id) {
        return ResponseEntity.ok(registrationService.cancelRegistration(id));
    }

    /**
     * GET /api/events/{id}/registrations
     * Returns all registrations for an event (admin view).
     * Only event creator, club admin, or platform ADMIN.
     */
    @GetMapping("/api/events/{id}/registrations")
    @PreAuthorize("hasAnyRole('CLUB_ADMIN', 'ADMIN')")
    @Operation(summary = "Get event registrations", description = "Returns all registrations for an event. Restricted to event creator, club admin, and ADMIN.")
    public ResponseEntity<PageResponse<EventRegistrationResponse>> getEventRegistrations(
            @PathVariable Long id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("registeredAt").ascending());
        return ResponseEntity.ok(registrationService.getEventRegistrations(id, pageable));
    }

    /**
     * GET /api/users/me/events
     * Returns the authenticated user's event registrations with optional status filter.
     */
    @GetMapping("/api/users/me/events")
    @Operation(summary = "Get my event registrations", description = "Returns the current user's event registrations, filterable by status.")
    public ResponseEntity<PageResponse<EventRegistrationResponse>> getMyEvents(
            @RequestParam(required = false) @Parameter(description = "Filter by registration status") RegistrationStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("registeredAt").descending());
        return ResponseEntity.ok(registrationService.getMyRegisteredEvents(status, pageable));
    }
}
