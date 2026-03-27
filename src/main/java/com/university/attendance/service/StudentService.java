package com.university.attendance.service;

import com.university.attendance.dto.response.*;
import com.university.attendance.models.*;
import com.university.attendance.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Student-facing service. All methods receive the student's PRN
 * (extracted from JWT by the controller) and never accept studentId
 * from the URL. Handles dashboard, attendance, timetable, and subjects.
 */
@Service
@RequiredArgsConstructor
public class StudentService {

    private final StudentRepository studentRepository;
    private final SubjectRepository subjectRepository;
    private final StudentSubjectEnrollmentRepository enrollmentRepository;
    private final AttendanceSessionRepository sessionRepository;
    private final AttendanceRecordRepository recordRepository;
    private final TimetableSlotRepository slotRepository;
    private final TeacherSubjectAllocationRepository allocationRepository;
    private final AcademicCalendarRepository calendarRepository;
    private final HolidayRepository holidayRepository;

    // ─── Dashboard ──────────────────────────────────────────────

    /**
     * Build the full student dashboard: profile strip, attendance
     * summary cards for every subject, and today's timetable slots.
     */
    @Transactional(readOnly = true)
    public StudentDashboardResponse getDashboard(String prn) {
        Student student = findStudentByPrn(prn);
        Semester semester = student.getCurrentSemester();
        Course course = semester.getCourse();
        String academicYear = calculateAcademicYear(LocalDate.now());

        // Resolve calendar date range (graceful if missing)
        LocalDate calStart = null;
        LocalDate calEnd = null;
        Optional<AcademicCalendar> calendar = findCurrentCalendar(course.getCourseId(),
                semester.getSemesterNumber(), academicYear);
        if (calendar.isPresent()) {
            calStart = calendar.get().getStartDate();
            calEnd = calendar.get().getEndDate();
        }

        // Gather all subjects (compulsory + elective)
        List<Subject> subjects = getAllSubjectsForStudent(semester.getSemesterId(),
                student.getStudentId(), academicYear);

        // Build attendance summaries
        List<AttendanceSummaryResponse> summaries = new ArrayList<>();
        boolean hasAtRisk = false;
        for (Subject subject : subjects) {
            AttendanceSummaryResponse summary = buildAttendanceSummary(
                    subject, student.getStudentId(), calStart, calEnd);
            summaries.add(summary);
            if (summary.isAtRisk()) {
                hasAtRisk = true;
            }
        }

        // Today's timetable
        List<TimetableSlotResponse> todaySlots = getTodaySlotsForStudent(subjects, academicYear);

        return StudentDashboardResponse.builder()
                .studentName(student.getFirstName() + " " + student.getLastName())
                .prn(student.getPrn())
                .courseName(course.getName())
                .currentSemesterLabel(semester.getLabel())
                .academicYear(academicYear)
                .attendanceSummaries(summaries)
                .hasAtRiskSubjects(hasAtRisk)
                .todaySlots(todaySlots)
                .build();
    }

    // ─── Attendance Detail ──────────────────────────────────────

    /**
     * Get every attendance record for a student in a specific subject,
     * ordered by session date ascending.
     */
    @Transactional(readOnly = true)
    public List<AttendanceRecordResponse> getAttendanceDetail(String prn, UUID subjectId) {
        Student student = findStudentByPrn(prn);

        List<AttendanceRecord> records = recordRepository
                .findByStudentStudentIdAndSessionSubjectSubjectId(
                        student.getStudentId(), subjectId);

        return records.stream()
                .sorted(Comparator.comparing(r -> r.getSession().getSessionDate()))
                .map(r -> AttendanceRecordResponse.builder()
                        .recordId(r.getRecordId())
                        .sessionDate(r.getSession().getSessionDate())
                        .sessionStartTime(r.getSession().getSlot().getStartTime())
                        .subjectName(r.getSession().getSubject().getName())
                        .status(r.getStatus())
                        .scannedAt(r.getScannedAt())
                        .build())
                .collect(Collectors.toList());
    }

    /**
     * Get the attendance summary for a single subject.
     */
    @Transactional(readOnly = true)
    public AttendanceSummaryResponse getAttendanceSummaryForSubject(String prn, UUID subjectId) {
        Student student = findStudentByPrn(prn);
        Subject subject = subjectRepository.findById(subjectId)
                .orElseThrow(() -> new RuntimeException("Subject not found"));

        Semester semester = student.getCurrentSemester();
        Course course = semester.getCourse();
        String academicYear = calculateAcademicYear(LocalDate.now());

        LocalDate calStart = null;
        LocalDate calEnd = null;
        Optional<AcademicCalendar> calendar = findCurrentCalendar(course.getCourseId(),
                semester.getSemesterNumber(), academicYear);
        if (calendar.isPresent()) {
            calStart = calendar.get().getStartDate();
            calEnd = calendar.get().getEndDate();
        }

        return buildAttendanceSummary(subject, student.getStudentId(), calStart, calEnd);
    }

    // ─── Timetable ──────────────────────────────────────────────

    /**
     * Get the full weekly timetable for a student, covering all their
     * subjects (compulsory + elective). Holidays are not filtered here
     * because this returns the recurring weekly schedule.
     */
    @Transactional(readOnly = true)
    public List<TimetableSlotResponse> getTimetable(String prn) {
        Student student = findStudentByPrn(prn);
        Semester semester = student.getCurrentSemester();
        String academicYear = calculateAcademicYear(LocalDate.now());

        List<Subject> subjects = getAllSubjectsForStudent(semester.getSemesterId(),
                student.getStudentId(), academicYear);

        List<TimetableSlotResponse> result = new ArrayList<>();
        for (Subject subject : subjects) {
            List<TimetableSlot> slots = slotRepository
                    .findByAllocationSubjectSubjectIdAndEffectiveToIsNull(subject.getSubjectId());
            for (TimetableSlot slot : slots) {
                TeacherSubjectAllocation alloc = slot.getAllocation();
                Teacher teacher = alloc.getTeacher();
                result.add(TimetableSlotResponse.builder()
                        .slotId(slot.getSlotId())
                        .dayOfWeek(slot.getDayOfWeek())
                        .startTime(slot.getStartTime())
                        .endTime(slot.getEndTime())
                        .room(slot.getRoom())
                        .subjectName(subject.getName())
                        .subjectCode(subject.getCode())
                        .teacherName(teacher.getFirstName() + " " + teacher.getLastName())
                        .build());
            }
        }

        // Sort by day of week ordinal, then start time
        result.sort(Comparator.comparing((TimetableSlotResponse s) -> s.getDayOfWeek().ordinal())
                .thenComparing(TimetableSlotResponse::getStartTime));
        return result;
    }

    // ─── My Subjects ────────────────────────────────────────────

    /**
     * Get all subjects for the student: compulsory first, then elective.
     * Each includes teacher name (or "Not assigned").
     */
    @Transactional(readOnly = true)
    public List<StudentSubjectResponse> getMySubjects(String prn) {
        Student student = findStudentByPrn(prn);
        Semester semester = student.getCurrentSemester();
        String academicYear = calculateAcademicYear(LocalDate.now());

        List<Subject> compulsory = subjectRepository
                .findBySemesterSemesterIdAndType(semester.getSemesterId(), SubjectType.COMPULSORY);
        List<Subject> electives = getElectiveSubjects(student.getStudentId(), academicYear);

        List<StudentSubjectResponse> result = new ArrayList<>();
        for (Subject s : compulsory) {
            result.add(mapToSubjectResponse(s, academicYear));
        }
        for (Subject s : electives) {
            result.add(mapToSubjectResponse(s, academicYear));
        }
        return result;
    }

    // ─── Private helpers ────────────────────────────────────────

    private Student findStudentByPrn(String prn) {
        return studentRepository.findByPrn(prn)
                .orElseThrow(() -> new RuntimeException("Student not found for PRN: " + prn));
    }

    /**
     * Combines COMPULSORY subjects from the semester + ELECTIVE subjects
     * the student is enrolled in for the current academic year.
     */
    private List<Subject> getAllSubjectsForStudent(UUID semesterId, UUID studentId, String academicYear) {
        List<Subject> compulsory = subjectRepository
                .findBySemesterSemesterIdAndType(semesterId, SubjectType.COMPULSORY);
        List<Subject> electives = getElectiveSubjects(studentId, academicYear);

        List<Subject> all = new ArrayList<>(compulsory);
        all.addAll(electives);
        return all;
    }

    private List<Subject> getElectiveSubjects(UUID studentId, String academicYear) {
        List<StudentSubjectEnrollment> enrollments = enrollmentRepository
                .findByStudentStudentIdAndAcademicYear(studentId, academicYear);
        return enrollments.stream()
                .map(StudentSubjectEnrollment::getSubject)
                .collect(Collectors.toList());
    }

    /**
     * Build attendance summary for one subject. Calculates percentage,
     * at-risk flag, classes can miss, and classes still needed.
     */
    private AttendanceSummaryResponse buildAttendanceSummary(
            Subject subject, UUID studentId, LocalDate calStart, LocalDate calEnd) {

        int totalSessions = 0;
        int presentCount = 0;

        if (calStart != null && calEnd != null) {
            totalSessions = (int) sessionRepository.countBySubjectSubjectIdAndIsActiveFalseAndSessionDateBetween(
                    subject.getSubjectId(), calStart, calEnd);

            List<AttendanceRecord> records = recordRepository
                    .findByStudentStudentIdAndSessionSubjectSubjectId(studentId, subject.getSubjectId());
            presentCount = (int) records.stream()
                    .filter(r -> r.getStatus() == AttendanceStatus.PRESENT)
                    .filter(r -> {
                        LocalDate d = r.getSession().getSessionDate();
                        return !d.isBefore(calStart) && !d.isAfter(calEnd);
                    })
                    .count();
        }

        double percentage = 0.0;
        if (totalSessions > 0) {
            percentage = BigDecimal.valueOf((double) presentCount / totalSessions * 100)
                    .setScale(2, RoundingMode.HALF_UP).doubleValue();
        }

        boolean atRisk = percentage < 75.0 && totalSessions > 0;

        // classesCanMiss: how many more absences before dropping below 75%
        // classesCanMiss = presentCount - CEIL(totalSessions * 0.75)
        int classesCanMiss = 0;
        // classesNeeded: solves (present + x) / (total + x) >= 0.75
        // → x = CEIL((0.75 * total - present) / 0.25)
        int classesNeeded = 0;
        if (totalSessions > 0) {
            int minRequired = (int) Math.ceil(totalSessions * 0.75);
            if (presentCount >= minRequired) {
                classesCanMiss = presentCount - minRequired;
            } else {
                double required = (0.75 * totalSessions - presentCount) / 0.25;
                classesNeeded = (int) Math.ceil(required);
                if (classesNeeded < 0) classesNeeded = 0;
            }
        }

        return AttendanceSummaryResponse.builder()
                .subjectId(subject.getSubjectId())
                .subjectName(subject.getName())
                .subjectCode(subject.getCode())
                .subjectType(subject.getType())
                .totalSessions(totalSessions)
                .presentCount(presentCount)
                .attendancePercentage(percentage)
                .isAtRisk(atRisk)
                .classesCanMiss(classesCanMiss)
                .classesNeeded(classesNeeded)
                .build();
    }

    /**
     * Get today's timetable slots for the student's subjects.
     * Checks if today is a holiday — if so, returns empty list.
     */
    private List<TimetableSlotResponse> getTodaySlotsForStudent(
            List<Subject> subjects, String academicYear) {

        LocalDate today = LocalDate.now();

        // If today is a holiday, no classes
        if (holidayRepository.existsByDate(today)) {
            return Collections.emptyList();
        }

        // Map Java day-of-week to our DayOfWeek enum
        DayOfWeek todayDow = mapJavaDayOfWeek(today.getDayOfWeek());
        if (todayDow == null) {
            return Collections.emptyList(); // Sunday
        }

        List<TimetableSlotResponse> result = new ArrayList<>();
        for (Subject subject : subjects) {
            List<TimetableSlot> slots = slotRepository
                    .findByAllocationSubjectSubjectIdAndEffectiveToIsNull(subject.getSubjectId());
            for (TimetableSlot slot : slots) {
                if (slot.getDayOfWeek() == todayDow) {
                    TeacherSubjectAllocation alloc = slot.getAllocation();
                    Teacher teacher = alloc.getTeacher();
                    result.add(TimetableSlotResponse.builder()
                            .slotId(slot.getSlotId())
                            .dayOfWeek(todayDow)
                            .startTime(slot.getStartTime())
                            .endTime(slot.getEndTime())
                            .room(slot.getRoom())
                            .subjectName(subject.getName())
                            .subjectCode(subject.getCode())
                            .teacherName(teacher.getFirstName() + " " + teacher.getLastName())
                            .build());
                }
            }
        }
        result.sort(Comparator.comparing(TimetableSlotResponse::getStartTime));
        return result;
    }

    /**
     * Map subject to StudentSubjectResponse, looking up teacher from allocation.
     */
    private StudentSubjectResponse mapToSubjectResponse(Subject subject, String academicYear) {
        String teacherName = "Not assigned";
        List<TeacherSubjectAllocation> allocations = allocationRepository
                .findBySubjectSubjectIdAndAcademicYear(subject.getSubjectId(), academicYear);
        if (!allocations.isEmpty()) {
            Teacher t = allocations.get(0).getTeacher();
            teacherName = t.getFirstName() + " " + t.getLastName();
        }

        return StudentSubjectResponse.builder()
                .subjectId(subject.getSubjectId())
                .subjectName(subject.getName())
                .subjectCode(subject.getCode())
                .subjectType(subject.getType())
                .credits(subject.getCredits())
                .teacherName(teacherName)
                .build();
    }

    /**
     * Find the academic calendar entry for the student's current semester.
     * semester_number in academic_calendars is 1 (odd) or 2 (even),
     * derived from absolute semester number: odd semester → 1, even → 2.
     */
    private Optional<AcademicCalendar> findCurrentCalendar(
            UUID courseId, int absoluteSemesterNumber, String academicYear) {
        int calSemNumber = (absoluteSemesterNumber % 2 == 0) ? 2 : 1;
        return calendarRepository.findByCourseCourseIdAndAcademicYearAndSemesterNumber(
                courseId, academicYear, calSemNumber);
    }

    /**
     * Map java.time.DayOfWeek to our custom DayOfWeek enum.
     * Returns null for SUNDAY (no classes).
     */
    private DayOfWeek mapJavaDayOfWeek(java.time.DayOfWeek javaDow) {
        return switch (javaDow) {
            case MONDAY -> DayOfWeek.MON;
            case TUESDAY -> DayOfWeek.TUE;
            case WEDNESDAY -> DayOfWeek.WED;
            case THURSDAY -> DayOfWeek.THU;
            case FRIDAY -> DayOfWeek.FRI;
            case SATURDAY -> DayOfWeek.SAT;
            case SUNDAY -> null;
        };
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
