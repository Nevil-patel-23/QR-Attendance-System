package com.university.attendance.dto.response;

import com.university.attendance.models.HolidayType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HolidayResponse {
    private UUID holidayId;
    private String name;
    private LocalDate date;
    private HolidayType type;
}
