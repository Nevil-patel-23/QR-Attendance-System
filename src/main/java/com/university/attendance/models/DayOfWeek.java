package com.university.attendance.models;

/**
 * Days of the week when lectures can be scheduled.
 * Sunday is excluded — no lectures on Sundays.
 * Stored in the 'day_of_week' column of the 'timetable_slots' table.
 *
 * Note: We use our own enum instead of java.time.DayOfWeek
 * because the DB stores short names (MON, TUE...) and
 * java.time uses full names (MONDAY, TUESDAY...).
 */
public enum DayOfWeek {
    MON,
    TUE,
    WED,
    THU,
    FRI,
    SAT
}
