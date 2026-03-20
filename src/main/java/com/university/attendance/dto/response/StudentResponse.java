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
public class StudentResponse {
    private UUID studentId;
    private String prn;
    private String firstName;
    private String lastName;
    private String phone;
    private String courseName;
    private String currentSemesterLabel;
    private Integer batchYear;
    private boolean isActive;
}
