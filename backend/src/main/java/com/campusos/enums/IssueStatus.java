package com.campusos.enums;

/**
 * IssueStatus enum representing help desk resolution workflow states.
 *
 * SUBMITTED   — issue reported by user, awaiting review.
 * IN_PROGRESS — issue assigned or being worked on.
 * RESOLVED    — issue resolved with resolution notes.
 * CLOSED      — issue confirmed closed by user or admin.
 */
public enum IssueStatus {
    SUBMITTED,
    IN_PROGRESS,
    RESOLVED,
    CLOSED
}
