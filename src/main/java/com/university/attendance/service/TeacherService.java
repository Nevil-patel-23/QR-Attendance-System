package com.university.attendance.service;

import com.university.attendance.dto.response.TodaySlotResponse;
import com.university.attendance.exception.ResourceNotFoundException;
import com.university.attendance.models.*;
import com.university.attendance.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;
import com.university.attendance.exception.ValidationException;
import com.university.attendance.dto.response.*;

/**
 * Service layer for teacher-specific operations.
 *
 * getTodaySlots() is the main method — it fetches today's timetable
 * slots for a teacher and enriches each one with:
 *   - isWithinWindow:    can the teacher generate a QR right now?
 *   - hasActiveSession:  is there already an active QR for this slot?
 *   - activeSessionId:   if yes, what's the session ID?
 *
 * Window logic (confirmed by user):
 *   Active if currentTime >= (slot.startTime - 15 min)
 *          AND currentTime <= (slot.endTime + 30 min)
 */
@Service
@RequiredArgsConstructor
public class TeacherService {

    private final TeacherRepository teacherRepo;
    private final TimetableSlotRepository slotRepo;
    private final AttendanceSessionRepository sessionRepo;
    private final HolidayRepository holidayRepo;
    private final TeacherSubjectAllocationRepository allocationRepository;
    private final AttendanceRecordRepository recordRepository;
    private final StudentSubjectEnrollmentRepository enrollmentRepository;
    private final StudentRepository studentRepository;

    /**
     * Resolve a Teacher entity from the JWT's userId claim.
     * The JWT stores user_id, not teacher_id, so we need this lookup.
     */
    /**
     * Resolve a Teacher entity from the JWT's userId claim, returning an unproxied object
     * containing only simple strings to prevent LazyInitializationException in the UI layer.
     */
    @Transactional(readOnly = true)
    public Teacher getTeacherByUserId(UUID userId) {
        Teacher dbTeacher = teacherRepo.findByUserUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Teacher profile not found for user"));
        
        Teacher unproxied = new Teacher();
        unproxied.setTeacherId(dbTeacher.getTeacherId());
        unproxied.setFirstName(dbTeacher.getFirstName());
        unproxied.setLastName(dbTeacher.getLastName());
        unproxied.setPrn(dbTeacher.getPrn());
        unproxied.setDesignation(dbTeacher.getDesignation());
        
        User user = new User();
        user.setIsActive(dbTeacher.getUser().getIsActive());
        unproxied.setUser(user);
        
        if (dbTeacher.getFaculty() != null) {
            Faculty f = new Faculty();
            f.setName(dbTeacher.getFaculty().getName());
            unproxied.setFaculty(f);
        }
        
        return unproxied;
    }

    /**
     * Get today's timetable slots for a teacher, enriched with QR generation info.
     *
     * Steps:
     * 1. Check if today is a holiday — if yes, return empty list
     * 2. Map today's java.time day to our custom DayOfWeek enum
     * 3. Query active slots for this teacher + day
     * 4. For each slot, compute window status and check for active sessions
     *
     * @param teacherId the teacher's UUID (from teachers table)
     * @return list of TodaySlotResponse with window and session info
     */
    @Transactional(readOnly = true)
    public List<TodaySlotResponse> getTodaySlots(UUID teacherId) {
        LocalDate today = LocalDate.now();

        // 1. Holiday check — no attendance on holidays
        if (holidayRepo.existsByDate(today)) {
            return List.of();
        }

        // 2. Map java.time.DayOfWeek → our custom DayOfWeek enum
        DayOfWeek dayEnum = mapToDayOfWeek(today.getDayOfWeek());
        if (dayEnum == null) {
            // Sunday — no lectures
            return List.of();
        }

        // 3. Get active slots for this teacher and day
        List<TimetableSlot> slots = slotRepo
                .findByDayOfWeekAndAllocationTeacherTeacherIdAndEffectiveToIsNull(dayEnum, teacherId);

        // 4. Enrich each slot with window + session info
        LocalTime now = LocalTime.now();
        return slots.stream()
                .map(slot -> buildSlotResponse(slot, today, now))
                .collect(Collectors.toList());
    }

    /**
     * Build a TodaySlotResponse from a TimetableSlot entity.
     * Navigates the allocation chain: slot → allocation → subject/semester/course.
     */
    private TodaySlotResponse buildSlotResponse(TimetableSlot slot, LocalDate today, LocalTime now) {
        TeacherSubjectAllocation allocation = slot.getAllocation();
        Subject subject = allocation.getSubject();
        Semester semester = allocation.getSemester();

        // Window: active from startTime-15min to endTime+30min
        boolean isWithinWindow = !now.isBefore(slot.getStartTime().minusMinutes(15))
                && !now.isAfter(slot.getEndTime().plusMinutes(30));

        // Check for an active session for this slot today
        var activeSession = sessionRepo.findBySlotSlotIdAndSessionDateAndIsActiveTrue(
                slot.getSlotId(), today);

        return TodaySlotResponse.builder()
                .slotId(slot.getSlotId())
                .subjectName(subject.getName())
                .subjectCode(subject.getCode())
                .semesterLabel(semester.getLabel())
                .courseName(semester.getCourse().getName())
                .dayOfWeek(slot.getDayOfWeek().name())
                .startTime(slot.getStartTime())
                .endTime(slot.getEndTime())
                .room(slot.getRoom())
                .isWithinWindow(isWithinWindow)
                .hasActiveSession(activeSession.isPresent())
                .activeSessionId(activeSession.map(s -> s.getSessionId()).orElse(null))
                .build();
    }

    /**
     * Map java.time.DayOfWeek (MONDAY, TUESDAY...) to our custom enum (MON, TUE...).
     * Returns null for SUNDAY since we don't have lectures on Sundays.
     */
    private DayOfWeek mapToDayOfWeek(java.time.DayOfWeek javaDay) {
        return switch (javaDay) {
            case MONDAY -> DayOfWeek.MON;
            case TUESDAY -> DayOfWeek.TUE;
            case WEDNESDAY -> DayOfWeek.WED;
            case THURSDAY -> DayOfWeek.THU;
            case FRIDAY -> DayOfWeek.FRI;
            case SATURDAY -> DayOfWeek.SAT;
            case SUNDAY -> null;
        };
    }

    private Teacher getTeacherByPrn(String prn) {
        return teacherRepo.findByPrn(prn)
                .orElseThrow(() -> new ValidationException("Teacher not found"));
    }

    private void validateTeacherAllocation(UUID teacherId, UUID subjectId) {
        boolean isAllocated = allocationRepository.findByTeacherTeacherId(teacherId)
                .stream()
                .anyMatch(a -> a.getSubject().getSubjectId().equals(subjectId));
        if (!isAllocated) {
            throw new ValidationException("You are not authorized for this subject");
        }
    }

    @Transactional(readOnly = true)
    public List<TeacherSubjectResponse> getMySubjects(String prn) {
        Teacher teacher = getTeacherByPrn(prn);
        return allocationRepository.findByTeacherTeacherId(teacher.getTeacherId()).stream()
                .map(allocation -> TeacherSubjectResponse.builder()
                        .allocationId(allocation.getAllocationId())
                        .subjectId(allocation.getSubject().getSubjectId())
                        .subjectName(allocation.getSubject().getName())
                        .subjectCode(allocation.getSubject().getCode())
                        .semesterLabel(allocation.getSemester().getLabel())
                        .courseCode(allocation.getSemester().getCourse().getCode())
                        .academicYear(allocation.getAcademicYear())
                        .build())
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<SessionSummaryResponse> getSessionsForSubject(String prn, UUID subjectId, LocalDate fromDate, LocalDate toDate) {
        Teacher teacher = getTeacherByPrn(prn);
        validateTeacherAllocation(teacher.getTeacherId(), subjectId);

        List<AttendanceSession> sessions = sessionRepo.findBySubjectSubjectIdAndIsActiveFalseAndSessionDateBetweenOrderBySessionDateAsc(
                subjectId, fromDate, toDate);

        return sessions.stream().map(session -> {
            long presentCount = sessionRepo.countBySessionIdAndStatus(session.getSessionId(), AttendanceStatus.PRESENT);
            
            TimetableSlot slot = session.getSlot();
            
            long totalEnrolled = 0;
            if (session.getSubject().getType() == SubjectType.COMPULSORY) {
                String academicYear = session.getAllocation().getAcademicYear();
                Integer batchYear = Integer.parseInt(academicYear);
                totalEnrolled = studentRepository.findByCurrentSemesterSemesterIdAndBatchYearAndUserIsActiveTrue(
                        session.getSemester().getSemesterId(), batchYear).size();
            } else {
                totalEnrolled = enrollmentRepository.findBySubjectSubjectIdAndAcademicYear(
                        subjectId, session.getAllocation().getAcademicYear()).size();
            }

            return SessionSummaryResponse.builder()
                    .sessionId(session.getSessionId())
                    .sessionDate(session.getSessionDate())
                    .startTime(slot != null ? slot.getStartTime() : null)
                    .endTime(slot != null ? slot.getEndTime() : null)
                    .subjectName(session.getSubject().getName())
                    .subjectCode(session.getSubject().getCode())
                    .presentCount(presentCount)
                    .totalEnrolled(totalEnrolled)
                    .build();
        }).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<StudentAttendanceRowResponse> getAttendanceBySubject(String prn, UUID subjectId, LocalDate fromDate, LocalDate toDate) {
        Teacher teacher = getTeacherByPrn(prn);
        validateTeacherAllocation(teacher.getTeacherId(), subjectId);

        List<AttendanceSession> sessions = sessionRepo.findBySubjectSubjectIdAndIsActiveFalseAndSessionDateBetweenOrderBySessionDateAsc(
                subjectId, fromDate, toDate);
        
        if (sessions.isEmpty()) return Collections.emptyList();
        
        List<UUID> sessionIds = sessions.stream().map(AttendanceSession::getSessionId).collect(Collectors.toList());
        List<AttendanceRecord> bulkRecords = recordRepository.findBySessionSessionIdIn(sessionIds);

        AttendanceSession firstSession = sessions.get(0);
        List<Student> enrolledStudents;
        if (firstSession.getSubject().getType() == SubjectType.COMPULSORY) {
            String academicYear = firstSession.getAllocation().getAcademicYear();
            Integer batchYear = Integer.parseInt(academicYear);
            enrolledStudents = studentRepository.findByCurrentSemesterSemesterIdAndBatchYearAndUserIsActiveTrue(
                    firstSession.getSemester().getSemesterId(), batchYear);
        } else {
            enrolledStudents = enrollmentRepository.findBySubjectSubjectIdAndAcademicYear(
                            subjectId, firstSession.getAllocation().getAcademicYear())
                    .stream()
                    .map(StudentSubjectEnrollment::getStudent)
                    .collect(Collectors.toList());
        }

        int totalSessions = sessions.size();
        List<StudentAttendanceRowResponse> response = new ArrayList<>();

        for (Student student : enrolledStudents) {
            Map<UUID, AttendanceStatus> statuses = new HashMap<>();
            int presentCount = 0;

            for (AttendanceSession s : sessions) {
                Optional<AttendanceRecord> recOptional = bulkRecords.stream()
                        .filter(r -> r.getSession().getSessionId().equals(s.getSessionId()) && 
                                     r.getStudent().getStudentId().equals(student.getStudentId()))
                        .findFirst();

                if (recOptional.isPresent()) {
                    AttendanceStatus status = recOptional.get().getStatus();
                    statuses.put(s.getSessionId(), status);
                    if (status == AttendanceStatus.PRESENT) presentCount++;
                } else {
                    statuses.put(s.getSessionId(), AttendanceStatus.ABSENT);
                }
            }

            double percentage = totalSessions == 0 ? 0.0 : ((double) presentCount / totalSessions) * 100.0;
            percentage = Math.round(percentage * 100.0) / 100.0;

            response.add(StudentAttendanceRowResponse.builder()
                    .studentId(student.getStudentId())
                    .studentName(student.getFirstName() + " " + student.getLastName())
                    .studentPrn(student.getPrn())
                    .presentCount(presentCount)
                    .totalSessions(totalSessions)
                    .attendancePercentage(percentage)
                    .isAtRisk(percentage < 75.0 && totalSessions > 0)
                    .sessionStatuses(statuses)
                    .build());
        }

        response.sort(Comparator.comparing(StudentAttendanceRowResponse::getStudentPrn));
        return response;
    }

    @Transactional(readOnly = true)
    public List<SessionAttendanceRowResponse> getAttendanceBySession(String prn, UUID sessionId) {
        Teacher teacher = getTeacherByPrn(prn);
        AttendanceSession session = sessionRepo.findById(sessionId)
                .orElseThrow(() -> new ValidationException("Session not found"));
        
        if (!session.getTeacher().getTeacherId().equals(teacher.getTeacherId())) {
            throw new ValidationException("You are not authorized for this session");
        }

        List<AttendanceRecord> sessionRecords = recordRepository.findBySessionSessionId(sessionId);

        List<Student> enrolledStudents;
        if (session.getSubject().getType() == SubjectType.COMPULSORY) {
            String academicYear = session.getAllocation().getAcademicYear();
            Integer batchYear = Integer.parseInt(academicYear);
            enrolledStudents = studentRepository.findByCurrentSemesterSemesterIdAndBatchYearAndUserIsActiveTrue(
                    session.getSemester().getSemesterId(), batchYear);
        } else {
            enrolledStudents = enrollmentRepository.findBySubjectSubjectIdAndAcademicYear(
                            session.getSubject().getSubjectId(), session.getAllocation().getAcademicYear())
                    .stream()
                    .map(StudentSubjectEnrollment::getStudent)
                    .collect(Collectors.toList());
        }

        List<SessionAttendanceRowResponse> response = new ArrayList<>();

        for (Student student : enrolledStudents) {
            Optional<AttendanceRecord> record = sessionRecords.stream()
                    .filter(r -> r.getStudent().getStudentId().equals(student.getStudentId()))
                    .findFirst();

            AttendanceStatus status = record.map(AttendanceRecord::getStatus).orElse(AttendanceStatus.ABSENT);
            java.time.LocalDateTime scannedAt = record.map(AttendanceRecord::getScannedAt).orElse(null);

            response.add(SessionAttendanceRowResponse.builder()
                    .studentId(student.getStudentId())
                    .studentName(student.getFirstName() + " " + student.getLastName())
                    .studentPrn(student.getPrn())
                    .status(status)
                    .scannedAt(scannedAt)
                    .build());
        }

        response.sort(Comparator.comparing(SessionAttendanceRowResponse::getStudentPrn));
        return response;
    }

    @Transactional(readOnly = true)
    public List<TeacherTimetableSlotResponse> getMyTimetable(String prn) {
        Teacher teacher = getTeacherByPrn(prn);
        List<TeacherTimetableSlotResponse> response = new ArrayList<>();
        
        List<TimetableSlot> slots = slotRepo.findByAllocationTeacherTeacherIdAndEffectiveToIsNull(teacher.getTeacherId());
        
        for (TimetableSlot slot : slots) {
            TeacherSubjectAllocation alloc = slot.getAllocation();
            response.add(TeacherTimetableSlotResponse.builder()
                    .slotId(slot.getSlotId())
                    .dayOfWeek(slot.getDayOfWeek())
                    .startTime(slot.getStartTime())
                    .endTime(slot.getEndTime())
                    .room(slot.getRoom())
                    .subjectName(alloc.getSubject().getName())
                    .subjectCode(alloc.getSubject().getCode())
                    .semesterLabel(alloc.getSemester().getLabel())
                    .build());
        }

        response.sort(Comparator.comparing(TeacherTimetableSlotResponse::getDayOfWeek)
                .thenComparing(TeacherTimetableSlotResponse::getStartTime));

        return response;
    }
}
