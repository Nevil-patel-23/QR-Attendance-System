package com.university.attendance.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Request body for enrolling a student in an elective subject.
 * Only ELECTIVE subjects are allowed — validated in service layer.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateEnrollmentRequest {

    @NotNull(message = "Student ID is required")
    private UUID studentId;

    @NotNull(message = "Subject ID is required")
    private UUID subjectId;

    @NotBlank(message = "Academic year is required")
    private String academicYear;
}
