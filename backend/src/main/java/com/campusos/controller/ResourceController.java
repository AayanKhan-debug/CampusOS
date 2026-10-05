package com.campusos.controller;

import com.campusos.dto.request.CreateResourceRequest;
import com.campusos.dto.request.UpdateResourceRequest;
import com.campusos.dto.response.PageResponse;
import com.campusos.dto.response.ResourceResponse;
import com.campusos.enums.ResourceType;
import com.campusos.service.ResourceHubService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * ResourceController — REST endpoints for the Resource Hub.
 *
 * Students can browse; CLUB_ADMIN and ADMIN can upload/manage.
 */
@RestController
@RequestMapping("/api/resources")
@RequiredArgsConstructor
@Tag(name = "Resource Hub - Resources", description = "Academic resource management (notes, papers, assignments, etc.)")
@SecurityRequirement(name = "Bearer Authentication")
public class ResourceController {

    private final ResourceHubService resourceHubService;

    @PostMapping
    @PreAuthorize("hasAnyRole('CLUB_ADMIN', 'ADMIN')")
    @Operation(summary = "Upload resource", description = "Uploads a new academic resource. CLUB_ADMIN and ADMIN only.")
    public ResponseEntity<ResourceResponse> createResource(@Valid @RequestBody CreateResourceRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(resourceHubService.createResource(request));
    }

    @GetMapping
    @Operation(summary = "List resources", description = "Returns paginated resources. Optional filters: subjectId, resourceType, search.")
    public ResponseEntity<PageResponse<ResourceResponse>> getAllResources(
            @RequestParam(required = false) @Parameter(description = "Filter by subject ID") Long subjectId,
            @RequestParam(required = false) @Parameter(description = "Filter by resource type") ResourceType resourceType,
            @RequestParam(required = false) @Parameter(description = "Search in title/description") String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(resourceHubService.getAllResources(subjectId, resourceType, search,
                PageRequest.of(page, size, Sort.by("createdAt").descending())));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get resource by ID")
    public ResponseEntity<ResourceResponse> getResourceById(@PathVariable Long id) {
        return ResponseEntity.ok(resourceHubService.getResourceById(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('CLUB_ADMIN', 'ADMIN')")
    @Operation(summary = "Update resource", description = "Updates a resource. Uploader or ADMIN only.")
    public ResponseEntity<ResourceResponse> updateResource(
            @PathVariable Long id, @Valid @RequestBody UpdateResourceRequest request) {
        return ResponseEntity.ok(resourceHubService.updateResource(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('CLUB_ADMIN', 'ADMIN')")
    @Operation(summary = "Delete resource", description = "Deletes a resource. Uploader or ADMIN only.")
    public ResponseEntity<Void> deleteResource(@PathVariable Long id) {
        resourceHubService.deleteResource(id);
        return ResponseEntity.noContent().build();
    }
}
