package com.campusos.service;

import com.campusos.dto.request.*;
import com.campusos.dto.response.HelpDeskIssueResponse;
import com.campusos.dto.response.PageResponse;
import com.campusos.entity.HelpDeskIssue;
import com.campusos.entity.User;
import com.campusos.enums.IssueCategory;
import com.campusos.enums.IssuePriority;
import com.campusos.enums.IssueStatus;
import com.campusos.enums.Role;
import com.campusos.exception.BadRequestException;
import com.campusos.exception.ResourceNotFoundException;
import com.campusos.exception.UnauthorizedException;
import com.campusos.repository.HelpDeskRepository;
import com.campusos.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * HelpDeskService — business logic for campus issue reporting and resolution workflow.
 *
 * Workflow:
 *   SUBMITTED -> IN_PROGRESS -> RESOLVED -> CLOSED
 *
 * Role rules:
 * - Any user can report an issue (starts as SUBMITTED).
 * - Students can update/delete their own issue only while it is SUBMITTED.
 * - Students can close their own issue once it has been marked RESOLVED.
 * - Only ADMIN can assign issues, change in-progress status, and resolve issues.
 * - Admin issue listing prioritizes: URGENT -> HIGH -> MEDIUM -> LOW, then newest first.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class HelpDeskService {

    private final HelpDeskRepository helpDeskRepository;
    private final UserRepository userRepository;

    public User getCurrentAuthenticatedUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));
    }

    /**
     * Submit a new campus issue.
     */
    @Transactional
    public HelpDeskIssueResponse createIssue(CreateHelpDeskIssueRequest request) {
        User currentUser = getCurrentAuthenticatedUser();

        HelpDeskIssue issue = HelpDeskIssue.builder()
                .title(request.getTitle().trim())
                .description(request.getDescription().trim())
                .category(request.getCategory())
                .location(request.getLocation().trim())
                .priority(request.getPriority())
                .status(IssueStatus.SUBMITTED)
                .reportedBy(currentUser)
                .build();

        HelpDeskIssue saved = helpDeskRepository.save(issue);
        log.info("Help desk issue '{}' (ID: {}) reported by {}", saved.getTitle(), saved.getId(), currentUser.getEmail());
        return HelpDeskIssueResponse.fromEntity(saved);
    }

    /**
     * Get paginated issues for admin, prioritized by URGENT -> HIGH -> MEDIUM -> LOW, then newest first.
     */
    @Transactional(readOnly = true)
    public PageResponse<HelpDeskIssueResponse> getAllIssues(
            IssueStatus status,
            IssueCategory category,
            IssuePriority priority,
            Pageable pageable) {

        Page<HelpDeskIssue> page = helpDeskRepository.findWithFiltersAndPrioritySort(status, category, priority, pageable);
        List<HelpDeskIssueResponse> responses = page.getContent().stream()
                .map(HelpDeskIssueResponse::fromEntity)
                .toList();

        return PageResponse.of(page, responses);
    }

    /**
     * Get issues reported by current user.
     */
    @Transactional(readOnly = true)
    public PageResponse<HelpDeskIssueResponse> getUserIssues(
            IssueStatus status,
            IssueCategory category,
            IssuePriority priority,
            Pageable pageable) {

        User currentUser = getCurrentAuthenticatedUser();
        Page<HelpDeskIssue> page = helpDeskRepository.findByReportedByIdWithFilters(
                currentUser.getId(), status, category, priority, pageable);

        List<HelpDeskIssueResponse> responses = page.getContent().stream()
                .map(HelpDeskIssueResponse::fromEntity)
                .toList();

        return PageResponse.of(page, responses);
    }

    /**
     * Get single issue by ID.
     * Accessible by ADMIN, assigned user, or the reporting user.
     */
    @Transactional(readOnly = true)
    public HelpDeskIssueResponse getIssueById(Long id) {
        User currentUser = getCurrentAuthenticatedUser();
        HelpDeskIssue issue = helpDeskRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("HelpDeskIssue", "id", id));

        boolean isReporter = issue.getReportedBy() != null && issue.getReportedBy().getId().equals(currentUser.getId());
        boolean isAssigned = issue.getAssignedTo() != null && issue.getAssignedTo().getId().equals(currentUser.getId());
        boolean isAdmin = currentUser.getRole() == Role.ADMIN;

        if (!isAdmin && !isReporter && !isAssigned) {
            throw new UnauthorizedException("You do not have permission to view this issue");
        }

        return HelpDeskIssueResponse.fromEntity(issue);
    }

    /**
     * Update issue details.
     * Reporter can only update while status is SUBMITTED.
     * ADMIN can update anytime.
     */
    @Transactional
    public HelpDeskIssueResponse updateIssue(Long id, UpdateHelpDeskIssueRequest request) {
        User currentUser = getCurrentAuthenticatedUser();
        HelpDeskIssue issue = helpDeskRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("HelpDeskIssue", "id", id));

        boolean isReporter = issue.getReportedBy() != null && issue.getReportedBy().getId().equals(currentUser.getId());
        boolean isAdmin = currentUser.getRole() == Role.ADMIN;

        if (!isAdmin && !isReporter) {
            throw new UnauthorizedException("You do not have permission to update this issue");
        }

        if (!isAdmin && issue.getStatus() != IssueStatus.SUBMITTED) {
            throw new BadRequestException("Cannot update issue once it is " + issue.getStatus());
        }

        if (request.getTitle() != null && !request.getTitle().trim().isEmpty()) {
            issue.setTitle(request.getTitle().trim());
        }
        if (request.getDescription() != null && !request.getDescription().trim().isEmpty()) {
            issue.setDescription(request.getDescription().trim());
        }
        if (request.getCategory() != null) {
            issue.setCategory(request.getCategory());
        }
        if (request.getLocation() != null && !request.getLocation().trim().isEmpty()) {
            issue.setLocation(request.getLocation().trim());
        }
        if (request.getPriority() != null) {
            issue.setPriority(request.getPriority());
        }

        HelpDeskIssue updated = helpDeskRepository.save(issue);
        log.info("Help desk issue ID {} updated by {}", id, currentUser.getEmail());
        return HelpDeskIssueResponse.fromEntity(updated);
    }

    /**
     * Delete an issue.
     * Reporter can only delete while status is SUBMITTED.
     * ADMIN can delete anytime.
     */
    @Transactional
    public void deleteIssue(Long id) {
        User currentUser = getCurrentAuthenticatedUser();
        HelpDeskIssue issue = helpDeskRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("HelpDeskIssue", "id", id));

        boolean isReporter = issue.getReportedBy() != null && issue.getReportedBy().getId().equals(currentUser.getId());
        boolean isAdmin = currentUser.getRole() == Role.ADMIN;

        if (!isAdmin && !isReporter) {
            throw new UnauthorizedException("You do not have permission to delete this issue");
        }

        if (!isAdmin && issue.getStatus() != IssueStatus.SUBMITTED) {
            throw new BadRequestException("Cannot delete issue once it is " + issue.getStatus());
        }

        helpDeskRepository.delete(issue);
        log.info("Help desk issue ID {} deleted by {}", id, currentUser.getEmail());
    }

    /**
     * Assign issue to staff / user.
     * ADMIN only.
     */
    @Transactional
    public HelpDeskIssueResponse assignIssue(Long id, AssignIssueRequest request) {
        User currentUser = getCurrentAuthenticatedUser();
        if (currentUser.getRole() != Role.ADMIN) {
            throw new UnauthorizedException("Only administrators can assign issues");
        }

        HelpDeskIssue issue = helpDeskRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("HelpDeskIssue", "id", id));

        User assignedUser = userRepository.findById(request.getAssignedToUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", request.getAssignedToUserId()));

        issue.setAssignedTo(assignedUser);
        if (issue.getStatus() == IssueStatus.SUBMITTED) {
            issue.setStatus(IssueStatus.IN_PROGRESS);
        }

        HelpDeskIssue updated = helpDeskRepository.save(issue);
        log.info("Issue ID {} assigned to {} by {}", id, assignedUser.getEmail(), currentUser.getEmail());
        return HelpDeskIssueResponse.fromEntity(updated);
    }

    /**
     * Update issue status.
     * ADMIN can update to any valid status.
     * Reporter can close their own RESOLVED issue.
     */
    @Transactional
    public HelpDeskIssueResponse updateStatus(Long id, UpdateIssueStatusRequest request) {
        User currentUser = getCurrentAuthenticatedUser();
        HelpDeskIssue issue = helpDeskRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("HelpDeskIssue", "id", id));

        boolean isReporter = issue.getReportedBy() != null && issue.getReportedBy().getId().equals(currentUser.getId());
        boolean isAdmin = currentUser.getRole() == Role.ADMIN;

        if (isAdmin) {
            issue.setStatus(request.getStatus());
        } else if (isReporter) {
            // Student can close their own RESOLVED issue
            if (issue.getStatus() == IssueStatus.RESOLVED && request.getStatus() == IssueStatus.CLOSED) {
                issue.setStatus(IssueStatus.CLOSED);
            } else {
                throw new BadRequestException("You can only close an issue that has been resolved");
            }
        } else {
            throw new UnauthorizedException("You do not have permission to update the status of this issue");
        }

        HelpDeskIssue updated = helpDeskRepository.save(issue);
        log.info("Issue ID {} status updated to {} by {}", id, request.getStatus(), currentUser.getEmail());
        return HelpDeskIssueResponse.fromEntity(updated);
    }

    /**
     * Resolve issue with resolution notes.
     * ADMIN only.
     */
    @Transactional
    public HelpDeskIssueResponse resolveIssue(Long id, ResolveIssueRequest request) {
        User currentUser = getCurrentAuthenticatedUser();
        if (currentUser.getRole() != Role.ADMIN) {
            throw new UnauthorizedException("Only administrators can resolve issues");
        }

        HelpDeskIssue issue = helpDeskRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("HelpDeskIssue", "id", id));

        issue.setStatus(IssueStatus.RESOLVED);
        issue.setResolvedAt(LocalDateTime.now());
        issue.setResolutionNotes(request.getResolutionNotes().trim());

        HelpDeskIssue updated = helpDeskRepository.save(issue);
        log.info("Issue ID {} resolved by {}", id, currentUser.getEmail());
        return HelpDeskIssueResponse.fromEntity(updated);
    }
}
