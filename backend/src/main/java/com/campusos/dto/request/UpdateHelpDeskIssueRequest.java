package com.campusos.dto.request;

import com.campusos.enums.IssueCategory;
import com.campusos.enums.IssuePriority;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * UpdateHelpDeskIssueRequest — incoming payload for PUT /api/help-desk/{id}
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateHelpDeskIssueRequest {

    @Size(min = 3, max = 200, message = "Title must be between 3 and 200 characters")
    private String title;

    @Size(min = 5, max = 5000, message = "Description must be between 5 and 5000 characters")
    private String description;

    private IssueCategory category;

    @Size(min = 2, max = 200, message = "Location must be between 2 and 200 characters")
    private String location;

    private IssuePriority priority;
}
