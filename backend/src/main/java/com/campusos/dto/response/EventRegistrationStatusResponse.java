package com.campusos.dto.response;

import com.campusos.enums.RegistrationStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * EventRegistrationStatusResponse — lightweight response showing
 * the current user's registration status for a specific event.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventRegistrationStatusResponse {

    private Long eventId;
    private RegistrationStatus status;
    private String message;
}
