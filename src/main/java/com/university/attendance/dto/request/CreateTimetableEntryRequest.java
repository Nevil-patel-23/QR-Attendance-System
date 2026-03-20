package com.university.attendance.dto.request;

import com.university.attendance.models.DayOfWeek;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateTimetableEntryRequest {

    @NotNull(message = "Course is required")
    private UUID courseId;

    @NotNull(message = "Semester is required")
    private UUID semesterId;

    @NotBlank(message = "Academic year is required")
    private String academicYear;

    @NotNull(message = "Teacher is required")
    private UUID teacherId;

    @NotNull(message = "Subject is required")
    private UUID subjectId;

    @NotNull(message = "Day of week is required")
    private DayOfWeek dayOfWeek;

    @NotNull(message = "Start time is required")
    private LocalTime startTime;

    @NotNull(message = "End time is required")
    private LocalTime endTime;

    @NotBlank(message = "Room is required")
    @Size(max = 20, message = "Room cannot exceed 20 characters")
    private String room;
}
