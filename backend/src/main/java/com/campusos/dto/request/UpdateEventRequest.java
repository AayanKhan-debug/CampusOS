package com.campusos.dto.request;

import com.campusos.enums.EventCategory;
import com.campusos.enums.EventStatus;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * UpdateEventRequest — incoming payload for PUT /api/events/{id}
 *
 * All fields are optional — only non-null fields are applied (patch-style update).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateEventRequest {

    @Size(min = 3, max = 200, message = "Title must be between 3 and 200 characters")
    private String title;

    @Size(max = 5000, message = "Description must not exceed 5000 characters")
    private String description;

    private EventCategory category;

    private LocalDate date;

    private LocalTime startTime;

    private LocalTime endTime;

    @Size(max = 200, message = "Location must not exceed 200 characters")
    private String location;

    @Min(value = 1, message = "Capacity must be at least 1")
    private Integer capacity;

    private LocalDateTime registrationDeadline;

    @Size(max = 255, message = "Image URL must not exceed 255 characters")
    private String image;

    private EventStatus status;
}
