package com.campusos.dto.response;

import com.campusos.entity.Event;
import com.campusos.enums.EventCategory;
import com.campusos.enums.EventStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * EventResponse — safe API representation of an Event.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventResponse {

    private Long id;
    private String title;
    private String description;
    private EventCategory category;
    private LocalDate date;
    private LocalTime startTime;
    private LocalTime endTime;
    private String location;
    private Integer capacity;
    private LocalDateTime registrationDeadline;
    private String image;
    private EventStatus status;
    private Long clubId;
    private String clubName;
    private UserResponse createdBy;
    private long confirmedCount;
    private long waitlistedCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static EventResponse fromEntity(Event event, long confirmedCount, long waitlistedCount) {
        return EventResponse.builder()
                .id(event.getId())
                .title(event.getTitle())
                .description(event.getDescription())
                .category(event.getCategory())
                .date(event.getDate())
                .startTime(event.getStartTime())
                .endTime(event.getEndTime())
                .location(event.getLocation())
                .capacity(event.getCapacity())
                .registrationDeadline(event.getRegistrationDeadline())
                .image(event.getImage())
                .status(event.getStatus())
                .clubId(event.getClub() != null ? event.getClub().getId() : null)
                .clubName(event.getClub() != null ? event.getClub().getName() : null)
                .createdBy(event.getCreatedBy() != null ? UserResponse.fromEntity(event.getCreatedBy()) : null)
                .confirmedCount(confirmedCount)
                .waitlistedCount(waitlistedCount)
                .createdAt(event.getCreatedAt())
                .updatedAt(event.getUpdatedAt())
                .build();
    }
}
