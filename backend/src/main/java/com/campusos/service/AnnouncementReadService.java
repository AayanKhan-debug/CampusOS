package com.campusos.service;

import com.campusos.dto.response.AnnouncementReadResponse;
import com.campusos.entity.Announcement;
import com.campusos.entity.AnnouncementRead;
import com.campusos.entity.User;
import com.campusos.enums.AnnouncementStatus;
import com.campusos.exception.BadRequestException;
import com.campusos.exception.DuplicateResourceException;
import com.campusos.exception.ResourceNotFoundException;
import com.campusos.repository.AnnouncementReadRepository;
import com.campusos.repository.AnnouncementRepository;
import com.campusos.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * AnnouncementReadService — handles marking announcements as read and unread tracking.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AnnouncementReadService {

    private final AnnouncementReadRepository readRepository;
    private final AnnouncementRepository announcementRepository;
    private final UserRepository userRepository;

    private User getCurrentAuthenticatedUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));
    }

    /**
     * Marks an announcement as read for the current user.
     * POST /api/announcements/{id}/read
     *
     * Business rules:
     * - Announcement must exist.
     * - User cannot mark a duplicate read (unique constraint enforced at DB level + application level).
     * - Only ACTIVE announcements can be marked as read.
     */
    @Transactional
    public AnnouncementReadResponse markAsRead(Long announcementId) {
        User currentUser = getCurrentAuthenticatedUser();

        Announcement announcement = announcementRepository.findById(announcementId)
                .orElseThrow(() -> new ResourceNotFoundException("Announcement", "id", announcementId));

        if (announcement.getStatus() != AnnouncementStatus.ACTIVE) {
            throw new BadRequestException("Can only mark active announcements as read");
        }

        // Duplicate read prevention
        if (readRepository.existsByAnnouncementIdAndUserId(announcementId, currentUser.getId())) {
            throw new DuplicateResourceException("You have already marked this announcement as read");
        }

        AnnouncementRead read = AnnouncementRead.builder()
                .announcement(announcement)
                .user(currentUser)
                .readAt(LocalDateTime.now())
                .build();

        AnnouncementRead saved = readRepository.save(read);
        log.debug("User {} marked announcement {} as read", currentUser.getEmail(), announcementId);

        return AnnouncementReadResponse.fromEntity(saved);
    }
}
