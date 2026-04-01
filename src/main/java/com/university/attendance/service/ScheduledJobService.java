package com.university.attendance.service;

import com.university.attendance.models.*;
import com.university.attendance.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Background job that runs every 60 seconds.
 *
 * Finds sessions where expires_at < NOW() but is_active is still true.
 * For each expired session:
 *   1. Set is_active = false
 *   2. Find all students who SHOULD have scanned (eligible students)
 *   3. Subtract the students who DID scan (PRESENT records)
 *   4. Insert ABSENT records for the remainder
 *
 * The DataIntegrityViolationException catch is a safety net —
 * if two instances of this job overlap (shouldn't happen with fixedDelay),
 * the UNIQUE constraint on (session_id, student_id) prevents duplicates.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ScheduledJobService {

    private final AttendanceSessionRepository sessionRepo;
    private final AttendanceRecordRepository recordRepo;
    private final StudentRepository studentRepo;
    private final StudentSubjectEnrollmentRepository enrollmentRepo;

    @Scheduled(fixedDelay = 60000)
    @Transactional
    public void processExpiredSessions() {
        List<AttendanceSession> expired = sessionRepo.findByIsActiveTrueAndExpiresAtBefore(
                LocalDateTime.now());

        if (expired.isEmpty()) {
            return;
        }

        log.info("Processing {} expired session(s)", expired.size());

        for (AttendanceSession session : expired) {
            try {
                processOneSession(session);
            } catch (Exception e) {
                log.error("Error processing session {}: {}", session.getSessionId(), e.getMessage());
            }
        }
    }

    /**
     * Process a single expired session:
     * 1. Deactivate the session
     * 2. Get the set of students who already scanned (PRESENT)
     * 3. Get all eligible students for this subject
     * 4. Insert ABSENT records for students who didn't scan
     */
    private void processOneSession(AttendanceSession session) {
        // 1. Deactivate
        session.setIsActive(false);
        sessionRepo.save(session);

        // 2. Get student IDs who already have a record (PRESENT)
        Set<UUID> presentStudentIds = recordRepo
                .findBySessionSessionId(session.getSessionId())
                .stream()
                .map(record -> record.getStudent().getStudentId())
                .collect(Collectors.toSet());

        // 3. Get all eligible students
        List<Student> eligibleStudents = getEligibleStudents(session);

        // 4. Insert ABSENT records for students who didn't scan
        int absentCount = 0;
        for (Student student : eligibleStudents) {
            if (!presentStudentIds.contains(student.getStudentId())) {
                try {
                    AttendanceRecord absentRecord = AttendanceRecord.builder()
                            .session(session)
                            .student(student)
                            .scannedAt(null)
                            .status(AttendanceStatus.ABSENT)
                            .build();
                    recordRepo.save(absentRecord);
                    absentCount++;
                } catch (DataIntegrityViolationException e) {
                    // Safety net: UNIQUE constraint prevents duplicates
                    // This only happens if there's a race condition, which
                    // is extremely unlikely with fixedDelay scheduling
                    log.warn("Duplicate record for session {} student {} — skipping",
                            session.getSessionId(), student.getStudentId());
                }
            }
        }

        log.info("Session {} expired: {} present, {} absent",
                session.getSessionId(), presentStudentIds.size(), absentCount);
    }

    /**
     * Get the list of students who are eligible for a session's subject.
     * - COMPULSORY: all active students in the subject's semester
     * - ELECTIVE: only students enrolled in the elective for the academic year
     */
    private List<Student> getEligibleStudents(AttendanceSession session) {
        Subject subject = session.getSubject();
        Semester semester = session.getSemester();

        if (subject.getType() == SubjectType.COMPULSORY) {
            String academicYear = session.getAllocation().getAcademicYear();
            Integer batchYear = Integer.parseInt(academicYear);
            return studentRepo.findByCurrentSemesterSemesterIdAndBatchYearAndUserIsActiveTrue(
                    semester.getSemesterId(), batchYear);
        } else {
            return enrollmentRepo.findBySubjectSubjectIdAndAcademicYear(
                            subject.getSubjectId(), session.getAllocation().getAcademicYear())
                    .stream()
                    .map(StudentSubjectEnrollment::getStudent)
                    .collect(Collectors.toList());
        }
    }
}
