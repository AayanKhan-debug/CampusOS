package com.campusos.enums;

/**
 * Membership status for a student in a Club.
 *
 * PENDING  — Join request submitted by student, awaiting club admin review.
 * APPROVED — Request approved; student is an active member.
 * REJECTED — Request rejected by club admin.
 */
public enum MembershipStatus {
    PENDING,
    APPROVED,
    REJECTED
}
