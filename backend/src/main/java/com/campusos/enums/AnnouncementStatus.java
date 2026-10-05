package com.campusos.enums;

/**
 * Lifecycle status of an Announcement.
 *
 * ACTIVE   — Visible to students. Not yet expired or archived.
 * EXPIRED  — Past the expiresAt date. Hidden from default feed.
 * ARCHIVED — Manually archived by admin. Hidden from default feed.
 */
public enum AnnouncementStatus {
    ACTIVE,
    EXPIRED,
    ARCHIVED
}
