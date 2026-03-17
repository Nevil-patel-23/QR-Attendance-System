package com.university.attendance.models;

/**
 * Attendance outcome for a student in a session.
 * PRESENT = student scanned the QR successfully.
 * ABSENT  = inserted by the background job after session expires.
 * Stored in the 'status' column of the 'attendance_records' table.
 */
public enum AttendanceStatus {
    PRESENT,
    ABSENT
}
