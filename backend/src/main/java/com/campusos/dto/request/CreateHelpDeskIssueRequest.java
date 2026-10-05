package com.campusos.dto.request;

import com.campusos.enums.IssueCategory;
import com.campusos.enums.IssuePriority;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * CreateHelpDeskIssueRequest — incoming payload for POST /api/help-desk
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateHelpDeskIssueRequest {

    @NotBlank(message = "Title is required")
    @Size(min = 3, max = 200, message = "Title must be between 3 and 200 characters")
    private String title;

    @NotBlank(message = "Description is required")
    @Size(min = 5, max = 5000, message = "Description must be between 5 and 5000 characters")
    private String description;

    @NotNull(message = "Category is required")
    private IssueCategory category;

    @NotBlank(message = "Location is required")
    @Size(min = 2, max = 200, message = "Location must be between 2 and 200 characters")
    private String location;

    @NotNull(message = "Priority is required")
    private IssuePriority priority;
}
