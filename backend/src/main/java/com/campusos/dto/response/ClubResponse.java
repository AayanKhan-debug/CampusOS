package com.campusos.dto.response;

import com.campusos.entity.Club;
import com.campusos.enums.ClubStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * ClubResponse — safe API representation of a Club.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClubResponse {

    private Long id;
    private String name;
    private String description;
    private String category;
    private String logo;
    private UserResponse createdBy;
    private ClubStatus status;
    private long memberCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static ClubResponse fromEntity(Club club, long memberCount) {
        return ClubResponse.builder()
                .id(club.getId())
                .name(club.getName())
                .description(club.getDescription())
                .category(club.getCategory())
                .logo(club.getLogo())
                .createdBy(club.getCreatedBy() != null ? UserResponse.fromEntity(club.getCreatedBy()) : null)
                .status(club.getStatus())
                .memberCount(memberCount)
                .createdAt(club.getCreatedAt())
                .updatedAt(club.getUpdatedAt())
                .build();
    }
}
