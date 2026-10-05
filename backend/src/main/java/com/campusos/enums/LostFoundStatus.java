package com.campusos.enums;

/**
 * LostFoundStatus enum representing lifecycle states of a lost or found item.
 *
 * OPEN      — item is actively being searched for or claimed.
 * MATCHED   — potential match found / owner identified.
 * RECOVERED — item returned to its rightful owner.
 * CLOSED    — listing resolved or closed without recovery.
 */
public enum LostFoundStatus {
    OPEN,
    MATCHED,
    RECOVERED,
    CLOSED
}
