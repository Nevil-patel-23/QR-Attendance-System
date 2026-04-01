package com.university.attendance.dto.response;

import com.university.attendance.models.SubjectType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubjectAttendanceSummary {
    private UUID subjectId;
    private String subjectName;
    private String subjectCode;
    private SubjectType subjectType;
    private int presentCount;
    private int totalSessions;
    private double attendancePercentage;
    private boolean isAtRisk;
    private boolean isEnrolled;
}
