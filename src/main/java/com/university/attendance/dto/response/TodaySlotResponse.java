package com.university.attendance.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalTime;
import java.util.UUID;

/**
 * DTO for teacher dashboard — one card per timetable slot today.
 * Includes window logic and active session info for the Generate QR button.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TodaySlotResponse {
    private UUID slotId;
    private String subjectName;
    private String subjectCode;
    private String semesterLabel;
    private String courseName;
    private String dayOfWeek;
    private LocalTime startTime;
    private LocalTime endTime;
    private String room;
    private boolean isWithinWindow;
    private boolean hasActiveSession;
    private UUID activeSessionId;
}
