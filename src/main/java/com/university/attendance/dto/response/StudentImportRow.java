package com.university.attendance.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudentImportRow {
    private int rowNumber;
    private String prn;
    private String firstName;
    private String lastName;
    private String phone;
    private String courseCode;
    private int semesterNumber;
    private int batchYear;
    private boolean isValid;
    private String errorMessage;
}
