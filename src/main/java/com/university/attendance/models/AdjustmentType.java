package com.university.attendance.models;

/**
 * Type of lecture adjustment (V2 feature — designed now, built later).
 * CANCELLED   = lecture will not happen on its original date.
 * RESCHEDULED = lecture moved to a different date/time.
 * EXTRA       = an additional lecture not in the regular timetable.
 * Stored in the 'type' column of the 'lecture_adjustments' table (V2).
 */
public enum AdjustmentType {
    CANCELLED,
    RESCHEDULED,
    EXTRA
}
