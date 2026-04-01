package com.university.attendance.dto.response;

import com.university.attendance.models.AttendanceStatus;
import lombok.Builder;
import lombok.Data;

import java.util.Map;
import java.util.UUID;

@Data
@Builder
public class StudentAttendanceRowResponse {
    private UUID studentId;
    private String studentName;
    private String studentPrn;
    private int presentCount;
    private int totalSessions;
    private double attendancePercentage;
    private boolean isAtRisk;
    // Map of session ID to attendance status
    private Map<UUID, AttendanceStatus> sessionStatuses;
}
