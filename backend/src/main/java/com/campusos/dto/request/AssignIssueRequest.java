package com.campusos.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * AssignIssueRequest — incoming payload for PATCH /api/help-desk/{id}/assign
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssignIssueRequest {

    @NotNull(message = "Assigned user ID is required")
    private Long assignedToUserId;
}
