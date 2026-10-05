package com.campusos.repository;

import com.campusos.entity.Announcement;
import com.campusos.enums.AnnouncementCategory;
import com.campusos.enums.AnnouncementStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * AnnouncementRepository — data access for Announcement entities.
 *
 * Implements JpaSpecificationExecutor for dynamic filtering
 * (category, status, search, pinned).
 */
@Repository
public interface AnnouncementRepository extends JpaRepository<Announcement, Long>, JpaSpecificationExecutor<Announcement> {

    /** Find all active announcements — pinned first, then by publishedAt desc */
    Page<Announcement> findByStatusOrderByIsPinnedDescPublishedAtDesc(AnnouncementStatus status, Pageable pageable);

    /** Find active announcements by category */
    Page<Announcement> findByStatusAndCategoryOrderByIsPinnedDescPublishedAtDesc(
            AnnouncementStatus status, AnnouncementCategory category, Pageable pageable);

    /**
     * Find all ACTIVE announcements whose IDs have NOT been read by the given user.
     * Used for the GET /api/users/me/announcements/unread endpoint.
     */
    @Query("SELECT a FROM Announcement a WHERE a.status = 'ACTIVE' " +
           "AND a.id NOT IN (SELECT ar.announcement.id FROM AnnouncementRead ar WHERE ar.user.id = :userId) " +
           "ORDER BY a.isPinned DESC, a.publishedAt DESC")
    Page<Announcement> findUnreadByUserId(@Param("userId") Long userId, Pageable pageable);

    /**
     * Find all announcements that have passed their expiresAt and are still ACTIVE.
     * Called by a scheduled job or service to auto-expire announcements.
     */
    @Query("SELECT a FROM Announcement a WHERE a.status = 'ACTIVE' AND a.expiresAt IS NOT NULL AND a.expiresAt < :now")
    List<Announcement> findExpiredActiveAnnouncements(@Param("now") LocalDateTime now);
}
