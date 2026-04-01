package com.university.attendance.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class TeacherSubjectResponse {
    private UUID allocationId;
    private UUID subjectId;
    private String subjectName;
    private String subjectCode;
    private String semesterLabel;
    private String courseCode;
    private String academicYear;
}
