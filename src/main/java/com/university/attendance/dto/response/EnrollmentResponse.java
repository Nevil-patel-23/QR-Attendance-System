package com.university.attendance.dto.response;

import com.university.attendance.models.SubjectType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Response DTO for an elective enrollment record.
 * Shows student info, subject info, and enrollment timestamp.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EnrollmentResponse {
    private UUID enrollmentId;
    private String studentName;
    private String studentPrn;
    private String subjectName;
    private String subjectCode;
    private SubjectType subjectType;
    private String academicYear;
    private LocalDateTime enrolledAt;
}
