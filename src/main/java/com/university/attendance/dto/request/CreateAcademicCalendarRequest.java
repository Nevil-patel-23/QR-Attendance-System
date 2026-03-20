package com.university.attendance.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateAcademicCalendarRequest {

    @NotNull
    private UUID courseId;

    @NotBlank
    private String academicYear;

    @NotNull
    @Min(1)
    @Max(2)
    private Integer semesterNumber;

    @NotNull
    private LocalDate startDate;

    @NotNull
    private LocalDate endDate;
}
