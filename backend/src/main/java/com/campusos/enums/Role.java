package com.campusos.enums;

/**
 * User roles in CampusOS.
 *
 * STUDENT    — default role assigned on registration
 * CLUB_ADMIN — can manage a specific club, its events and members
 * ADMIN      — platform administrator with full access
 *
 * Stored as a STRING in the database (not an ordinal number) so that
 * adding new roles later doesn't shift the ordinal values and corrupt data.
 */
public enum Role {
    STUDENT,
    CLUB_ADMIN,
    ADMIN
}
