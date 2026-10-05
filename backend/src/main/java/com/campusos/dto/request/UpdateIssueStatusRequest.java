package com.campusos.dto.request;

import com.campusos.enums.IssueStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * UpdateIssueStatusRequest — incoming payload for PATCH /api/help-desk/{id}/status
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateIssueStatusRequest {

    @NotNull(message = "Status is required")
    private IssueStatus status;
}
