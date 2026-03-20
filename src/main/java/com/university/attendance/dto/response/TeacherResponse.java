package com.university.attendance.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TeacherResponse {
    private UUID teacherId;
    private String prn;
    private String firstName;
    private String lastName;
    private String phone;
    private String facultyName;
    private String designation;
    private boolean isActive;
}
