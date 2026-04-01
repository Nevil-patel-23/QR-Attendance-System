package com.university.attendance.dto.response;

import com.university.attendance.models.DayOfWeek;
import lombok.Builder;
import lombok.Data;

import java.time.LocalTime;
import java.util.UUID;

@Data
@Builder
public class TeacherTimetableSlotResponse {
    private UUID slotId;
    private DayOfWeek dayOfWeek;
    private LocalTime startTime;
    private LocalTime endTime;
    private String room;
    private String subjectName;
    private String subjectCode;
    private String semesterLabel;
}
