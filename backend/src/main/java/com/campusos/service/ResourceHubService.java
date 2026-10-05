package com.campusos.service;

import com.campusos.dto.request.CreateResourceRequest;
import com.campusos.dto.request.UpdateResourceRequest;
import com.campusos.dto.response.PageResponse;
import com.campusos.dto.response.ResourceResponse;
import com.campusos.entity.Resource;
import com.campusos.entity.Subject;
import com.campusos.entity.User;
import com.campusos.enums.ResourceType;
import com.campusos.enums.Role;
import com.campusos.exception.ResourceNotFoundException;
import com.campusos.exception.UnauthorizedException;
import com.campusos.repository.ResourceRepository;
import com.campusos.repository.SubjectRepository;
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

import java.util.ArrayList;
import java.util.List;

/**
 * ResourceService — CRUD for academic resources in the Resource Hub.
 *
 * Authorization:
 *   STUDENT    → read-only (browse resources)
 *   CLUB_ADMIN → read-only (same as student for resources)
 *   ADMIN      → full access (create, update, delete)
 *
 * uploadedBy is always set from the authenticated user, not the request body.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ResourceHubService {

    private final ResourceRepository resourceRepository;
    private final SubjectRepository subjectRepository;
    private final UserRepository userRepository;

    private User getCurrentAuthenticatedUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));
    }

    private boolean canManageResource(User user, Resource resource) {
        if (user.getRole() == Role.ADMIN) return true;
        return resource.getUploadedBy() != null
                && resource.getUploadedBy().getId().equals(user.getId());
    }

    /**
     * Creates a new resource.
     * POST /api/resources
     *
     * uploadedBy is set from the authenticated user.
     * STUDENT role is blocked.
     */
    @Transactional
    public ResourceResponse createResource(CreateResourceRequest request) {
        User currentUser = getCurrentAuthenticatedUser();

        if (currentUser.getRole() == Role.STUDENT) {
            throw new UnauthorizedException("Students are not allowed to upload resources");
        }

        Subject subject = subjectRepository.findById(request.getSubjectId())
                .orElseThrow(() -> new ResourceNotFoundException("Subject", "id", request.getSubjectId()));

        Resource resource = Resource.builder()
                .title(request.getTitle().trim())
                .description(request.getDescription())
                .resourceType(request.getResourceType())
                .url(request.getUrl().trim())
                .subject(subject)
                .uploadedBy(currentUser)
                .build();

        Resource saved = resourceRepository.save(resource);
        log.info("Resource '{}' uploaded by {}", saved.getTitle(), currentUser.getEmail());
        return ResourceResponse.fromEntity(saved);
    }

    /**
     * Returns paginated resources with optional filters.
     * GET /api/resources?subjectId=1&resourceType=NOTES&search=...
     */
    @Transactional(readOnly = true)
    public PageResponse<ResourceResponse> getAllResources(
            Long subjectId, ResourceType resourceType, String search, Pageable pageable) {

        Specification<Resource> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (subjectId != null) {
                predicates.add(cb.equal(root.get("subject").get("id"), subjectId));
            }
            if (resourceType != null) {
                predicates.add(cb.equal(root.get("resourceType"), resourceType));
            }
            if (search != null && !search.isBlank()) {
                String pattern = "%" + search.trim().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("title")), pattern),
                        cb.like(cb.lower(root.get("description")), pattern)
                ));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<Resource> page = resourceRepository.findAll(spec, pageable);
        List<ResourceResponse> content = page.getContent().stream()
                .map(ResourceResponse::fromEntity).toList();
        return PageResponse.of(page, content);
    }

    /**
     * Returns details of a specific resource.
     * GET /api/resources/{id}
     */
    @Transactional(readOnly = true)
    public ResourceResponse getResourceById(Long id) {
        return ResourceResponse.fromEntity(
                resourceRepository.findById(id)
                        .orElseThrow(() -> new ResourceNotFoundException("Resource", "id", id))
        );
    }

    /**
     * Updates an existing resource.
     * PUT /api/resources/{id}
     */
    @Transactional
    public ResourceResponse updateResource(Long id, UpdateResourceRequest request) {
        User currentUser = getCurrentAuthenticatedUser();
        Resource resource = resourceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Resource", "id", id));

        if (!canManageResource(currentUser, resource)) {
            throw new UnauthorizedException("You do not have permission to update this resource");
        }

        if (request.getTitle() != null && !request.getTitle().isBlank()) {
            resource.setTitle(request.getTitle().trim());
        }
        if (request.getDescription() != null) {
            resource.setDescription(request.getDescription());
        }
        if (request.getResourceType() != null) {
            resource.setResourceType(request.getResourceType());
        }
        if (request.getUrl() != null && !request.getUrl().isBlank()) {
            resource.setUrl(request.getUrl().trim());
        }
        if (request.getSubjectId() != null) {
            Subject subject = subjectRepository.findById(request.getSubjectId())
                    .orElseThrow(() -> new ResourceNotFoundException("Subject", "id", request.getSubjectId()));
            resource.setSubject(subject);
        }

        Resource updated = resourceRepository.save(resource);
        log.info("Resource {} updated by {}", id, currentUser.getEmail());
        return ResourceResponse.fromEntity(updated);
    }

    /**
     * Deletes a resource.
     * DELETE /api/resources/{id}
     */
    @Transactional
    public void deleteResource(Long id) {
        User currentUser = getCurrentAuthenticatedUser();
        Resource resource = resourceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Resource", "id", id));

        if (!canManageResource(currentUser, resource)) {
            throw new UnauthorizedException("You do not have permission to delete this resource");
        }

        resourceRepository.delete(resource);
        log.info("Resource {} deleted by {}", id, currentUser.getEmail());
    }
}
