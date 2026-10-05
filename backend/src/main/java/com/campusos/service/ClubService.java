package com.campusos.service;

import com.campusos.dto.request.CreateClubRequest;
import com.campusos.dto.request.UpdateClubRequest;
import com.campusos.dto.response.ClubResponse;
import com.campusos.dto.response.PageResponse;
import com.campusos.entity.Club;
import com.campusos.entity.ClubMember;
import com.campusos.entity.User;
import com.campusos.enums.ClubStatus;
import com.campusos.enums.MembershipStatus;
import com.campusos.enums.Role;
import com.campusos.exception.DuplicateResourceException;
import com.campusos.exception.ResourceNotFoundException;
import com.campusos.exception.UnauthorizedException;
import com.campusos.repository.ClubMemberRepository;
import com.campusos.repository.ClubRepository;
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

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * ClubService — handles club lifecycle and queries.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ClubService {

    private final ClubRepository clubRepository;
    private final ClubMemberRepository clubMemberRepository;
    private final UserRepository userRepository;

    /**
     * Helper to get the currently authenticated User entity from the SecurityContext.
     */
    public User getCurrentAuthenticatedUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));
    }

    /**
     * Checks if the user is authorized to manage the club (must be ADMIN or the club creator/admin).
     */
    public boolean canManageClub(User user, Club club) {
        if (user.getRole() == Role.ADMIN) {
            return true;
        }
        return club.getCreatedBy() != null && club.getCreatedBy().getId().equals(user.getId());
    }

    /**
     * Creates a new Club and adds the creator as an APPROVED member.
     * POST /api/clubs
     */
    @Transactional
    public ClubResponse createClub(CreateClubRequest request) {
        User currentUser = getCurrentAuthenticatedUser();

        // Check for duplicate club name
        if (clubRepository.existsByNameIgnoreCase(request.getName().trim())) {
            throw new DuplicateResourceException("A club with name '" + request.getName() + "' already exists");
        }

        Club club = Club.builder()
                .name(request.getName().trim())
                .description(request.getDescription())
                .category(request.getCategory() != null ? request.getCategory().trim() : null)
                .logo(request.getLogo())
                .createdBy(currentUser)
                .status(ClubStatus.ACTIVE)
                .build();

        Club savedClub = clubRepository.save(club);

        // Automatically register creator as an approved member of their club
        ClubMember creatorMembership = ClubMember.builder()
                .club(savedClub)
                .user(currentUser)
                .status(MembershipStatus.APPROVED)
                .joinedAt(LocalDateTime.now())
                .build();
        clubMemberRepository.save(creatorMembership);

        log.info("Club '{}' created by user {}", savedClub.getName(), currentUser.getEmail());

        return ClubResponse.fromEntity(savedClub, 1);
    }

    /**
     * Returns paginated clubs with optional search, category, and status filters.
     * GET /api/clubs
     */
    @Transactional(readOnly = true)
    public PageResponse<ClubResponse> getAllClubs(String search, String category, ClubStatus status, Pageable pageable) {
        Specification<Club> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Filter by search query across name and description
            if (search != null && !search.isBlank()) {
                String searchPattern = "%" + search.trim().toLowerCase() + "%";
                Predicate namePredicate = cb.like(cb.lower(root.get("name")), searchPattern);
                Predicate descPredicate = cb.like(cb.lower(root.get("description")), searchPattern);
                predicates.add(cb.or(namePredicate, descPredicate));
            }

            // Filter by category
            if (category != null && !category.isBlank()) {
                predicates.add(cb.equal(cb.lower(root.get("category")), category.trim().toLowerCase()));
            }

            // Filter by status (default to ACTIVE if not specified, or allow all if requested)
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<Club> clubPage = clubRepository.findAll(spec, pageable);

        List<ClubResponse> content = clubPage.getContent().stream()
                .map(club -> {
                    long memberCount = clubMemberRepository.countByClubIdAndStatus(club.getId(), MembershipStatus.APPROVED);
                    return ClubResponse.fromEntity(club, memberCount);
                })
                .toList();

        return PageResponse.of(clubPage, content);
    }

    /**
     * Returns details of a specific club by ID.
     * GET /api/clubs/{id}
     */
    @Transactional(readOnly = true)
    public ClubResponse getClubById(Long id) {
        Club club = clubRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Club", "id", id));
        long memberCount = clubMemberRepository.countByClubIdAndStatus(club.getId(), MembershipStatus.APPROVED);
        return ClubResponse.fromEntity(club, memberCount);
    }

    /**
     * Updates an existing club's details.
     * PUT /api/clubs/{id}
     */
    @Transactional
    public ClubResponse updateClub(Long id, UpdateClubRequest request) {
        User currentUser = getCurrentAuthenticatedUser();
        Club club = clubRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Club", "id", id));

        // Authorization check: only ADMIN or club's creator can update
        if (!canManageClub(currentUser, club)) {
            throw new UnauthorizedException("You do not have permission to update this club");
        }

        // Validate name uniqueness if changing name
        if (request.getName() != null && !request.getName().isBlank()) {
            String trimmedName = request.getName().trim();
            if (clubRepository.existsByNameIgnoreCaseAndIdNot(trimmedName, id)) {
                throw new DuplicateResourceException("Another club with name '" + trimmedName + "' already exists");
            }
            club.setName(trimmedName);
        }

        if (request.getDescription() != null) {
            club.setDescription(request.getDescription());
        }
        if (request.getCategory() != null) {
            club.setCategory(request.getCategory().trim());
        }
        if (request.getLogo() != null) {
            club.setLogo(request.getLogo());
        }
        if (request.getStatus() != null) {
            club.setStatus(request.getStatus());
        }

        Club updatedClub = clubRepository.save(club);
        long memberCount = clubMemberRepository.countByClubIdAndStatus(updatedClub.getId(), MembershipStatus.APPROVED);
        log.info("Club '{}' (id: {}) updated by user {}", updatedClub.getName(), id, currentUser.getEmail());

        return ClubResponse.fromEntity(updatedClub, memberCount);
    }

    /**
     * Deactivates (soft deletes) a club.
     * DELETE /api/clubs/{id}
     */
    @Transactional
    public void deleteClub(Long id) {
        User currentUser = getCurrentAuthenticatedUser();
        Club club = clubRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Club", "id", id));

        // Authorization check: only ADMIN or club creator can deactivate
        if (!canManageClub(currentUser, club)) {
            throw new UnauthorizedException("You do not have permission to delete/deactivate this club");
        }

        // Soft deactivation
        club.setStatus(ClubStatus.INACTIVE);
        clubRepository.save(club);
        log.info("Club '{}' (id: {}) deactivated by user {}", club.getName(), id, currentUser.getEmail());
    }
}
