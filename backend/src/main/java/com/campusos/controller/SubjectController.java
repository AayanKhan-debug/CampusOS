package com.campusos.controller;

import com.campusos.dto.request.CreateSubjectRequest;
import com.campusos.dto.request.UpdateSubjectRequest;
import com.campusos.dto.response.PageResponse;
import com.campusos.dto.response.SubjectResponse;
import com.campusos.service.SubjectService;
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

@RestController
@RequestMapping("/api/subjects")
@RequiredArgsConstructor
@Tag(name = "Resource Hub - Subjects", description = "Academic subject management")
@SecurityRequirement(name = "Bearer Authentication")
public class SubjectController {

    private final SubjectService subjectService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create subject", description = "Creates a new subject in a course. ADMIN only.")
    public ResponseEntity<SubjectResponse> createSubject(@Valid @RequestBody CreateSubjectRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(subjectService.createSubject(request));
    }

    @GetMapping
    @Operation(summary = "List subjects", description = "Returns subjects. Optional: filter by courseId and/or semester.")
    public ResponseEntity<PageResponse<SubjectResponse>> getAllSubjects(
            @RequestParam(required = false) @Parameter(description = "Filter by course ID") Long courseId,
            @RequestParam(required = false) @Parameter(description = "Filter by semester (1-10)") Integer semester,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(subjectService.getAllSubjects(courseId, semester,
                PageRequest.of(page, size, Sort.by("semester").ascending().and(Sort.by("name").ascending()))));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get subject by ID")
    public ResponseEntity<SubjectResponse> getSubjectById(@PathVariable Long id) {
        return ResponseEntity.ok(subjectService.getSubjectById(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update subject", description = "Updates a subject. ADMIN only.")
    public ResponseEntity<SubjectResponse> updateSubject(
            @PathVariable Long id, @Valid @RequestBody UpdateSubjectRequest request) {
        return ResponseEntity.ok(subjectService.updateSubject(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete subject", description = "Deletes a subject. ADMIN only.")
    public ResponseEntity<Void> deleteSubject(@PathVariable Long id) {
        subjectService.deleteSubject(id);
        return ResponseEntity.noContent().build();
    }
}
