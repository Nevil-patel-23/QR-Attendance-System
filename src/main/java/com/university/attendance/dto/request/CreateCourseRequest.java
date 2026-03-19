package com.university.attendance.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateCourseRequest {
    @NotBlank(message = "Course name is required")
    private String name;

    @NotBlank(message = "Course code is required")
    @Size(max = 10, message = "Code cannot exceed 10 characters")
    private String code;

    @Min(value = 1, message = "Duration must be at least 1 year")
    @Max(value = 6, message = "Duration cannot exceed 6 years")
    private Integer durationYears;

    @NotNull(message = "Faculty ID is required")
    private UUID facultyId;
}
