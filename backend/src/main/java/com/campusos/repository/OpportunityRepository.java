package com.campusos.repository;

import com.campusos.entity.Opportunity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

/**
 * OpportunityRepository — Spring Data JPA repository for Opportunity.
 *
 * Supports JpaSpecificationExecutor for dynamic filtering:
 * - opportunityType
 * - remote status
 * - search across title, description, organization
 * - active deadlines (deadline >= now)
 */
@Repository
public interface OpportunityRepository extends JpaRepository<Opportunity, Long>, JpaSpecificationExecutor<Opportunity> {
}
