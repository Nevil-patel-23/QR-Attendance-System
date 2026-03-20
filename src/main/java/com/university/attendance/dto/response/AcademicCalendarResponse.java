package com.university.attendance.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AcademicCalendarResponse {
    private UUID calendarId;
    private String academicYear;
    private Integer semesterNumber;
    private String courseName;
    private String courseCode;
    private LocalDate startDate;
    private LocalDate endDate;
    private String semesterLabel;
}
