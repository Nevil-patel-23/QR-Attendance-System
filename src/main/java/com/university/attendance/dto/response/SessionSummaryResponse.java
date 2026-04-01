package com.university.attendance.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

@Data
@Builder
public class SessionSummaryResponse {
    private UUID sessionId;
    private LocalDate sessionDate;
    private LocalTime startTime;
    private LocalTime endTime;
    private String subjectName;
    private String subjectCode;
    private long presentCount;
    private long totalEnrolled;
}
