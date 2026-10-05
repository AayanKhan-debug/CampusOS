package com.campusos.service;

import com.campusos.dto.request.MembershipStatusUpdateRequest;
import com.campusos.dto.response.ClubMemberResponse;
import com.campusos.dto.response.PageResponse;
import com.campusos.entity.Club;
import com.campusos.entity.ClubMember;
import com.campusos.entity.User;
import com.campusos.enums.ClubStatus;
import com.campusos.enums.MembershipStatus;
import com.campusos.enums.Role;
import com.campusos.exception.BadRequestException;
import com.campusos.exception.DuplicateResourceException;
import com.campusos.exception.ResourceNotFoundException;
import com.campusos.exception.UnauthorizedException;
import com.campusos.repository.ClubMemberRepository;
import com.campusos.repository.ClubRepository;
import com.campusos.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * ClubMemberService — handles club join requests, membership listings, and approval workflows.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ClubMemberService {

    private final ClubMemberRepository clubMemberRepository;
    private final ClubRepository clubRepository;
    private final UserRepository userRepository;

    /**
     * Helper to get the currently authenticated User entity.
     */
    private User getCurrentAuthenticatedUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));
    }

    /**
     * Checks if the user is authorized to manage club memberships (ADMIN or club creator/admin).
     */
    private boolean canManageClubMembers(User user, Club club) {
        if (user.getRole() == Role.ADMIN) {
            return true;
        }
        return club.getCreatedBy() != null && club.getCreatedBy().getId().equals(user.getId());
    }

    /**
     * A student requests to join a club.
     * POST /api/clubs/{clubId}/join
     *
     * Business rules:
     * - Cannot join an INACTIVE/deactivated club.
     * - If already APPROVED -> 409 Conflict.
     * - If already PENDING -> 409 Conflict.
     * - If previously REJECTED -> resets request to PENDING for re-evaluation.
     * - If no previous record -> creates a new PENDING membership.
     */
    @Transactional
    public ClubMemberResponse joinClub(Long clubId) {
        User currentUser = getCurrentAuthenticatedUser();

        Club club = clubRepository.findById(clubId)
                .orElseThrow(() -> new ResourceNotFoundException("Club", "id", clubId));

        if (club.getStatus() == ClubStatus.INACTIVE) {
            throw new BadRequestException("Cannot join an inactive or deactivated club");
        }

        Optional<ClubMember> existingMembershipOpt = clubMemberRepository.findByClubIdAndUserId(clubId, currentUser.getId());

        if (existingMembershipOpt.isPresent()) {
            ClubMember existing = existingMembershipOpt.get();

            if (existing.getStatus() == MembershipStatus.APPROVED) {
                throw new DuplicateResourceException("You are already an approved member of this club");
            }

            if (existing.getStatus() == MembershipStatus.PENDING) {
                throw new DuplicateResourceException("You already have a pending membership request for this club");
            }

            // If previously REJECTED, allow re-requesting by updating the existing record back to PENDING
            if (existing.getStatus() == MembershipStatus.REJECTED) {
                existing.setStatus(MembershipStatus.PENDING);
                existing.setJoinedAt(null);
                ClubMember reRequested = clubMemberRepository.save(existing);
                log.info("User {} re-applied for club '{}'", currentUser.getEmail(), club.getName());
                return ClubMemberResponse.fromEntity(reRequested);
            }
        }

        // New membership record
        ClubMember newMember = ClubMember.builder()
                .club(club)
                .user(currentUser)
                .status(MembershipStatus.PENDING)
                .build();

        ClubMember savedMember = clubMemberRepository.save(newMember);
        log.info("User {} requested to join club '{}'", currentUser.getEmail(), club.getName());

        return ClubMemberResponse.fromEntity(savedMember);
    }

    /**
     * Returns approved members of a club.
     * GET /api/clubs/{clubId}/members
     */
    @Transactional(readOnly = true)
    public PageResponse<ClubMemberResponse> getClubMembers(Long clubId, Pageable pageable) {
        if (!clubRepository.existsById(clubId)) {
            throw new ResourceNotFoundException("Club", "id", clubId);
        }

        Page<ClubMember> membersPage = clubMemberRepository.findByClubIdAndStatus(clubId, MembershipStatus.APPROVED, pageable);
        List<ClubMemberResponse> content = membersPage.getContent().stream()
                .map(ClubMemberResponse::fromEntity)
                .toList();

        return PageResponse.of(membersPage, content);
    }

    /**
     * Returns pending membership requests for a club.
     * GET /api/clubs/{clubId}/members/requests
     * Only accessible by ADMIN or the club creator/admin.
     */
    @Transactional(readOnly = true)
    public PageResponse<ClubMemberResponse> getPendingRequests(Long clubId, Pageable pageable) {
        User currentUser = getCurrentAuthenticatedUser();
        Club club = clubRepository.findById(clubId)
                .orElseThrow(() -> new ResourceNotFoundException("Club", "id", clubId));

        if (!canManageClubMembers(currentUser, club)) {
            throw new UnauthorizedException("You do not have permission to view membership requests for this club");
        }

        Page<ClubMember> requestsPage = clubMemberRepository.findByClubIdAndStatus(clubId, MembershipStatus.PENDING, pageable);
        List<ClubMemberResponse> content = requestsPage.getContent().stream()
                .map(ClubMemberResponse::fromEntity)
                .toList();

        return PageResponse.of(requestsPage, content);
    }

    /**
     * Approves or rejects a membership request.
     * PUT /api/clubs/{clubId}/members/{userId}
     *
     * Business rules:
     * - Only ADMIN or club creator/admin can approve/reject.
     * - User cannot approve their own membership request.
     * - Only transitions from PENDING -> APPROVED or PENDING -> REJECTED are permitted.
     */
    @Transactional
    public ClubMemberResponse updateMemberStatus(Long clubId, Long userId, MembershipStatusUpdateRequest request) {
        User currentUser = getCurrentAuthenticatedUser();
        Club club = clubRepository.findById(clubId)
                .orElseThrow(() -> new ResourceNotFoundException("Club", "id", clubId));

        // Authorization check
        if (!canManageClubMembers(currentUser, club)) {
            throw new UnauthorizedException("You do not have permission to manage memberships for this club");
        }

        // Business rule: User cannot approve/reject their own request
        if (currentUser.getId().equals(userId)) {
            throw new BadRequestException("You cannot approve or reject your own membership request");
        }

        ClubMember membership = clubMemberRepository.findByClubIdAndUserId(clubId, userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Membership not found for user ID " + userId + " in club ID " + clubId
                ));

        // State transition validation
        if (membership.getStatus() != MembershipStatus.PENDING) {
            throw new BadRequestException(
                    "Cannot update membership with status " + membership.getStatus() +
                    ". Only PENDING requests can be APPROVED or REJECTED."
            );
        }

        if (request.getStatus() != MembershipStatus.APPROVED && request.getStatus() != MembershipStatus.REJECTED) {
            throw new BadRequestException("Target status must be either APPROVED or REJECTED");
        }

        membership.setStatus(request.getStatus());

        if (request.getStatus() == MembershipStatus.APPROVED) {
            membership.setJoinedAt(LocalDateTime.now());
            // TODO: [Phase 7 Notification] Send notification to user about club membership approval
            log.info("Membership APPROVED for user {} in club '{}'", userId, club.getName());
        } else {
            // TODO: [Phase 7 Notification] Send notification to user about club membership rejection
            log.info("Membership REJECTED for user {} in club '{}'", userId, club.getName());
        }

        ClubMember updatedMember = clubMemberRepository.save(membership);
        return ClubMemberResponse.fromEntity(updatedMember);
    }
}
