package com.university.attendance.dto.response;

import com.university.attendance.models.DayOfWeek;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TimetableEntryResponse {

    private UUID slotId;
    private UUID allocationId;
    
    private String teacherName;
    private String teacherPrn;
    
    private String subjectName;
    private String subjectCode;
    
    private String semesterLabel;
    
    private String courseName;
    private String courseCode;
    
    private String academicYear;
    
    private DayOfWeek dayOfWeek;
    private LocalTime startTime;
    private LocalTime endTime;
    private String room;
    
    private LocalDate effectiveFrom;
}
