package com.campusos.service;

import com.campusos.dto.request.CreateLostFoundRequest;
import com.campusos.dto.request.UpdateLostFoundRequest;
import com.campusos.dto.request.UpdateLostFoundStatusRequest;
import com.campusos.dto.response.LostFoundResponse;
import com.campusos.dto.response.PageResponse;
import com.campusos.entity.LostFoundItem;
import com.campusos.entity.User;
import com.campusos.enums.ListingType;
import com.campusos.enums.LostFoundStatus;
import com.campusos.enums.Role;
import com.campusos.exception.BadRequestException;
import com.campusos.exception.ResourceNotFoundException;
import com.campusos.exception.UnauthorizedException;
import com.campusos.repository.LostFoundRepository;
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

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * LostFoundService — business logic for campus Lost & Found management.
 *
 * Enforces ownership rules:
 * - Students and Club Admins can manage their own listings.
 * - Platform Admins can manage any listing.
 * - Allowed status transitions:
 *     OPEN -> MATCHED
 *     MATCHED -> RECOVERED
 *     OPEN -> CLOSED
 *     MATCHED -> CLOSED
 *     RECOVERED -> CLOSED
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class LostFoundService {

    private final LostFoundRepository lostFoundRepository;
    private final UserRepository userRepository;

    public User getCurrentAuthenticatedUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));
    }

    private boolean canManageListing(User user, LostFoundItem item) {
        if (user.getRole() == Role.ADMIN) {
            return true;
        }
        return item.getReportedBy() != null && item.getReportedBy().getId().equals(user.getId());
    }

    /**
     * Create a new Lost & Found listing.
     * reportedBy is automatically assigned from the authenticated user.
     */
    @Transactional
    public LostFoundResponse createListing(CreateLostFoundRequest request) {
        User currentUser = getCurrentAuthenticatedUser();

        if (request.getDateOccurred() != null && request.getDateOccurred().isAfter(LocalDate.now())) {
            throw new BadRequestException("Date occurred cannot be in the future");
        }

        LostFoundItem item = LostFoundItem.builder()
                .title(request.getTitle().trim())
                .description(request.getDescription() != null ? request.getDescription().trim() : null)
                .itemType(request.getItemType().trim())
                .listingType(request.getListingType())
                .location(request.getLocation().trim())
                .dateOccurred(request.getDateOccurred())
                .imageUrl(request.getImageUrl())
                .contactInfo(request.getContactInfo() != null ? request.getContactInfo().trim() : null)
                .status(LostFoundStatus.OPEN)
                .reportedBy(currentUser)
                .build();

        LostFoundItem saved = lostFoundRepository.save(item);
        log.info("Lost & Found item '{}' (ID: {}) created by user {}", saved.getTitle(), saved.getId(), currentUser.getEmail());
        return LostFoundResponse.fromEntity(saved);
    }

    /**
     * Get paginated listings.
     * If status is not provided, default feed shows OPEN and MATCHED.
     * Default sort: createdAt DESC.
     */
    @Transactional(readOnly = true)
    public PageResponse<LostFoundResponse> getAllListings(
            ListingType listingType,
            LostFoundStatus status,
            String search,
            Pageable pageable) {

        // Apply default sort createdAt DESC if no sort specified
        if (pageable.getSort().isUnsorted()) {
            pageable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by(Sort.Direction.DESC, "createdAt"));
        }

        Specification<LostFoundItem> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (listingType != null) {
                predicates.add(cb.equal(root.get("listingType"), listingType));
            }

            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            } else {
                // Default feed shows OPEN and MATCHED
                predicates.add(root.get("status").in(LostFoundStatus.OPEN, LostFoundStatus.MATCHED));
            }

            if (search != null && !search.trim().isEmpty()) {
                String searchPattern = "%" + search.trim().toLowerCase() + "%";
                Predicate titleMatch = cb.like(cb.lower(root.get("title")), searchPattern);
                Predicate descMatch = cb.like(cb.lower(cb.coalesce(root.get("description"), "")), searchPattern);
                Predicate itemTypeMatch = cb.like(cb.lower(root.get("itemType")), searchPattern);
                Predicate locationMatch = cb.like(cb.lower(root.get("location")), searchPattern);
                predicates.add(cb.or(titleMatch, descMatch, itemTypeMatch, locationMatch));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<LostFoundItem> page = lostFoundRepository.findAll(spec, pageable);
        List<LostFoundResponse> responses = page.getContent().stream()
                .map(LostFoundResponse::fromEntity)
                .toList();

        return PageResponse.of(page, responses);
    }

    /**
     * Get single listing by ID.
     */
    @Transactional(readOnly = true)
    public LostFoundResponse getListingById(Long id) {
        LostFoundItem item = lostFoundRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("LostFoundItem", "id", id));
        return LostFoundResponse.fromEntity(item);
    }

    /**
     * Update an existing listing.
     * Owner or ADMIN only.
     */
    @Transactional
    public LostFoundResponse updateListing(Long id, UpdateLostFoundRequest request) {
        User currentUser = getCurrentAuthenticatedUser();
        LostFoundItem item = lostFoundRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("LostFoundItem", "id", id));

        if (!canManageListing(currentUser, item)) {
            throw new UnauthorizedException("You do not have permission to update this listing");
        }

        if (request.getTitle() != null && !request.getTitle().trim().isEmpty()) {
            item.setTitle(request.getTitle().trim());
        }
        if (request.getDescription() != null) {
            item.setDescription(request.getDescription().trim());
        }
        if (request.getItemType() != null && !request.getItemType().trim().isEmpty()) {
            item.setItemType(request.getItemType().trim());
        }
        if (request.getLocation() != null && !request.getLocation().trim().isEmpty()) {
            item.setLocation(request.getLocation().trim());
        }
        if (request.getDateOccurred() != null) {
            if (request.getDateOccurred().isAfter(LocalDate.now())) {
                throw new BadRequestException("Date occurred cannot be in the future");
            }
            item.setDateOccurred(request.getDateOccurred());
        }
        if (request.getImageUrl() != null) {
            item.setImageUrl(request.getImageUrl());
        }
        if (request.getContactInfo() != null) {
            item.setContactInfo(request.getContactInfo().trim());
        }

        LostFoundItem updated = lostFoundRepository.save(item);
        log.info("Lost & Found item ID {} updated by user {}", id, currentUser.getEmail());
        return LostFoundResponse.fromEntity(updated);
    }

    /**
     * Delete a listing.
     * Owner or ADMIN only.
     */
    @Transactional
    public void deleteListing(Long id) {
        User currentUser = getCurrentAuthenticatedUser();
        LostFoundItem item = lostFoundRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("LostFoundItem", "id", id));

        if (!canManageListing(currentUser, item)) {
            throw new UnauthorizedException("You do not have permission to delete this listing");
        }

        lostFoundRepository.delete(item);
        log.info("Lost & Found item ID {} deleted by user {}", id, currentUser.getEmail());
    }

    /**
     * Update listing status according to allowed workflow transitions:
     * OPEN -> MATCHED
     * MATCHED -> RECOVERED
     * OPEN -> CLOSED
     * MATCHED -> CLOSED
     * RECOVERED -> CLOSED
     */
    @Transactional
    public LostFoundResponse updateStatus(Long id, UpdateLostFoundStatusRequest request) {
        User currentUser = getCurrentAuthenticatedUser();
        LostFoundItem item = lostFoundRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("LostFoundItem", "id", id));

        if (!canManageListing(currentUser, item)) {
            throw new UnauthorizedException("You do not have permission to change the status of this listing");
        }

        LostFoundStatus currentStatus = item.getStatus();
        LostFoundStatus newStatus = request.getStatus();

        if (currentStatus == newStatus) {
            return LostFoundResponse.fromEntity(item);
        }

        boolean isValidTransition = switch (currentStatus) {
            case OPEN -> (newStatus == LostFoundStatus.MATCHED || newStatus == LostFoundStatus.CLOSED);
            case MATCHED -> (newStatus == LostFoundStatus.RECOVERED || newStatus == LostFoundStatus.CLOSED);
            case RECOVERED -> (newStatus == LostFoundStatus.CLOSED);
            case CLOSED -> false;
        };

        if (!isValidTransition) {
            throw new BadRequestException("Invalid status transition from " + currentStatus + " to " + newStatus);
        }

        item.setStatus(newStatus);
        LostFoundItem updated = lostFoundRepository.save(item);
        log.info("Lost & Found item ID {} status updated from {} to {} by {}", id, currentStatus, newStatus, currentUser.getEmail());
        return LostFoundResponse.fromEntity(updated);
    }
}
