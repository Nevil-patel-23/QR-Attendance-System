package com.university.attendance.controller;

import com.university.attendance.dto.response.AttendanceSessionResponse;
import com.university.attendance.models.Teacher;
import com.university.attendance.security.JwtUtil;
import com.university.attendance.service.QrService;
import com.university.attendance.service.TeacherService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * REST endpoint for generating QR attendance sessions.
 *
 * POST /api/v1/attendance/generate?slotId={uuid}
 *
 * Only teachers can call this — the JWT role is checked by Spring Security.
 * Creates a new session, deactivates any previous one for the same slot+date.
 */
@RestController
@RequestMapping("/api/v1/attendance")
@RequiredArgsConstructor
public class AttendanceController {

    private final QrService qrService;
    private final TeacherService teacherService;
    private final JwtUtil jwtUtil;

    /**
     * Generate a new QR attendance session for a specific timetable slot.
     *
     * @param slotId the UUID of the timetable slot
     * @return session details including the QR token and scan counts
     */
    @PostMapping("/generate")
    public ResponseEntity<AttendanceSessionResponse> generateSession(
            @RequestParam UUID slotId, HttpServletRequest request) {

        UUID teacherId = resolveTeacherId(request);
        AttendanceSessionResponse response = qrService.generateSession(slotId, teacherId);
        return ResponseEntity.ok(response);
    }

    /**
     * Extract the teacher's entity ID from the JWT cookie.
     */
    private UUID resolveTeacherId(HttpServletRequest request) {
        String token = extractJwtFromCookie(request);
        UUID userId = jwtUtil.extractUserId(token);
        Teacher teacher = teacherService.getTeacherByUserId(userId);
        return teacher.getTeacherId();
    }

    private String extractJwtFromCookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if ("jwt".equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        throw new RuntimeException("JWT cookie not found");
    }
}
