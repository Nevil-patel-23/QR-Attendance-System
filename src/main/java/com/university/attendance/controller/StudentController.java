package com.university.attendance.controller;

import com.university.attendance.dto.response.*;
import com.university.attendance.service.StudentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Student REST endpoints. All methods derive student identity from JWT
 * via SecurityContextHolder — never from URL path parameters.
 */
@RestController
@RequestMapping("/api/v1/student")
@RequiredArgsConstructor
public class StudentController {

    private final StudentService studentService;

    @GetMapping("/dashboard")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<StudentDashboardResponse> getDashboard() {
        String prn = getCurrentPrn();
        return ResponseEntity.ok(studentService.getDashboard(prn));
    }

    @GetMapping("/attendance/{subjectId}")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<List<AttendanceRecordResponse>> getAttendanceDetail(
            @PathVariable UUID subjectId) {
        String prn = getCurrentPrn();
        return ResponseEntity.ok(studentService.getAttendanceDetail(prn, subjectId));
    }

    @GetMapping("/timetable")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<List<TimetableSlotResponse>> getTimetable() {
        String prn = getCurrentPrn();
        return ResponseEntity.ok(studentService.getTimetable(prn));
    }

    @GetMapping("/subjects")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<List<StudentSubjectResponse>> getMySubjects() {
        String prn = getCurrentPrn();
        return ResponseEntity.ok(studentService.getMySubjects(prn));
    }

    private String getCurrentPrn() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }
}
