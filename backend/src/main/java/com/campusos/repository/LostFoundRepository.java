package com.campusos.repository;

import com.campusos.entity.LostFoundItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

/**
 * LostFoundRepository — Spring Data JPA repository for LostFoundItem.
 *
 * Supports JpaSpecificationExecutor for dynamic multi-field filtering and searching:
 * - listingType (LOST / FOUND)
 * - status (OPEN / MATCHED / RECOVERED / CLOSED)
 * - keyword search across title, description, itemType, location
 */
@Repository
public interface LostFoundRepository extends JpaRepository<LostFoundItem, Long>, JpaSpecificationExecutor<LostFoundItem> {
}
