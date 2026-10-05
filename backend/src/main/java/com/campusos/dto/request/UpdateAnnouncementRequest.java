package com.campusos.dto.request;

import com.campusos.enums.AnnouncementCategory;
import com.campusos.enums.AnnouncementStatus;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * UpdateAnnouncementRequest — incoming payload for PUT /api/announcements/{id}
 *
 * All fields optional — only non-null fields are applied (patch-style update).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateAnnouncementRequest {

    @Size(min = 3, max = 200, message = "Title must be between 3 and 200 characters")
    private String title;

    @Size(min = 10, max = 10000, message = "Content must be between 10 and 10000 characters")
    private String content;

    private AnnouncementCategory category;

    @Size(max = 255, message = "Image URL must not exceed 255 characters")
    private String image;

    private Boolean isPinned;

    private LocalDateTime publishedAt;

    private LocalDateTime expiresAt;

    private AnnouncementStatus status;
}
