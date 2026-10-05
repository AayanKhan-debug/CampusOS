package com.campusos.dto.request;

import com.campusos.enums.ClubStatus;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * UpdateClubRequest — incoming payload for PUT /api/clubs/{id}
 * All fields are optional (partial update).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateClubRequest {

    @Size(min = 2, max = 100, message = "Club name must be between 2 and 100 characters")
    private String name;

    @Size(max = 2000, message = "Description must not exceed 2000 characters")
    private String description;

    @Size(max = 50, message = "Category must not exceed 50 characters")
    private String category;

    @Size(max = 255, message = "Logo URL must not exceed 255 characters")
    private String logo;

    private ClubStatus status;
}
