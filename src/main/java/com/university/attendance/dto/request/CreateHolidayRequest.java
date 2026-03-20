package com.university.attendance.dto.request;

import com.university.attendance.models.HolidayType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateHolidayRequest {

    @NotBlank
    private String name;

    @NotNull
    private LocalDate date;

    @NotNull
    private HolidayType type;
}
