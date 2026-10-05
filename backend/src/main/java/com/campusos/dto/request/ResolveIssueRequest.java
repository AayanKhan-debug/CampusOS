package com.campusos.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * ResolveIssueRequest — incoming payload for PATCH /api/help-desk/{id}/resolve
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResolveIssueRequest {

    @NotBlank(message = "Resolution notes are required")
    @Size(min = 3, max = 5000, message = "Resolution notes must be between 3 and 5000 characters")
    private String resolutionNotes;
}
