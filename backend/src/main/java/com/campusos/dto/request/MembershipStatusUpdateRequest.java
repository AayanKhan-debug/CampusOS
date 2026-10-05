package com.campusos.dto.request;

import com.campusos.enums.MembershipStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * MembershipStatusUpdateRequest — incoming payload for PUT /api/clubs/{clubId}/members/{userId}
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MembershipStatusUpdateRequest {

    @NotNull(message = "Membership status is required (APPROVED or REJECTED)")
    private MembershipStatus status;
}
