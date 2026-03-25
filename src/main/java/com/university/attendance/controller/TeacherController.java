package com.university.attendance.controller;

import com.university.attendance.dto.response.AttendanceSessionResponse;
import com.university.attendance.dto.response.TodaySlotResponse;
import com.university.attendance.models.Teacher;
import com.university.attendance.security.JwtUtil;
import com.university.attendance.service.QrService;
import com.university.attendance.service.TeacherService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST endpoints for teachers.
 *
 * GET  /api/v1/teacher/today-slots        — today's timetable with QR button state
 * GET  /api/v1/teacher/session/{id}/status — live scan counter for a session
 *
 * Both endpoints resolve the teacher from the JWT "userId" claim.
 * The teacher must be logged in (JWT cookie present).
 */
@RestController
@RequestMapping("/api/v1/teacher")
@RequiredArgsConstructor
public class TeacherController {

    private final TeacherService teacherService;
    private final QrService qrService;
    private final com.university.attendance.repository.UserRepository userRepository;

    /**
     * Get today's timetable slots for the logged-in teacher.
     * Each slot includes:
     *   - isWithinWindow: can Generate QR be clicked now?
     *   - hasActiveSession: is there already an active QR?
     *   - activeSessionId: the session UUID if active
     */
    @GetMapping("/today-slots")
    public ResponseEntity<List<TodaySlotResponse>> getTodaySlots() {
        UUID teacherId = resolveTeacherId();
        List<TodaySlotResponse> slots = teacherService.getTodaySlots(teacherId);
        return ResponseEntity.ok(slots);
    }

    /**
     * Get live session status (present count / total students).
     * Called by the UI every 5 seconds to update the scan counter.
     */
    @GetMapping("/session/{sessionId}/status")
    public ResponseEntity<AttendanceSessionResponse> getSessionStatus(
            @PathVariable UUID sessionId) {
        AttendanceSessionResponse status = qrService.getSessionStatus(sessionId);
        return ResponseEntity.ok(status);
    }

    /**
     * Extract the teacher's entity ID using the authenticated principal (PRN).
     */
    private UUID resolveTeacherId() {
        String prn = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getName();
        com.university.attendance.models.User user = userRepository.findByPrn(prn).orElseThrow(() -> new RuntimeException("User not found"));
        Teacher teacher = teacherService.getTeacherByUserId(user.getUserId());
        return teacher.getTeacherId();
    }
}
