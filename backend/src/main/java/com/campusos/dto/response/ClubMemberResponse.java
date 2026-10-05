package com.campusos.dto.response;

import com.campusos.entity.ClubMember;
import com.campusos.enums.MembershipStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * ClubMemberResponse — safe API representation of a ClubMember relationship.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClubMemberResponse {

    private Long id;
    private Long clubId;
    private String clubName;
    private UserResponse user;
    private MembershipStatus status;
    private LocalDateTime joinedAt;
    private LocalDateTime createdAt;

    public static ClubMemberResponse fromEntity(ClubMember member) {
        return ClubMemberResponse.builder()
                .id(member.getId())
                .clubId(member.getClub() != null ? member.getClub().getId() : null)
                .clubName(member.getClub() != null ? member.getClub().getName() : null)
                .user(member.getUser() != null ? UserResponse.fromEntity(member.getUser()) : null)
                .status(member.getStatus())
                .joinedAt(member.getJoinedAt())
                .createdAt(member.getCreatedAt())
                .build();
    }
}
