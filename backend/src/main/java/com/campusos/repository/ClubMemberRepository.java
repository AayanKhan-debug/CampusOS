package com.campusos.repository;

import com.campusos.entity.ClubMember;
import com.campusos.enums.MembershipStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * ClubMemberRepository — data access for ClubMember entities.
 */
@Repository
public interface ClubMemberRepository extends JpaRepository<ClubMember, Long> {

    /** Find a user's membership in a specific club */
    Optional<ClubMember> findByClubIdAndUserId(Long clubId, Long userId);

    /** Check if a membership record exists for a user in a club */
    boolean existsByClubIdAndUserId(Long clubId, Long userId);

    /** Check if a user has a specific status in a club (e.g. APPROVED) */
    boolean existsByClubIdAndUserIdAndStatus(Long clubId, Long userId, MembershipStatus status);

    /** Find members of a club with a specific status (with pagination) */
    Page<ClubMember> findByClubIdAndStatus(Long clubId, MembershipStatus status, Pageable pageable);

    /** Count the approved members of a club */
    long countByClubIdAndStatus(Long clubId, MembershipStatus status);

    /** Find all clubs a specific user has joined with a given status */
    List<ClubMember> findByUserIdAndStatus(Long userId, MembershipStatus status);

    /** Check if a user is an approved member or creator of a club */
    @Query("SELECT CASE WHEN COUNT(cm) > 0 THEN true ELSE false END " +
           "FROM ClubMember cm WHERE cm.club.id = :clubId AND cm.user.id = :userId AND cm.status = 'APPROVED'")
    boolean isUserApprovedMember(@Param("clubId") Long clubId, @Param("userId") Long userId);
}
