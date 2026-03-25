package com.university.attendance.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO returned when a teacher generates or checks a QR session.
 * Contains session details plus live scan counts.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AttendanceSessionResponse {
    private UUID sessionId;
    private String qrToken;
    private String subjectName;
    private String subjectCode;
    private String semesterLabel;
    private LocalDate sessionDate;
    private LocalDateTime generatedAt;
    private LocalDateTime expiresAt;
    private boolean isActive;
    private int presentCount;
    private int totalStudents;
}
