package com.campusos.dto.request;

import com.campusos.enums.ListingType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * CreateLostFoundRequest — incoming payload for POST /api/lost-found
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateLostFoundRequest {

    @NotBlank(message = "Title is required")
    @Size(min = 3, max = 200, message = "Title must be between 3 and 200 characters")
    private String title;

    @Size(max = 5000, message = "Description must not exceed 5000 characters")
    private String description;

    @NotBlank(message = "Item type is required")
    @Size(min = 2, max = 100, message = "Item type must be between 2 and 100 characters")
    private String itemType;

    @NotNull(message = "Listing type is required (LOST or FOUND)")
    private ListingType listingType;

    @NotBlank(message = "Location is required")
    @Size(min = 2, max = 200, message = "Location must be between 2 and 200 characters")
    private String location;

    @NotNull(message = "Date occurred is required")
    private LocalDate dateOccurred;

    @Size(max = 500, message = "Image URL must not exceed 500 characters")
    private String imageUrl;

    @Size(max = 200, message = "Contact info must not exceed 200 characters")
    private String contactInfo;
}
