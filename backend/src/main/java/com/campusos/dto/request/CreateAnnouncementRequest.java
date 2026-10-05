package com.campusos.dto.request;

import com.campusos.enums.AnnouncementCategory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * CreateAnnouncementRequest — incoming payload for POST /api/announcements
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateAnnouncementRequest {

    @NotBlank(message = "Title is required")
    @Size(min = 3, max = 200, message = "Title must be between 3 and 200 characters")
    private String title;

    @NotBlank(message = "Content is required")
    @Size(min = 10, max = 10000, message = "Content must be between 10 and 10000 characters")
    private String content;

    @NotNull(message = "Category is required")
    private AnnouncementCategory category;

    @Size(max = 255, message = "Image URL must not exceed 255 characters")
    private String image;

    @Builder.Default
    private boolean isPinned = false;

    private LocalDateTime publishedAt;

    private LocalDateTime expiresAt;
}
