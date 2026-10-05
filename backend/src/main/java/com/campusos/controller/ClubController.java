package com.campusos.controller;

import com.campusos.dto.request.CreateClubRequest;
import com.campusos.dto.request.MembershipStatusUpdateRequest;
import com.campusos.dto.request.UpdateClubRequest;
import com.campusos.dto.response.ClubMemberResponse;
import com.campusos.dto.response.ClubResponse;
import com.campusos.dto.response.PageResponse;
import com.campusos.enums.ClubStatus;
import com.campusos.service.ClubMemberService;
import com.campusos.service.ClubService;
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
 * ClubController — REST endpoints for Clubs and Club Memberships.
 */
@RestController
@RequestMapping("/api/clubs")
@RequiredArgsConstructor
@Tag(name = "Clubs & Memberships", description = "Endpoints for student clubs, organizations, and membership management")
@SecurityRequirement(name = "Bearer Authentication")
public class ClubController {

    private final ClubService clubService;
    private final ClubMemberService clubMemberService;

    /**
     * POST /api/clubs
     * Creates a new club. Allowed for CLUB_ADMIN and ADMIN roles.
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('CLUB_ADMIN', 'ADMIN')")
    @Operation(summary = "Create a club", description = "Creates a new club organization. Restricted to CLUB_ADMIN and ADMIN.")
    public ResponseEntity<ClubResponse> createClub(@Valid @RequestBody CreateClubRequest request) {
        ClubResponse response = clubService.createClub(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * GET /api/clubs
     * Returns a paginated list of clubs with optional search, category, and status filters.
     */
    @GetMapping
    @Operation(summary = "Get clubs", description = "Returns a paginated list of clubs with search and filtering.")
    public ResponseEntity<PageResponse<ClubResponse>> getAllClubs(
            @RequestParam(required = false) @Parameter(description = "Search term in name or description") String search,
            @RequestParam(required = false) @Parameter(description = "Category filter") String category,
            @RequestParam(required = false) @Parameter(description = "Status filter (ACTIVE or INACTIVE)") ClubStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir
    ) {
        Sort sort = sortDir.equalsIgnoreCase("asc") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);
        return ResponseEntity.ok(clubService.getAllClubs(search, category, status, pageable));
    }

    /**
     * GET /api/clubs/{id}
     * Returns details of a specific club.
     */
    @GetMapping("/{id}")
    @Operation(summary = "Get club by ID", description = "Returns details for a specific club.")
    public ResponseEntity<ClubResponse> getClubById(@PathVariable Long id) {
        return ResponseEntity.ok(clubService.getClubById(id));
    }

    /**
     * PUT /api/clubs/{id}
     * Updates an existing club. Allowed for the club's creator/admin or platform ADMIN.
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('CLUB_ADMIN', 'ADMIN')")
    @Operation(summary = "Update club", description = "Updates club details. Restricted to the club creator and ADMIN.")
    public ResponseEntity<ClubResponse> updateClub(
            @PathVariable Long id,
            @Valid @RequestBody UpdateClubRequest request
    ) {
        return ResponseEntity.ok(clubService.updateClub(id, request));
    }

    /**
     * DELETE /api/clubs/{id}
     * Deactivates a club (soft delete).
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('CLUB_ADMIN', 'ADMIN')")
    @Operation(summary = "Deactivate club", description = "Soft-deactivates a club. Restricted to the club creator and ADMIN.")
    public ResponseEntity<Void> deleteClub(@PathVariable Long id) {
        clubService.deleteClub(id);
        return ResponseEntity.noContent().build();
    }

    // ── Membership Endpoints ───────────────────────────────────────────────

    /**
     * POST /api/clubs/{clubId}/join
     * Request to join a club (creates PENDING membership).
     */
    @PostMapping("/{clubId}/join")
    @Operation(summary = "Join a club", description = "Submits a membership request for a club. Status will be PENDING.")
    public ResponseEntity<ClubMemberResponse> joinClub(@PathVariable Long clubId) {
        ClubMemberResponse response = clubMemberService.joinClub(clubId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * GET /api/clubs/{clubId}/members
     * Returns approved members of a club.
     */
    @GetMapping("/{clubId}/members")
    @Operation(summary = "Get club members", description = "Returns a paginated list of approved club members.")
    public ResponseEntity<PageResponse<ClubMemberResponse>> getClubMembers(
            @PathVariable Long clubId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("joinedAt").descending());
        return ResponseEntity.ok(clubMemberService.getClubMembers(clubId, pageable));
    }

    /**
     * GET /api/clubs/{clubId}/members/requests
     * Returns pending membership requests for a club.
     * Restricted to the club creator and ADMIN.
     */
    @GetMapping("/{clubId}/members/requests")
    @PreAuthorize("hasAnyRole('CLUB_ADMIN', 'ADMIN')")
    @Operation(summary = "Get pending membership requests", description = "Returns pending join requests. Restricted to club admin and ADMIN.")
    public ResponseEntity<PageResponse<ClubMemberResponse>> getPendingRequests(
            @PathVariable Long clubId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").ascending());
        return ResponseEntity.ok(clubMemberService.getPendingRequests(clubId, pageable));
    }

    /**
     * PUT /api/clubs/{clubId}/members/{userId}
     * Approves or rejects a student's membership request.
     * Restricted to the club creator and ADMIN.
     */
    @PutMapping("/{clubId}/members/{userId}")
    @PreAuthorize("hasAnyRole('CLUB_ADMIN', 'ADMIN')")
    @Operation(summary = "Approve or reject membership", description = "Updates membership status (APPROVED or REJECTED). Restricted to club admin and ADMIN.")
    public ResponseEntity<ClubMemberResponse> updateMemberStatus(
            @PathVariable Long clubId,
            @PathVariable Long userId,
            @Valid @RequestBody MembershipStatusUpdateRequest request
    ) {
        return ResponseEntity.ok(clubMemberService.updateMemberStatus(clubId, userId, request));
    }
}
