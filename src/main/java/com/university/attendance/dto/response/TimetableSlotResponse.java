package com.university.attendance.dto.response;

import com.university.attendance.models.DayOfWeek;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalTime;
import java.util.UUID;

/**
 * One timetable slot for the student timetable view.
 * Contains day, time range, room, subject info, and teacher name.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TimetableSlotResponse {
    private UUID slotId;
    private DayOfWeek dayOfWeek;
    private LocalTime startTime;
    private LocalTime endTime;
    private String room;
    private String subjectName;
    private String subjectCode;
    private String teacherName;
}
