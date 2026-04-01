package com.university.attendance.repository;

import com.university.attendance.models.AttendanceSession;
import com.university.attendance.models.AttendanceStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Queries the attendance_sessions table.
 * Used by QrService to create/lookup sessions and by ScheduledJobService
 * to find expired sessions that need ABSENT records inserted.
 */
@Repository
public interface AttendanceSessionRepository extends JpaRepository<AttendanceSession, UUID> {

    /**
     * Find a session by its QR token that is still active and not expired.
     * Used during QR scan validation (Check 1).
     */
    Optional<AttendanceSession> findByQrTokenAndIsActiveTrueAndExpiresAtAfter(
            String qrToken, LocalDateTime now);

    /**
     * Find all sessions for a specific slot on a specific date.
     * Used to check session history for a slot.
     */
    List<AttendanceSession> findBySlotSlotIdAndSessionDate(UUID slotId, LocalDate date);

    /**
     * Find the active session for a specific slot on a specific date.
     * Used to deactivate previous session before generating a new one.
     */
    Optional<AttendanceSession> findBySlotSlotIdAndSessionDateAndIsActiveTrue(
            UUID slotId, LocalDate date);

    /**
     * Count attendance records with a specific status for a session.
     * Used to get the live scan counter (how many PRESENT so far).
     */
    @Query("SELECT COUNT(ar) FROM AttendanceRecord ar WHERE ar.session.sessionId = :sessionId AND ar.status = :status")
    long countBySessionIdAndStatus(@Param("sessionId") UUID sessionId, @Param("status") AttendanceStatus status);

    /**
     * Find all sessions that have expired but are still marked active.
     * Used by the @Scheduled background job to process expired sessions.
     */
    List<AttendanceSession> findByIsActiveTrueAndExpiresAtBefore(LocalDateTime now);

    /**
     * Count completed sessions for a subject within a date range.
     * Used to calculate total sessions for attendance percentage.
     */
    long countBySubjectSubjectIdAndIsActiveFalseAndSessionDateBetween(
            UUID subjectId, LocalDate startDate, LocalDate endDate);

    /**
     * Find all completed sessions for a subject within a date range, ordered by date.
     * Used for teacher attendance reports (T3).
     */
    List<AttendanceSession> findBySubjectSubjectIdAndIsActiveFalseAndSessionDateBetweenOrderBySessionDateAsc(
            UUID subjectId, LocalDate startDate, LocalDate endDate);

    /**
     * Find all completed sessions for an entire semester within a date range.
     * Used by admin attendance matrix report to bulk-fetch all sessions at once.
     */
    List<AttendanceSession> findBySemesterSemesterIdAndIsActiveFalseAndSessionDateBetween(
            UUID semesterId, LocalDate startDate, LocalDate endDate);
}
