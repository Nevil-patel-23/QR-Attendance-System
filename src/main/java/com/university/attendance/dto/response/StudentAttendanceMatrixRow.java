package com.university.attendance.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudentAttendanceMatrixRow {
    private UUID studentId;
    private String studentName;
    private String studentPrn;
    private List<SubjectAttendanceSummary> compulsorySubjects;
    private List<SubjectAttendanceSummary> electiveSubjects;
}
