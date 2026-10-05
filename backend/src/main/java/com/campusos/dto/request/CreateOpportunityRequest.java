package com.campusos.dto.request;

import com.campusos.enums.OpportunityType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * CreateOpportunityRequest — incoming payload for POST /api/opportunities
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateOpportunityRequest {

    @NotBlank(message = "Title is required")
    @Size(min = 3, max = 200, message = "Title must be between 3 and 200 characters")
    private String title;

    @NotBlank(message = "Description is required")
    private String description;

    @NotBlank(message = "Organization is required")
    @Size(min = 2, max = 200, message = "Organization must be between 2 and 200 characters")
    private String organization;

    @NotNull(message = "Opportunity type is required")
    private OpportunityType opportunityType;

    private String eligibility;

    @Size(max = 500, message = "Application URL must not exceed 500 characters")
    private String applicationUrl;

    private LocalDateTime deadline;

    @Size(max = 200, message = "Location must not exceed 200 characters")
    private String location;

    @Builder.Default
    private Boolean remote = false;
}
