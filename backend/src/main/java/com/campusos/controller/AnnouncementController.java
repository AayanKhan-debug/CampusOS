package com.campusos.controller;

import com.campusos.dto.request.CreateAnnouncementRequest;
import com.campusos.dto.request.UpdateAnnouncementRequest;
import com.campusos.dto.response.AnnouncementReadResponse;
import com.campusos.dto.response.AnnouncementResponse;
import com.campusos.dto.response.PageResponse;
import com.campusos.enums.AnnouncementCategory;
import com.campusos.enums.AnnouncementStatus;
import com.campusos.service.AnnouncementReadService;
import com.campusos.service.AnnouncementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * AnnouncementController — REST endpoints for campus announcements.
 *
 * /api/announcements           — CRUD
 * /api/announcements/{id}/pin  — toggle pin
 * /api/announcements/{id}/archive — archive
 * /api/announcements/{id}/read    — mark as read
 * /api/users/me/announcements/unread — user's unread feed
 */
@RestController
@RequiredArgsConstructor
@Tag(name = "Announcements", description = "Campus announcement management and read tracking")
@SecurityRequirement(name = "Bearer Authentication")
public class AnnouncementController {

    private final AnnouncementService announcementService;
    private final AnnouncementReadService announcementReadService;

    // ── CRUD ──────────────────────────────────────────────────────

    @PostMapping("/api/announcements")
    @PreAuthorize("hasAnyRole('CLUB_ADMIN', 'ADMIN')")
    @Operation(summary = "Create announcement", description = "Creates a new announcement. Restricted to CLUB_ADMIN and ADMIN.")
    public ResponseEntity<AnnouncementResponse> createAnnouncement(
            @Valid @RequestBody CreateAnnouncementRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(announcementService.createAnnouncement(request));
    }

    @GetMapping("/api/announcements")
    @Operation(summary = "List announcements", description = "Returns paginated announcements. Default: ACTIVE only, pinned first. Supports search, category, status, pinned filters.")
    public ResponseEntity<PageResponse<AnnouncementResponse>> getAllAnnouncements(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) @Parameter(description = "Filter by category") AnnouncementCategory category,
            @RequestParam(required = false) @Parameter(description = "Filter by status (default: ACTIVE)") AnnouncementStatus status,
            @RequestParam(required = false) @Parameter(description = "Filter pinned only") Boolean pinned,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        // Pinned-first, then newest — enforce this sort in the query
        Pageable pageable = PageRequest.of(page, size,
                Sort.by("isPinned").descending().and(Sort.by("publishedAt").descending()));
        return ResponseEntity.ok(announcementService.getAllAnnouncements(search, category, status, pinned, pageable));
    }

    @GetMapping("/api/announcements/{id}")
    @Operation(summary = "Get announcement by ID")
    public ResponseEntity<AnnouncementResponse> getAnnouncementById(@PathVariable Long id) {
        return ResponseEntity.ok(announcementService.getAnnouncementById(id));
    }

    @PutMapping("/api/announcements/{id}")
    @PreAuthorize("hasAnyRole('CLUB_ADMIN', 'ADMIN')")
    @Operation(summary = "Update announcement", description = "Updates an announcement. Creator or ADMIN only.")
    public ResponseEntity<AnnouncementResponse> updateAnnouncement(
            @PathVariable Long id,
            @Valid @RequestBody UpdateAnnouncementRequest request) {
        return ResponseEntity.ok(announcementService.updateAnnouncement(id, request));
    }

    @DeleteMapping("/api/announcements/{id}")
    @PreAuthorize("hasAnyRole('CLUB_ADMIN', 'ADMIN')")
    @Operation(summary = "Delete announcement", description = "Permanently deletes an announcement. Creator or ADMIN only.")
    public ResponseEntity<Void> deleteAnnouncement(@PathVariable Long id) {
        announcementService.deleteAnnouncement(id);
        return ResponseEntity.noContent().build();
    }

    // ── Special Actions ───────────────────────────────────────────

    @PatchMapping("/api/announcements/{id}/pin")
    @PreAuthorize("hasAnyRole('CLUB_ADMIN', 'ADMIN')")
    @Operation(summary = "Toggle pin", description = "Toggles the pinned status of an announcement.")
    public ResponseEntity<AnnouncementResponse> togglePin(@PathVariable Long id) {
        return ResponseEntity.ok(announcementService.togglePin(id));
    }

    @PatchMapping("/api/announcements/{id}/archive")
    @PreAuthorize("hasAnyRole('CLUB_ADMIN', 'ADMIN')")
    @Operation(summary = "Archive announcement", description = "Archives an announcement, hiding it from the default feed.")
    public ResponseEntity<AnnouncementResponse> archiveAnnouncement(@PathVariable Long id) {
        return ResponseEntity.ok(announcementService.archiveAnnouncement(id));
    }

    // ── Read Tracking ─────────────────────────────────────────────

    @PostMapping("/api/announcements/{id}/read")
    @Operation(summary = "Mark as read", description = "Marks an announcement as read for the current user.")
    public ResponseEntity<AnnouncementReadResponse> markAsRead(@PathVariable Long id) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(announcementReadService.markAsRead(id));
    }

    @GetMapping("/api/users/me/announcements/unread")
    @Operation(summary = "Get unread announcements", description = "Returns the current user's unread active announcements, pinned first.")
    public ResponseEntity<PageResponse<AnnouncementResponse>> getUnreadAnnouncements(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(announcementService.getUnreadAnnouncements(pageable));
    }
}
