package com.university.attendance.repository;

import com.university.attendance.models.AttendanceRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Queries the attendance_records table.
 * Used to check for duplicate scans, list records per session,
 * and retrieve a student's attendance history per subject.
 */
@Repository
public interface AttendanceRecordRepository extends JpaRepository<AttendanceRecord, UUID> {

    /**
     * Check if a student already has an attendance record for a session.
     * Used during scan validation (Check 4 — duplicate scan).
     */
    boolean existsBySessionSessionIdAndStudentStudentId(UUID sessionId, UUID studentId);

    /**
     * Get all attendance records for a session.
     * Used to display session attendance summary.
     */
    List<AttendanceRecord> findBySessionSessionId(UUID sessionId);

    /**
     * Get a student's attendance records for a specific subject.
     * Used for student attendance detail view.
     */
    List<AttendanceRecord> findByStudentStudentIdAndSessionSubjectSubjectId(
            UUID studentId, UUID subjectId);

    /**
     * Get a student's attendance records for a specific subject, but only for inactive sessions.
     * Prevents active lectures from showing up in details before they end.
     */
    List<AttendanceRecord> findByStudentStudentIdAndSessionSubjectSubjectIdAndSessionIsActiveFalse(
            UUID studentId, UUID subjectId);


    /**
     * Get attendance records for multiple sessions in bulk.
     * Used for teacher report generation to prevent N+1 queries.
     */
    List<AttendanceRecord> findBySessionSessionIdIn(List<UUID> sessionIds);
}
