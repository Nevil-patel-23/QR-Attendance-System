package com.university.attendance.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AcademicCalendarImportRow {
    private int rowNumber;
    private String courseCode;
    private String academicYear;
    private String semesterNumber;
    private String startDate;
    private String endDate;
    private boolean isValid;
    private String errorMessage;
}
