package com.campusos.repository;

import com.campusos.entity.Club;
import com.campusos.enums.ClubStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * ClubRepository — data access for Club entities.
 *
 * Implements JpaSpecificationExecutor to support dynamic multi-field filtering
 * (e.g., search by name, filter by category and status) with Pageable.
 */
@Repository
public interface ClubRepository extends JpaRepository<Club, Long>, JpaSpecificationExecutor<Club> {

    /** Case-insensitive check to prevent duplicate club names */
    boolean existsByNameIgnoreCase(String name);

    /** Case-insensitive check when updating club to ensure name uniqueness across other clubs */
    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);

    /** Find all active clubs */
    Page<Club> findByStatus(ClubStatus status, Pageable pageable);
}
