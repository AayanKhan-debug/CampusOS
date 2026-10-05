package com.campusos.enums;

/**
 * Type of academic resource in the Resource Hub.
 *
 * NOTES         — lecture notes, handwritten or typed
 * QUESTION_PAPER — previous year exam papers
 * ASSIGNMENT    — assignment sheets or solutions
 * LAB           — lab manuals, programs, viva questions
 * VIDEO         — lecture/tutorial video links
 * BOOK          — textbook PDFs or reference links
 * REFERENCE     — external reference links, articles
 * OTHER         — anything that doesn't fit above
 */
public enum ResourceType {
    NOTES,
    QUESTION_PAPER,
    ASSIGNMENT,
    LAB,
    VIDEO,
    BOOK,
    REFERENCE,
    OTHER
}
