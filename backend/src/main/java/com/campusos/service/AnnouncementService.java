package com.campusos.service;

import com.campusos.dto.request.CreateAnnouncementRequest;
import com.campusos.dto.request.UpdateAnnouncementRequest;
import com.campusos.dto.response.AnnouncementResponse;
import com.campusos.dto.response.PageResponse;
import com.campusos.entity.Announcement;
import com.campusos.entity.User;
import com.campusos.enums.AnnouncementCategory;
import com.campusos.enums.AnnouncementStatus;
import com.campusos.enums.Role;
import com.campusos.exception.BadRequestException;
import com.campusos.exception.ResourceNotFoundException;
import com.campusos.exception.UnauthorizedException;
import com.campusos.repository.AnnouncementRepository;
import com.campusos.repository.UserRepository;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * AnnouncementService — full lifecycle management for campus announcements.
 *
 * Authorization:
 *   STUDENT       → read-only access to ACTIVE announcements
 *   CLUB_ADMIN    → can create/manage their own announcements
 *   ADMIN         → full access to all announcements
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AnnouncementService {

    private final AnnouncementRepository announcementRepository;
    private final UserRepository userRepository;

    public User getCurrentAuthenticatedUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));
    }

    private boolean canManageAnnouncement(User user, Announcement announcement) {
        if (user.getRole() == Role.ADMIN) return true;
        return announcement.getCreatedBy() != null
                && announcement.getCreatedBy().getId().equals(user.getId());
    }

    /**
     * Creates a new announcement.
     * POST /api/announcements
     *
     * Business rules:
     * - STUDENT cannot create announcements.
     * - expiresAt must be after publishedAt if both are set.
     * - publishedAt defaults to now if not provided.
     */
    @Transactional
    public AnnouncementResponse createAnnouncement(CreateAnnouncementRequest request) {
        User currentUser = getCurrentAuthenticatedUser();

        if (currentUser.getRole() == Role.STUDENT) {
            throw new UnauthorizedException("Students are not allowed to create announcements");
        }

        LocalDateTime publishedAt = request.getPublishedAt() != null
                ? request.getPublishedAt() : LocalDateTime.now();

        // Validate expiresAt
        if (request.getExpiresAt() != null && request.getExpiresAt().isBefore(publishedAt)) {
            throw new BadRequestException("Expiry date must be after published date");
        }

        Announcement announcement = Announcement.builder()
                .title(request.getTitle().trim())
                .content(request.getContent().trim())
                .category(request.getCategory())
                .image(request.getImage())
                .isPinned(request.isPinned())
                .publishedAt(publishedAt)
                .expiresAt(request.getExpiresAt())
                .status(AnnouncementStatus.ACTIVE)
                .createdBy(currentUser)
                .build();

        Announcement saved = announcementRepository.save(announcement);
        log.info("Announcement '{}' created by {}", saved.getTitle(), currentUser.getEmail());
        return AnnouncementResponse.fromEntity(saved);
    }

    /**
     * Returns paginated announcements with optional filters.
     * GET /api/announcements
     *
     * - Default: only ACTIVE announcements
     * - Pinned appear first, then sorted by newest publishedAt
     * - Supports: search (title/content), category, status, pinned filter
     * - Auto-expires announcements before listing
     */
    @Transactional
    public PageResponse<AnnouncementResponse> getAllAnnouncements(
            String search, AnnouncementCategory category, AnnouncementStatus status,
            Boolean pinned, Pageable pageable) {

        // Auto-expire any announcements past their expiry date
        autoExpireAnnouncements();

        Specification<Announcement> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Default to ACTIVE if no status filter
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            } else {
                predicates.add(cb.equal(root.get("status"), AnnouncementStatus.ACTIVE));
            }

            if (search != null && !search.isBlank()) {
                String pattern = "%" + search.trim().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("title")), pattern),
                        cb.like(cb.lower(root.get("content")), pattern)
                ));
            }

            if (category != null) {
                predicates.add(cb.equal(root.get("category"), category));
            }

            if (pinned != null) {
                predicates.add(cb.equal(root.get("isPinned"), pinned));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<Announcement> page = announcementRepository.findAll(spec, pageable);
        List<AnnouncementResponse> content = page.getContent().stream()
                .map(AnnouncementResponse::fromEntity)
                .toList();

        return PageResponse.of(page, content);
    }

    /**
     * Returns a specific announcement by ID.
     * GET /api/announcements/{id}
     */
    @Transactional(readOnly = true)
    public AnnouncementResponse getAnnouncementById(Long id) {
        Announcement announcement = announcementRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Announcement", "id", id));
        return AnnouncementResponse.fromEntity(announcement);
    }

    /**
     * Updates an existing announcement.
     * PUT /api/announcements/{id}
     */
    @Transactional
    public AnnouncementResponse updateAnnouncement(Long id, UpdateAnnouncementRequest request) {
        User currentUser = getCurrentAuthenticatedUser();
        Announcement announcement = announcementRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Announcement", "id", id));

        if (!canManageAnnouncement(currentUser, announcement)) {
            throw new UnauthorizedException("You do not have permission to update this announcement");
        }

        if (request.getTitle() != null && !request.getTitle().isBlank()) {
            announcement.setTitle(request.getTitle().trim());
        }
        if (request.getContent() != null && !request.getContent().isBlank()) {
            announcement.setContent(request.getContent().trim());
        }
        if (request.getCategory() != null) {
            announcement.setCategory(request.getCategory());
        }
        if (request.getImage() != null) {
            announcement.setImage(request.getImage());
        }
        if (request.getIsPinned() != null) {
            announcement.setPinned(request.getIsPinned());
        }
        if (request.getPublishedAt() != null) {
            announcement.setPublishedAt(request.getPublishedAt());
        }
        if (request.getExpiresAt() != null) {
            if (request.getExpiresAt().isBefore(announcement.getPublishedAt())) {
                throw new BadRequestException("Expiry date must be after published date");
            }
            announcement.setExpiresAt(request.getExpiresAt());
        }
        if (request.getStatus() != null) {
            announcement.setStatus(request.getStatus());
        }

        Announcement updated = announcementRepository.save(announcement);
        log.info("Announcement {} updated by {}", id, currentUser.getEmail());
        return AnnouncementResponse.fromEntity(updated);
    }

    /**
     * Deletes (hard-delete) an announcement.
     * DELETE /api/announcements/{id}
     */
    @Transactional
    public void deleteAnnouncement(Long id) {
        User currentUser = getCurrentAuthenticatedUser();
        Announcement announcement = announcementRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Announcement", "id", id));

        if (!canManageAnnouncement(currentUser, announcement)) {
            throw new UnauthorizedException("You do not have permission to delete this announcement");
        }

        announcementRepository.delete(announcement);
        log.info("Announcement {} deleted by {}", id, currentUser.getEmail());
    }

    /**
     * Pins or unpins an announcement.
     * PATCH /api/announcements/{id}/pin
     */
    @Transactional
    public AnnouncementResponse togglePin(Long id) {
        User currentUser = getCurrentAuthenticatedUser();
        Announcement announcement = announcementRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Announcement", "id", id));

        if (!canManageAnnouncement(currentUser, announcement)) {
            throw new UnauthorizedException("You do not have permission to pin/unpin this announcement");
        }

        announcement.setPinned(!announcement.isPinned());
        Announcement updated = announcementRepository.save(announcement);
        log.info("Announcement {} pin toggled to {} by {}", id, updated.isPinned(), currentUser.getEmail());
        return AnnouncementResponse.fromEntity(updated);
    }

    /**
     * Archives an announcement.
     * PATCH /api/announcements/{id}/archive
     */
    @Transactional
    public AnnouncementResponse archiveAnnouncement(Long id) {
        User currentUser = getCurrentAuthenticatedUser();
        Announcement announcement = announcementRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Announcement", "id", id));

        if (!canManageAnnouncement(currentUser, announcement)) {
            throw new UnauthorizedException("You do not have permission to archive this announcement");
        }

        if (announcement.getStatus() == AnnouncementStatus.ARCHIVED) {
            throw new BadRequestException("Announcement is already archived");
        }

        announcement.setStatus(AnnouncementStatus.ARCHIVED);
        Announcement updated = announcementRepository.save(announcement);
        log.info("Announcement {} archived by {}", id, currentUser.getEmail());
        return AnnouncementResponse.fromEntity(updated);
    }

    /**
     * Automatically sets ACTIVE announcements past their expiresAt to EXPIRED.
     * Called before listing announcements.
     */
    @Transactional
    public void autoExpireAnnouncements() {
        List<Announcement> expired = announcementRepository.findExpiredActiveAnnouncements(LocalDateTime.now());
        if (!expired.isEmpty()) {
            expired.forEach(a -> a.setStatus(AnnouncementStatus.EXPIRED));
            announcementRepository.saveAll(expired);
            log.debug("Auto-expired {} announcements", expired.size());
        }
    }

    /**
     * Returns the current user's unread active announcements.
     * GET /api/users/me/announcements/unread
     */
    @Transactional
    public PageResponse<AnnouncementResponse> getUnreadAnnouncements(Pageable pageable) {
        User currentUser = getCurrentAuthenticatedUser();
        autoExpireAnnouncements();

        Page<Announcement> page = announcementRepository.findUnreadByUserId(currentUser.getId(), pageable);
        List<AnnouncementResponse> content = page.getContent().stream()
                .map(AnnouncementResponse::fromEntity)
                .toList();

        return PageResponse.of(page, content);
    }
}
