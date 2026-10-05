package com.campusos.dto.response;

import com.campusos.entity.HelpDeskIssue;
import com.campusos.enums.IssueCategory;
import com.campusos.enums.IssuePriority;
import com.campusos.enums.IssueStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * HelpDeskIssueResponse — representation of a campus help desk issue.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HelpDeskIssueResponse {

    private Long id;
    private String title;
    private String description;
    private IssueCategory category;
    private String location;
    private IssuePriority priority;
    private IssueStatus status;
    private UserResponse reportedBy;
    private UserResponse assignedTo;
    private String resolutionNotes;
    private LocalDateTime resolvedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static HelpDeskIssueResponse fromEntity(HelpDeskIssue issue) {
        if (issue == null) return null;

        return HelpDeskIssueResponse.builder()
                .id(issue.getId())
                .title(issue.getTitle())
                .description(issue.getDescription())
                .category(issue.getCategory())
                .location(issue.getLocation())
                .priority(issue.getPriority())
                .status(issue.getStatus())
                .reportedBy(issue.getReportedBy() != null ? UserResponse.fromEntity(issue.getReportedBy()) : null)
                .assignedTo(issue.getAssignedTo() != null ? UserResponse.fromEntity(issue.getAssignedTo()) : null)
                .resolutionNotes(issue.getResolutionNotes())
                .resolvedAt(issue.getResolvedAt())
                .createdAt(issue.getCreatedAt())
                .updatedAt(issue.getUpdatedAt())
                .build();
    }
}
