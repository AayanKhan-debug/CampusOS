package com.campusos.enums;

/**
 * Lifecycle status of an Event.
 *
 * UPCOMING   — Event is scheduled and open for registration.
 * ONGOING    — Event is currently taking place.
 * COMPLETED  — Event has finished.
 * CANCELLED  — Event was cancelled (soft-deleted).
 */
public enum EventStatus {
    UPCOMING,
    ONGOING,
    COMPLETED,
    CANCELLED
}
