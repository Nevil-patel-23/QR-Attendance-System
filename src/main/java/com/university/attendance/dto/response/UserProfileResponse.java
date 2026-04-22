package com.university.attendance.dto.response;

import com.university.attendance.models.Role;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UserProfileResponse {
    private String prn;
    private String firstName;
    private String lastName;
    private String fullName;
    private Role role;
    private String phone;

    // Student fields
    private String courseName;
    private String semesterLabel;
    private Integer batchYear;

    // Teacher fields
    private String facultyName;
}
