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
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

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

    /**
     * Resolve a Teacher entity from the JWT's userId claim.
     * The JWT stores user_id, not teacher_id, so we need this lookup.
     */
    @Transactional(readOnly = true)
    public Teacher getTeacherByUserId(UUID userId) {
        return teacherRepo.findByUserUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Teacher profile not found for user"));
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
}
