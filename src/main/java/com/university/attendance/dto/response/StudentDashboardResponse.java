package com.university.attendance.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Aggregated response for the student dashboard page.
 * Combines profile info, attendance summaries, at-risk flag, and today's timetable.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StudentDashboardResponse {
    private String studentName;
    private String prn;
    private String courseName;
    private String currentSemesterLabel;
    private String academicYear;
    private List<AttendanceSummaryResponse> attendanceSummaries;
    private boolean hasAtRiskSubjects;
    private List<TimetableSlotResponse> todaySlots;
}
