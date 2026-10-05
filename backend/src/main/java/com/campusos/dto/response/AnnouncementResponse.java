package com.campusos.dto.response;

import com.campusos.entity.Announcement;
import com.campusos.enums.AnnouncementCategory;
import com.campusos.enums.AnnouncementStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * AnnouncementResponse — safe API representation of an Announcement.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnnouncementResponse {

    private Long id;
    private String title;
    private String content;
    private AnnouncementCategory category;
    private String image;
    private boolean isPinned;
    private LocalDateTime publishedAt;
    private LocalDateTime expiresAt;
    private AnnouncementStatus status;
    private UserResponse createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static AnnouncementResponse fromEntity(Announcement announcement) {
        return AnnouncementResponse.builder()
                .id(announcement.getId())
                .title(announcement.getTitle())
                .content(announcement.getContent())
                .category(announcement.getCategory())
                .image(announcement.getImage())
                .isPinned(announcement.isPinned())
                .publishedAt(announcement.getPublishedAt())
                .expiresAt(announcement.getExpiresAt())
                .status(announcement.getStatus())
                .createdBy(announcement.getCreatedBy() != null
                        ? UserResponse.fromEntity(announcement.getCreatedBy()) : null)
                .createdAt(announcement.getCreatedAt())
                .updatedAt(announcement.getUpdatedAt())
                .build();
    }
}
