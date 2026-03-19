package com.university.attendance.dto.response;

import com.university.attendance.models.SubjectType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubjectResponse {
    private UUID subjectId;
    private String name;
    private String code;
    private SubjectType type;
    private Integer credits;
    private UUID semesterId;
}
