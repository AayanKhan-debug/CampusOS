package com.campusos.repository;

import com.campusos.entity.Event;
import com.campusos.enums.EventCategory;
import com.campusos.enums.EventStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

/**
 * EventRepository — data access for Event entities.
 *
 * Implements JpaSpecificationExecutor to support dynamic multi-field filtering
 * (search by title, filter by category/status/date/location) with Pageable.
 */
@Repository
public interface EventRepository extends JpaRepository<Event, Long>, JpaSpecificationExecutor<Event> {

    /** Find events by category */
    Page<Event> findByCategory(EventCategory category, Pageable pageable);

    /** Find events by status */
    Page<Event> findByStatus(EventStatus status, Pageable pageable);

    /** Find events by date */
    Page<Event> findByDate(LocalDate date, Pageable pageable);

    /** Find events by club ID */
    Page<Event> findByClubId(Long clubId, Pageable pageable);

    /** Find upcoming events for a specific club */
    Page<Event> findByClubIdAndStatus(Long clubId, EventStatus status, Pageable pageable);

    /** Find events created by a specific user */
    Page<Event> findByCreatedById(Long userId, Pageable pageable);
}
