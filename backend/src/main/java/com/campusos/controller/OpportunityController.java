package com.campusos.controller;

import com.campusos.dto.request.CreateOpportunityRequest;
import com.campusos.dto.request.UpdateOpportunityRequest;
import com.campusos.dto.response.OpportunityResponse;
import com.campusos.dto.response.PageResponse;
import com.campusos.enums.OpportunityType;
import com.campusos.service.OpportunityService;
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
 * OpportunityController — REST endpoints for student opportunities (internships, hackathons, etc.).
 */
@RestController
@RequestMapping("/api/opportunities")
@RequiredArgsConstructor
@Tag(name = "Opportunities", description = "Student internships, competitions, scholarships, and opportunities")
@SecurityRequirement(name = "Bearer Authentication")
public class OpportunityController {

    private final OpportunityService opportunityService;

    @PostMapping
    @PreAuthorize("hasAnyRole('CLUB_ADMIN', 'ADMIN')")
    @Operation(summary = "Create opportunity", description = "Create a new opportunity. Restricted to CLUB_ADMIN and ADMIN.")
    public ResponseEntity<OpportunityResponse> createOpportunity(
            @Valid @RequestBody CreateOpportunityRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(opportunityService.createOpportunity(request));
    }

    @GetMapping
    @Operation(summary = "List opportunities", description = "Returns active opportunities (status OPEN and deadline not expired). Nearest deadline first by default.")
    public ResponseEntity<PageResponse<OpportunityResponse>> getAllOpportunities(
            @RequestParam(required = false) @Parameter(description = "Filter by opportunity type") OpportunityType type,
            @RequestParam(required = false) @Parameter(description = "Search across title, description, organization") String search,
            @RequestParam(required = false) @Parameter(description = "Filter by remote eligibility") Boolean remote,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) @Parameter(description = "Sort field (e.g. deadline, createdAt)") String sort) {

        Sort sortObj = Sort.by(Sort.Direction.ASC, "deadline");
        if (sort != null && !sort.trim().isEmpty()) {
            sortObj = Sort.by(sort.trim());
        }
        Pageable pageable = PageRequest.of(page, size, sortObj);

        return ResponseEntity.ok(opportunityService.getAllOpportunities(type, search, remote, pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get opportunity by ID", description = "Fetch details of a single opportunity.")
    public ResponseEntity<OpportunityResponse> getOpportunityById(@PathVariable Long id) {
        return ResponseEntity.ok(opportunityService.getOpportunityById(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('CLUB_ADMIN', 'ADMIN')")
    @Operation(summary = "Update opportunity", description = "Update an opportunity. Restricted to creating CLUB_ADMIN or ADMIN.")
    public ResponseEntity<OpportunityResponse> updateOpportunity(
            @PathVariable Long id,
            @Valid @RequestBody UpdateOpportunityRequest request) {
        return ResponseEntity.ok(opportunityService.updateOpportunity(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('CLUB_ADMIN', 'ADMIN')")
    @Operation(summary = "Delete opportunity", description = "Delete an opportunity. Restricted to creating CLUB_ADMIN or ADMIN.")
    public ResponseEntity<Void> deleteOpportunity(@PathVariable Long id) {
        opportunityService.deleteOpportunity(id);
        return ResponseEntity.noContent().build();
    }
}
