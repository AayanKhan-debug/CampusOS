package com.campusos.dto.response;

import com.campusos.entity.AnnouncementRead;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * AnnouncementReadResponse — confirms a successful read mark operation.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnnouncementReadResponse {

    private Long id;
    private Long announcementId;
    private String announcementTitle;
    private LocalDateTime readAt;

    public static AnnouncementReadResponse fromEntity(AnnouncementRead read) {
        return AnnouncementReadResponse.builder()
                .id(read.getId())
                .announcementId(read.getAnnouncement() != null ? read.getAnnouncement().getId() : null)
                .announcementTitle(read.getAnnouncement() != null ? read.getAnnouncement().getTitle() : null)
                .readAt(read.getReadAt())
                .build();
    }
}
