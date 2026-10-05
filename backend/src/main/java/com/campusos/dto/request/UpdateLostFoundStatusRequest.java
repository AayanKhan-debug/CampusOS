package com.campusos.dto.request;

import com.campusos.enums.LostFoundStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * UpdateLostFoundStatusRequest — incoming payload for PATCH /api/lost-found/{id}/status
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateLostFoundStatusRequest {

    @NotNull(message = "Status is required")
    private LostFoundStatus status;
}
