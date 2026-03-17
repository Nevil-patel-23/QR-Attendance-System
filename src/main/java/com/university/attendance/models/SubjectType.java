package com.university.attendance.models;

/**
 * Whether a subject is compulsory for all students in a semester
 * or an elective that students opt into.
 * Stored in the 'type' column of the 'subjects' table.
 */
public enum SubjectType {
    COMPULSORY,
    ELECTIVE
}
