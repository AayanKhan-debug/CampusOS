package com.campusos.enums;

/**
 * Registration status for a user in an Event.
 *
 * CONFIRMED  — Registration confirmed (capacity was available).
 * WAITLISTED — Event is full; user is on the waitlist.
 * CANCELLED  — Registration was cancelled by the user.
 */
public enum RegistrationStatus {
    CONFIRMED,
    WAITLISTED,
    CANCELLED
}
