package com.university.attendance.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Safe DTO returned from AttendanceService.recordAttendance().
 * Contains only flat strings — no Hibernate proxies — so Vaadin
 * can render it safely outside the transaction boundary.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AttendanceSuccessResponse {
    private String studentName;
    private String subjectName;
    private String teacherName;
    private LocalDate sessionDate;
    private LocalDateTime scannedAt;
}
