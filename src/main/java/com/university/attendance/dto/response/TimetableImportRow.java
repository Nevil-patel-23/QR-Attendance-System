package com.university.attendance.dto.response;

import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TimetableImportRow {

    private int rowNumber;
    private String teacherPrn;
    private String subjectCode;
    private String courseCode;
    private String semesterNumber;
    private String academicYear;
    private String day;
    private String startTime;
    private String endTime;
    private String room;
    
    private boolean isValid;
    private String errorMessage;
}
