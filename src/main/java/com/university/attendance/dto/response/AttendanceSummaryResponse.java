package com.university.attendance.dto.response;

import com.university.attendance.models.SubjectType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Per-subject attendance summary for the student dashboard.
 * Contains totals, percentage, and at-risk calculations.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AttendanceSummaryResponse {
    private UUID subjectId;
    private String subjectName;
    private String subjectCode;
    private SubjectType subjectType;
    private int totalSessions;
    private int presentCount;
    private double attendancePercentage;
    private boolean isAtRisk;
    private int classesCanMiss;
    private int classesNeeded;
}
