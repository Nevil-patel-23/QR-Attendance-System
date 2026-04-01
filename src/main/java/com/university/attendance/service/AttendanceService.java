package com.university.attendance.service;

import com.university.attendance.dto.response.AttendanceSuccessResponse;
import com.university.attendance.exception.DuplicateScanException;
import com.university.attendance.exception.QrExpiredException;
import com.university.attendance.exception.ValidationException;
import com.university.attendance.models.*;
import com.university.attendance.repository.AttendanceRecordRepository;
import com.university.attendance.repository.AttendanceSessionRepository;
import com.university.attendance.repository.StudentRepository;
import com.university.attendance.repository.StudentSubjectEnrollmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Core attendance recording service. Implements the 5-check validation chain
 * that runs when a student scans a QR code and submits their attendance.
 */
@Service
@RequiredArgsConstructor
public class AttendanceService {

    private final AttendanceSessionRepository sessionRepository;
    private final AttendanceRecordRepository recordRepository;
    private final StudentRepository studentRepository;
    private final StudentSubjectEnrollmentRepository enrollmentRepository;

    /**
     * Record a student's attendance by validating the QR token through
     * a strict 5-check chain and creating a PRESENT record.
     *
     * Returns a safe DTO (not a Hibernate entity) so Vaadin can render
     * it outside the transaction boundary without LazyInitializationException.
     */
    @Transactional
    public AttendanceSuccessResponse recordAttendance(String token, UUID studentId) {

        // ── Check 1: Token validity + session active + not expired ──
        AttendanceSession session = sessionRepository
                .findByQrTokenAndIsActiveTrueAndExpiresAtAfter(token, LocalDateTime.now())
                .orElseThrow(() -> new QrExpiredException("QR code has expired or is invalid"));

        // ── Load the student ──
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new ValidationException("Student not found"));

        // ── Check 2: Semester match — student must be in the same semester as the session ──
        if (!session.getSemester().getSemesterId().equals(
                student.getCurrentSemester().getSemesterId())) {
            throw new ValidationException("You are not in this class");
        }

        // Ensure the student's batch year matches the session's computed academic year
        Integer sessionBatchYear = Integer.parseInt(session.getAllocation().getAcademicYear());
        if (!student.getBatchYear().equals(sessionBatchYear)) {
            throw new ValidationException("You belong to a different academic batch");
        }

        // ── Check 3: Subject eligibility ──
        Subject subject = session.getSubject();
        if (subject.getType() == SubjectType.ELECTIVE) {
            // For elective subjects, verify the student is enrolled
            String academicYear = calculateAcademicYear(session.getSessionDate());
            boolean enrolled = enrollmentRepository.existsByStudentStudentIdAndSubjectSubjectIdAndAcademicYear(
                    studentId, subject.getSubjectId(), academicYear);
            if (!enrolled) {
                throw new ValidationException("You are not enrolled in this subject");
            }
        }
        // COMPULSORY subjects pass automatically — Check 2 already proved semester match

        // ── Check 4: Duplicate scan (application-level check) ──
        if (recordRepository.existsBySessionSessionIdAndStudentStudentId(
                session.getSessionId(), studentId)) {
            throw new DuplicateScanException("Attendance already marked for this session");
        }

        // ── Check 5: Create and save the PRESENT record ──
        LocalDateTime scannedAt = LocalDateTime.now();
        AttendanceRecord record = AttendanceRecord.builder()
                .session(session)
                .student(student)
                .status(AttendanceStatus.PRESENT)
                .scannedAt(scannedAt)
                .build();

        try {
            recordRepository.save(record);
        } catch (DataIntegrityViolationException e) {
            // DB UNIQUE(session_id, student_id) constraint — race condition safety net
            throw new DuplicateScanException("Attendance already marked for this session");
        }

        // ── Build safe DTO inside the transaction while entities are still attached ──
        return AttendanceSuccessResponse.builder()
                .studentName(student.getFirstName() + " " + student.getLastName())
                .subjectName(subject.getName())
                .teacherName(session.getTeacher().getFirstName() + " " + session.getTeacher().getLastName())
                .sessionDate(session.getSessionDate())
                .scannedAt(scannedAt)
                .build();
    }

    /**
     * Calculate the academic year string from a date.
     * June-Dec → "currentYear" + last-two-digits-of-next-year  (e.g. "202526")
     * Jan-May  → "previousYear" + last-two-digits-of-current-year (e.g. "202526")
     */
    private String calculateAcademicYear(LocalDate date) {
        int year = date.getYear();
        if (date.getMonthValue() >= 6) {
            return String.valueOf(year) + String.valueOf(year + 1).substring(2);
        } else {
            return String.valueOf(year - 1) + String.valueOf(year).substring(2);
        }
    }
}

