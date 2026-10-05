package com.campusos.dto.response;

import com.campusos.entity.Opportunity;
import com.campusos.enums.OpportunityStatus;
import com.campusos.enums.OpportunityType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * OpportunityResponse — representation of an Opportunity.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OpportunityResponse {

    private Long id;
    private String title;
    private String description;
    private String organization;
    private OpportunityType opportunityType;
    private String eligibility;
    private String applicationUrl;
    private LocalDateTime deadline;
    private String location;
    private Boolean remote;
    private OpportunityStatus status;
    private UserResponse createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static OpportunityResponse fromEntity(Opportunity opp) {
        if (opp == null) return null;

        return OpportunityResponse.builder()
                .id(opp.getId())
                .title(opp.getTitle())
                .description(opp.getDescription())
                .organization(opp.getOrganization())
                .opportunityType(opp.getOpportunityType())
                .eligibility(opp.getEligibility())
                .applicationUrl(opp.getApplicationUrl())
                .deadline(opp.getDeadline())
                .location(opp.getLocation())
                .remote(opp.getRemote())
                .status(opp.getStatus())
                .createdBy(opp.getCreatedBy() != null ? UserResponse.fromEntity(opp.getCreatedBy()) : null)
                .createdAt(opp.getCreatedAt())
                .updatedAt(opp.getUpdatedAt())
                .build();
    }
}
