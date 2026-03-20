package com.university.attendance.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HolidayImportRow {
    private int rowNumber;
    private String name;
    private String date;
    private String type;
    private boolean isValid;
    private String errorMessage;
}
