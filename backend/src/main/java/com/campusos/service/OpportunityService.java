package com.campusos.service;

import com.campusos.dto.request.CreateOpportunityRequest;
import com.campusos.dto.request.UpdateOpportunityRequest;
import com.campusos.dto.response.OpportunityResponse;
import com.campusos.dto.response.PageResponse;
import com.campusos.entity.Opportunity;
import com.campusos.entity.User;
import com.campusos.enums.OpportunityStatus;
import com.campusos.enums.OpportunityType;
import com.campusos.enums.Role;
import com.campusos.exception.BadRequestException;
import com.campusos.exception.ResourceNotFoundException;
import com.campusos.exception.UnauthorizedException;
import com.campusos.repository.OpportunityRepository;
import com.campusos.repository.UserRepository;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * OpportunityService — business logic for student opportunities (internships, hackathons, etc.).
 *
 * Authorization:
 * - STUDENT: browse / view opportunities.
 * - CLUB_ADMIN: create, and update/delete opportunities created by themselves.
 * - ADMIN: full management.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OpportunityService {

    private final OpportunityRepository opportunityRepository;
    private final UserRepository userRepository;

    public User getCurrentAuthenticatedUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));
    }

    private boolean canManageOpportunity(User user, Opportunity opp) {
        if (user.getRole() == Role.ADMIN) {
            return true;
        }
        return opp.getCreatedBy() != null && opp.getCreatedBy().getId().equals(user.getId());
    }

    private void validateApplicationUrl(String url) {
        if (url != null && !url.trim().isEmpty()) {
            String trimmed = url.trim();
            if (!trimmed.startsWith("http://") && !trimmed.startsWith("https://")) {
                throw new BadRequestException("Application URL must start with http:// or https://");
            }
        }
    }

    private void validateDeadline(LocalDateTime deadline) {
        if (deadline != null && deadline.isBefore(LocalDateTime.now())) {
            throw new BadRequestException("Opportunity deadline cannot be in the past");
        }
    }

    /**
     * Create a new opportunity.
     * Accessible to CLUB_ADMIN and ADMIN.
     */
    @Transactional
    public OpportunityResponse createOpportunity(CreateOpportunityRequest request) {
        User currentUser = getCurrentAuthenticatedUser();

        if (currentUser.getRole() == Role.STUDENT) {
            throw new UnauthorizedException("Students are not allowed to create opportunities");
        }

        validateApplicationUrl(request.getApplicationUrl());
        validateDeadline(request.getDeadline());

        Opportunity opportunity = Opportunity.builder()
                .title(request.getTitle().trim())
                .description(request.getDescription().trim())
                .organization(request.getOrganization().trim())
                .opportunityType(request.getOpportunityType())
                .eligibility(request.getEligibility() != null ? request.getEligibility().trim() : null)
                .applicationUrl(request.getApplicationUrl() != null ? request.getApplicationUrl().trim() : null)
                .deadline(request.getDeadline())
                .location(request.getLocation() != null ? request.getLocation().trim() : null)
                .remote(request.getRemote() != null ? request.getRemote() : false)
                .status(OpportunityStatus.OPEN)
                .createdBy(currentUser)
                .build();

        Opportunity saved = opportunityRepository.save(opportunity);
        log.info("Opportunity '{}' (ID: {}) created by user {}", saved.getTitle(), saved.getId(), currentUser.getEmail());
        return OpportunityResponse.fromEntity(saved);
    }

    /**
     * Get paginated opportunities.
     * Default feed shows OPEN status with active deadlines (deadline >= now or null).
     * Default sort: nearest deadline first.
     */
    @Transactional(readOnly = true)
    public PageResponse<OpportunityResponse> getAllOpportunities(
            OpportunityType type,
            String search,
            Boolean remote,
            Pageable pageable) {

        // Default sort: deadline ASC (nearest deadline first)
        if (pageable.getSort().isUnsorted()) {
            pageable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by(Sort.Direction.ASC, "deadline"));
        }

        LocalDateTime now = LocalDateTime.now();

        Specification<Opportunity> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Default feed: OPEN status
            predicates.add(cb.equal(root.get("status"), OpportunityStatus.OPEN));

            // Default feed: deadline is null OR deadline >= now
            Predicate deadlineNull = cb.isNull(root.get("deadline"));
            Predicate deadlineFuture = cb.greaterThanOrEqualTo(root.get("deadline"), now);
            predicates.add(cb.or(deadlineNull, deadlineFuture));

            if (type != null) {
                predicates.add(cb.equal(root.get("opportunityType"), type));
            }

            if (remote != null) {
                predicates.add(cb.equal(root.get("remote"), remote));
            }

            if (search != null && !search.trim().isEmpty()) {
                String searchPattern = "%" + search.trim().toLowerCase() + "%";
                Predicate titleMatch = cb.like(cb.lower(root.get("title")), searchPattern);
                Predicate descMatch = cb.like(cb.lower(root.get("description")), searchPattern);
                Predicate orgMatch = cb.like(cb.lower(root.get("organization")), searchPattern);
                predicates.add(cb.or(titleMatch, descMatch, orgMatch));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<Opportunity> page = opportunityRepository.findAll(spec, pageable);
        List<OpportunityResponse> responses = page.getContent().stream()
                .map(OpportunityResponse::fromEntity)
                .toList();

        return PageResponse.of(page, responses);
    }

    /**
     * Get single opportunity by ID.
     */
    @Transactional(readOnly = true)
    public OpportunityResponse getOpportunityById(Long id) {
        Opportunity opp = opportunityRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Opportunity", "id", id));
        return OpportunityResponse.fromEntity(opp);
    }

    /**
     * Update an opportunity.
     * CLUB_ADMIN can update their own. ADMIN can update any.
     */
    @Transactional
    public OpportunityResponse updateOpportunity(Long id, UpdateOpportunityRequest request) {
        User currentUser = getCurrentAuthenticatedUser();
        Opportunity opp = opportunityRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Opportunity", "id", id));

        if (!canManageOpportunity(currentUser, opp)) {
            throw new UnauthorizedException("You do not have permission to update this opportunity");
        }

        if (request.getApplicationUrl() != null) {
            validateApplicationUrl(request.getApplicationUrl());
            opp.setApplicationUrl(request.getApplicationUrl().trim());
        }

        if (request.getDeadline() != null) {
            validateDeadline(request.getDeadline());
            opp.setDeadline(request.getDeadline());
        }

        if (request.getTitle() != null && !request.getTitle().trim().isEmpty()) {
            opp.setTitle(request.getTitle().trim());
        }
        if (request.getDescription() != null && !request.getDescription().trim().isEmpty()) {
            opp.setDescription(request.getDescription().trim());
        }
        if (request.getOrganization() != null && !request.getOrganization().trim().isEmpty()) {
            opp.setOrganization(request.getOrganization().trim());
        }
        if (request.getOpportunityType() != null) {
            opp.setOpportunityType(request.getOpportunityType());
        }
        if (request.getEligibility() != null) {
            opp.setEligibility(request.getEligibility().trim());
        }
        if (request.getLocation() != null) {
            opp.setLocation(request.getLocation().trim());
        }
        if (request.getRemote() != null) {
            opp.setRemote(request.getRemote());
        }
        if (request.getStatus() != null) {
            opp.setStatus(request.getStatus());
        }

        Opportunity updated = opportunityRepository.save(opp);
        log.info("Opportunity ID {} updated by user {}", id, currentUser.getEmail());
        return OpportunityResponse.fromEntity(updated);
    }

    /**
     * Delete an opportunity.
     * CLUB_ADMIN can delete their own. ADMIN can delete any.
     */
    @Transactional
    public void deleteOpportunity(Long id) {
        User currentUser = getCurrentAuthenticatedUser();
        Opportunity opp = opportunityRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Opportunity", "id", id));

        if (!canManageOpportunity(currentUser, opp)) {
            throw new UnauthorizedException("You do not have permission to delete this opportunity");
        }

        opportunityRepository.delete(opp);
        log.info("Opportunity ID {} deleted by user {}", id, currentUser.getEmail());
    }
}
