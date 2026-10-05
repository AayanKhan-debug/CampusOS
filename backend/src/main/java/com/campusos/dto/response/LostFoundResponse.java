package com.campusos.dto.response;

import com.campusos.entity.LostFoundItem;
import com.campusos.enums.ListingType;
import com.campusos.enums.LostFoundStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * LostFoundResponse — representation of a Lost & Found item.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LostFoundResponse {

    private Long id;
    private String title;
    private String description;
    private String itemType;
    private ListingType listingType;
    private String location;
    private LocalDate dateOccurred;
    private String imageUrl;
    private String contactInfo;
    private LostFoundStatus status;
    private UserResponse reportedBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static LostFoundResponse fromEntity(LostFoundItem item) {
        if (item == null) return null;

        return LostFoundResponse.builder()
                .id(item.getId())
                .title(item.getTitle())
                .description(item.getDescription())
                .itemType(item.getItemType())
                .listingType(item.getListingType())
                .location(item.getLocation())
                .dateOccurred(item.getDateOccurred())
                .imageUrl(item.getImageUrl())
                .contactInfo(item.getContactInfo())
                .status(item.getStatus())
                .reportedBy(item.getReportedBy() != null ? UserResponse.fromEntity(item.getReportedBy()) : null)
                .createdAt(item.getCreatedAt())
                .updatedAt(item.getUpdatedAt())
                .build();
    }
}
