package com.university.attendance.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginRequest {
    @NotBlank(message = "PRN is required")
    @Size(min = 10, max = 10, message = "PRN must be exactly 10 digits")
    private String prn;

    @NotBlank(message = "Password is required")
    private String password;
}
