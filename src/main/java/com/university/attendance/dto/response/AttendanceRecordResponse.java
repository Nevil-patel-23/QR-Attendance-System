package com.university.attendance.dto.response;

import com.university.attendance.models.AttendanceStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;

/**
 * Single attendance record row for the detail view.
 * Shows date, time, subject name, status badge, and scan timestamp.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AttendanceRecordResponse {
    private UUID recordId;
    private LocalDate sessionDate;
    private LocalTime sessionStartTime;
    private String subjectName;
    private AttendanceStatus status;
    private LocalDateTime scannedAt;
}
