package com.campusos.controller;

import com.campusos.dto.request.*;
import com.campusos.dto.response.HelpDeskIssueResponse;
import com.campusos.dto.response.PageResponse;
import com.campusos.enums.IssueCategory;
import com.campusos.enums.IssuePriority;
import com.campusos.enums.IssueStatus;
import com.campusos.service.HelpDeskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * HelpDeskController — REST endpoints for campus issue reporting and resolution workflow.
 */
@RestController
@RequiredArgsConstructor
@Tag(name = "Campus Help Desk", description = "Campus issue reporting, tracking, assignment, and resolution")
@SecurityRequirement(name = "Bearer Authentication")
public class HelpDeskController {

    private final HelpDeskService helpDeskService;

    @PostMapping("/api/help-desk")
    @Operation(summary = "Submit issue", description = "Submit a campus issue. Initial status is SUBMITTED.")
    public ResponseEntity<HelpDeskIssueResponse> createIssue(
            @Valid @RequestBody CreateHelpDeskIssueRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(helpDeskService.createIssue(request));
    }

    @GetMapping("/api/help-desk")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "List all issues (Admin)", description = "Returns issues prioritized by URGENT -> HIGH -> MEDIUM -> LOW, then newest. Restricted to ADMIN.")
    public ResponseEntity<PageResponse<HelpDeskIssueResponse>> getAllIssues(
            @RequestParam(required = false) @Parameter(description = "Filter by status") IssueStatus status,
            @RequestParam(required = false) @Parameter(description = "Filter by category") IssueCategory category,
            @RequestParam(required = false) @Parameter(description = "Filter by priority") IssuePriority priority,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(helpDeskService.getAllIssues(status, category, priority, pageable));
    }

    @GetMapping("/api/users/me/issues")
    @Operation(summary = "Get my reported issues", description = "Returns issues submitted by the currently logged-in user.")
    public ResponseEntity<PageResponse<HelpDeskIssueResponse>> getMyIssues(
            @RequestParam(required = false) @Parameter(description = "Filter by status") IssueStatus status,
            @RequestParam(required = false) @Parameter(description = "Filter by category") IssueCategory category,
            @RequestParam(required = false) @Parameter(description = "Filter by priority") IssuePriority priority,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(helpDeskService.getUserIssues(status, category, priority, pageable));
    }

    @GetMapping("/api/help-desk/{id}")
    @Operation(summary = "Get issue by ID", description = "Fetch details of an issue. Restricted to reporter, assigned staff, or ADMIN.")
    public ResponseEntity<HelpDeskIssueResponse> getIssueById(@PathVariable Long id) {
        return ResponseEntity.ok(helpDeskService.getIssueById(id));
    }

    @PutMapping("/api/help-desk/{id}")
    @Operation(summary = "Update issue", description = "Update issue details. Reporter can only update while status is SUBMITTED. ADMIN can update anytime.")
    public ResponseEntity<HelpDeskIssueResponse> updateIssue(
            @PathVariable Long id,
            @Valid @RequestBody UpdateHelpDeskIssueRequest request) {
        return ResponseEntity.ok(helpDeskService.updateIssue(id, request));
    }

    @DeleteMapping("/api/help-desk/{id}")
    @Operation(summary = "Delete issue", description = "Delete issue. Reporter can only delete while status is SUBMITTED. ADMIN can delete anytime.")
    public ResponseEntity<Void> deleteIssue(@PathVariable Long id) {
        helpDeskService.deleteIssue(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/api/help-desk/{id}/assign")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Assign issue (Admin)", description = "Assign issue to staff/user and move status to IN_PROGRESS. Restricted to ADMIN.")
    public ResponseEntity<HelpDeskIssueResponse> assignIssue(
            @PathVariable Long id,
            @Valid @RequestBody AssignIssueRequest request) {
        return ResponseEntity.ok(helpDeskService.assignIssue(id, request));
    }

    @PatchMapping("/api/help-desk/{id}/status")
    @Operation(summary = "Update issue status", description = "ADMIN can update to any valid status. Reporter can close their own RESOLVED issue.")
    public ResponseEntity<HelpDeskIssueResponse> updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateIssueStatusRequest request) {
        return ResponseEntity.ok(helpDeskService.updateStatus(id, request));
    }

    @PatchMapping("/api/help-desk/{id}/resolve")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Resolve issue (Admin)", description = "Mark issue as RESOLVED with resolution notes. Restricted to ADMIN.")
    public ResponseEntity<HelpDeskIssueResponse> resolveIssue(
            @PathVariable Long id,
            @Valid @RequestBody ResolveIssueRequest request) {
        return ResponseEntity.ok(helpDeskService.resolveIssue(id, request));
    }
}
