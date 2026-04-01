package com.university.attendance.dto.response;

import com.university.attendance.models.AttendanceStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class SessionAttendanceRowResponse {
    private UUID studentId;
    private String studentName;
    private String studentPrn;
    private AttendanceStatus status;
    private LocalDateTime scannedAt; // will be null if ABSENT
}
