package com.campusos.repository;

import com.campusos.entity.Resource;
import com.campusos.enums.ResourceType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

/**
 * ResourceRepository — data access for Resource entities.
 *
 * JpaSpecificationExecutor enables dynamic filtering by subjectId + resourceType + search.
 */
@Repository
public interface ResourceRepository extends JpaRepository<Resource, Long>, JpaSpecificationExecutor<Resource> {

    Page<Resource> findBySubjectId(Long subjectId, Pageable pageable);

    Page<Resource> findByResourceType(ResourceType resourceType, Pageable pageable);

    Page<Resource> findBySubjectIdAndResourceType(Long subjectId, ResourceType resourceType, Pageable pageable);

    Page<Resource> findByUploadedById(Long userId, Pageable pageable);
}
