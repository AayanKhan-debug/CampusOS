package com.campusos.dto.response;

import com.campusos.entity.EventRegistration;
import com.campusos.enums.RegistrationStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * EventRegistrationResponse — safe API representation of an EventRegistration.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventRegistrationResponse {

    private Long id;
    private Long eventId;
    private String eventTitle;
    private UserResponse user;
    private RegistrationStatus status;
    private LocalDateTime registeredAt;
    private LocalDateTime cancelledAt;
    private LocalDateTime createdAt;

    public static EventRegistrationResponse fromEntity(EventRegistration registration) {
        return EventRegistrationResponse.builder()
                .id(registration.getId())
                .eventId(registration.getEvent() != null ? registration.getEvent().getId() : null)
                .eventTitle(registration.getEvent() != null ? registration.getEvent().getTitle() : null)
                .user(registration.getUser() != null ? UserResponse.fromEntity(registration.getUser()) : null)
                .status(registration.getStatus())
                .registeredAt(registration.getRegisteredAt())
                .cancelledAt(registration.getCancelledAt())
                .createdAt(registration.getCreatedAt())
                .build();
    }
}
