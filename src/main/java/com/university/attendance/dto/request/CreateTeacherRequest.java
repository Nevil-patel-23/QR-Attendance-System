package com.university.attendance.dto.request;

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
public class CreateTeacherRequest {
    @NotBlank(message = "PRN is required")
    @Size(min = 10, max = 10, message = "PRN must be exactly 10 digits")
    private String prn;

    @NotBlank(message = "First name is required")
    private String firstName;

    @NotBlank(message = "Last name is required")
    private String lastName;

    private String phone;

    @NotNull(message = "Faculty is required")
    private UUID facultyId;

    @NotBlank(message = "Designation is required")
    private String designation;
}
