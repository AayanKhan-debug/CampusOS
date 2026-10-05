package com.campusos.repository;

import com.campusos.entity.AnnouncementRead;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * AnnouncementReadRepository — data access for read tracking.
 */
@Repository
public interface AnnouncementReadRepository extends JpaRepository<AnnouncementRead, Long> {

    /** Find a user's read record for a specific announcement */
    Optional<AnnouncementRead> findByAnnouncementIdAndUserId(Long announcementId, Long userId);

    /** Check if a user has already read an announcement */
    boolean existsByAnnouncementIdAndUserId(Long announcementId, Long userId);

    /** Count how many users have read a specific announcement */
    long countByAnnouncementId(Long announcementId);
}
