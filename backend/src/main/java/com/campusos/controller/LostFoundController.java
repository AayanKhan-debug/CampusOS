package com.campusos.controller;

import com.campusos.dto.request.CreateLostFoundRequest;
import com.campusos.dto.request.UpdateLostFoundRequest;
import com.campusos.dto.request.UpdateLostFoundStatusRequest;
import com.campusos.dto.response.LostFoundResponse;
import com.campusos.dto.response.PageResponse;
import com.campusos.enums.ListingType;
import com.campusos.enums.LostFoundStatus;
import com.campusos.service.LostFoundService;
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
import org.springframework.web.bind.annotation.*;

/**
 * LostFoundController — REST endpoints for student Lost & Found listings.
 */
@RestController
@RequestMapping("/api/lost-found")
@RequiredArgsConstructor
@Tag(name = "Lost & Found", description = "Student Lost & Found item listings and status tracking")
@SecurityRequirement(name = "Bearer Authentication")
public class LostFoundController {

    private final LostFoundService lostFoundService;

    @PostMapping
    @Operation(summary = "Create listing", description = "Report a lost or found item. Reporter is set to current user.")
    public ResponseEntity<LostFoundResponse> createListing(
            @Valid @RequestBody CreateLostFoundRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(lostFoundService.createListing(request));
    }

    @GetMapping
    @Operation(summary = "List items", description = "Returns paginated items. Default feed shows OPEN and MATCHED, sorted by createdAt DESC.")
    public ResponseEntity<PageResponse<LostFoundResponse>> getAllListings(
            @RequestParam(required = false) @Parameter(description = "Filter by listing type (LOST / FOUND)") ListingType listingType,
            @RequestParam(required = false) @Parameter(description = "Filter by status (default: OPEN, MATCHED)") LostFoundStatus status,
            @RequestParam(required = false) @Parameter(description = "Search across title, description, itemType, location") String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return ResponseEntity.ok(lostFoundService.getAllListings(listingType, status, search, pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get listing by ID", description = "Fetch details of a single lost/found item.")
    public ResponseEntity<LostFoundResponse> getListingById(@PathVariable Long id) {
        return ResponseEntity.ok(lostFoundService.getListingById(id));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update listing", description = "Update details of a listing. Restricted to owner or ADMIN.")
    public ResponseEntity<LostFoundResponse> updateListing(
            @PathVariable Long id,
            @Valid @RequestBody UpdateLostFoundRequest request) {
        return ResponseEntity.ok(lostFoundService.updateListing(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete listing", description = "Delete a listing. Restricted to owner or ADMIN.")
    public ResponseEntity<Void> deleteListing(@PathVariable Long id) {
        lostFoundService.deleteListing(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Update listing status", description = "Update status according to allowed workflow transitions (OPEN -> MATCHED -> RECOVERED -> CLOSED). Restricted to owner or ADMIN.")
    public ResponseEntity<LostFoundResponse> updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateLostFoundStatusRequest request) {
        return ResponseEntity.ok(lostFoundService.updateStatus(id, request));
    }
}
