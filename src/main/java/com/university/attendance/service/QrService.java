package com.university.attendance.service;

import com.university.attendance.dto.response.AttendanceSessionResponse;
import com.university.attendance.exception.ResourceNotFoundException;
import com.university.attendance.models.*;
import com.university.attendance.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Handles QR attendance session lifecycle:
 * - Generate a new session (with auto-deactivation of previous)
 * - Retrieve session status with live scan counts
 *
 * Each "Generate QR" click creates a new AttendanceSession row
 * with a fresh UUID token. If a previous active session exists
 * for the same slot+date, it is deactivated first.
 */
@Service
@RequiredArgsConstructor
public class QrService {

    private final AttendanceSessionRepository sessionRepo;
    private final AttendanceRecordRepository recordRepo;
    private final TimetableSlotRepository slotRepo;
    private final StudentRepository studentRepo;
    private final StudentSubjectEnrollmentRepository enrollmentRepo;

    @Value("${qr.expiry.seconds:300}")
    private int qrExpirySeconds;

    /**
     * Generate a new QR attendance session for a timetable slot.
     *
     * Steps:
     * 1. Load the slot and its allocation chain (teacher → subject → semester)
     * 2. Deactivate any previous active session for this slot+date
     * 3. Create a new session with a fresh UUID token and expiry
     * 4. Return the session details with scan counts
     *
     * @param slotId    the timetable slot to generate a session for
     * @param teacherId the teacher's UUID (from the teachers table, not user_id)
     * @return AttendanceSessionResponse with QR token and session info
     */
    @Transactional
    public AttendanceSessionResponse generateSession(UUID slotId, UUID teacherId) {
        // 1. Load slot with allocation chain
        TimetableSlot slot = slotRepo.findById(slotId)
                .orElseThrow(() -> new ResourceNotFoundException("Timetable slot not found"));

        TeacherSubjectAllocation allocation = slot.getAllocation();
        Subject subject = allocation.getSubject();
        Semester semester = allocation.getSemester();

        // 2. Deactivate any previous active session for same slot + today
        LocalDate today = LocalDate.now();
        sessionRepo.findBySlotSlotIdAndSessionDateAndIsActiveTrue(slotId, today)
                .ifPresent(previous -> {
                    previous.setIsActive(false);
                    sessionRepo.save(previous);
                });

        // 3. Create new session
        LocalDateTime now = LocalDateTime.now();
        AttendanceSession session = AttendanceSession.builder()
                .allocation(allocation)
                .slot(slot)
                .teacher(allocation.getTeacher())
                .subject(subject)
                .semester(semester)
                .sessionDate(today)
                .qrToken(UUID.randomUUID().toString())
                .generatedAt(now)
                .expiresAt(now.plusSeconds(qrExpirySeconds))
                .isActive(true)
                .build();

        session = sessionRepo.save(session);

        // 4. Count total eligible students for this subject
        int totalStudents = countEligibleStudents(subject, semester, allocation.getAcademicYear());

        return buildResponse(session, subject, semester, 0, totalStudents);
    }

    /**
     * Get current status of an attendance session.
     * Returns session details + live present count + total student count.
     *
     * @param sessionId the session to check
     * @return AttendanceSessionResponse with current scan counts
     */
    @Transactional(readOnly = true)
    public AttendanceSessionResponse getSessionStatus(UUID sessionId) {
        AttendanceSession session = sessionRepo.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Session not found"));

        Subject subject = session.getSubject();
        Semester semester = session.getSemester();

        long presentCount = sessionRepo.countBySessionIdAndStatus(sessionId, AttendanceStatus.PRESENT);
        int totalStudents = countEligibleStudents(subject, semester,
                session.getAllocation().getAcademicYear());

        return buildResponse(session, subject, semester, (int) presentCount, totalStudents);
    }

    /**
     * Count eligible students for a subject.
     * - COMPULSORY: all active students in the semester
     * - ELECTIVE: only students enrolled in the elective for the academic year
     */
    private int countEligibleStudents(Subject subject, Semester semester, String academicYear) {
        if (subject.getType() == SubjectType.COMPULSORY) {
            return studentRepo.findByCurrentSemesterSemesterIdAndUserIsActiveTrue(
                    semester.getSemesterId()).size();
        } else {
            return enrollmentRepo.findBySubjectSubjectIdAndAcademicYear(
                    subject.getSubjectId(), academicYear).size();
        }
    }

    /**
     * Build the DTO response from an AttendanceSession entity.
     */
    private AttendanceSessionResponse buildResponse(AttendanceSession session, Subject subject,
                                                     Semester semester, int presentCount, int totalStudents) {
        return AttendanceSessionResponse.builder()
                .sessionId(session.getSessionId())
                .qrToken(session.getQrToken())
                .subjectName(subject.getName())
                .subjectCode(subject.getCode())
                .semesterLabel(semester.getLabel())
                .sessionDate(session.getSessionDate())
                .generatedAt(session.getGeneratedAt())
                .expiresAt(session.getExpiresAt())
                .isActive(session.getIsActive())
                .presentCount(presentCount)
                .totalStudents(totalStudents)
                .build();
    }
}
