package com.campusos.repository;

import com.campusos.entity.HelpDeskIssue;
import com.campusos.enums.IssueCategory;
import com.campusos.enums.IssuePriority;
import com.campusos.enums.IssueStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * HelpDeskRepository — Spring Data JPA repository for HelpDeskIssue.
 *
 * Implements database-level sorting prioritizing:
 * URGENT (1) -> HIGH (2) -> MEDIUM (3) -> LOW (4), followed by createdAt DESC.
 */
@Repository
public interface HelpDeskRepository extends JpaRepository<HelpDeskIssue, Long>, JpaSpecificationExecutor<HelpDeskIssue> {

    @Query("SELECT h FROM HelpDeskIssue h WHERE " +
           "(:status IS NULL OR h.status = :status) AND " +
           "(:category IS NULL OR h.category = :category) AND " +
           "(:priority IS NULL OR h.priority = :priority) " +
           "ORDER BY CASE h.priority " +
           "  WHEN com.campusos.enums.IssuePriority.URGENT THEN 1 " +
           "  WHEN com.campusos.enums.IssuePriority.HIGH THEN 2 " +
           "  WHEN com.campusos.enums.IssuePriority.MEDIUM THEN 3 " +
           "  WHEN com.campusos.enums.IssuePriority.LOW THEN 4 " +
           "  ELSE 5 END ASC, h.createdAt DESC")
    Page<HelpDeskIssue> findWithFiltersAndPrioritySort(
            @Param("status") IssueStatus status,
            @Param("category") IssueCategory category,
            @Param("priority") IssuePriority priority,
            Pageable pageable);

    @Query("SELECT h FROM HelpDeskIssue h WHERE " +
           "h.reportedBy.id = :userId AND " +
           "(:status IS NULL OR h.status = :status) AND " +
           "(:category IS NULL OR h.category = :category) AND " +
           "(:priority IS NULL OR h.priority = :priority) " +
           "ORDER BY h.createdAt DESC")
    Page<HelpDeskIssue> findByReportedByIdWithFilters(
            @Param("userId") Long userId,
            @Param("status") IssueStatus status,
            @Param("category") IssueCategory category,
            @Param("priority") IssuePriority priority,
            Pageable pageable);
}
