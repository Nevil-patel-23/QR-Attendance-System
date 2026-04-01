package com.university.attendance.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AttendanceMatrixResponse {
    private String semesterLabel;
    private String courseName;
    private String courseCode;
    private LocalDate fromDate;
    private LocalDate toDate;
    private List<String> compulsorySubjectHeaders;
    private List<String> electiveSubjectHeaders;
    private List<StudentAttendanceMatrixRow> rows;
    private int totalStudents;
    private int atRiskCount;
}
