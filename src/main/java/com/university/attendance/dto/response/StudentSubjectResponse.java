package com.university.attendance.dto.response;

import com.university.attendance.models.SubjectType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Subject card for the "My Subjects" view.
 * Shows subject details and the allocated teacher (or "Not assigned").
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StudentSubjectResponse {
    private UUID subjectId;
    private String subjectName;
    private String subjectCode;
    private SubjectType subjectType;
    private int credits;
    private String teacherName;
}
