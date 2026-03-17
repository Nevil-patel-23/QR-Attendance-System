package com.university.attendance.models;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import lombok.*;

import java.util.UUID;

/**
 * Represents one semester of a course (e.g. BCA — Semester 3).
 * Maps to the 'semesters' table.
 *
 * Uses absolute numbering: 1–6 for a 3-year course, 1–8 for 4-year.
 * Year is derived as CEIL(semester_number / 2) — never stored.
 *
 * The unique constraint on (course_id, semester_number) prevents
 * duplicate semester rows per course.
 */
@Entity
@Table(name = "semesters", uniqueConstraints = {
        @UniqueConstraint(
                name = "uk_semesters_course_number",
                columnNames = {"course_id", "semester_number"}
        )
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Semester {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "semester_id", updatable = false, nullable = false)
    private UUID semesterId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @Min(1)
    @Column(name = "semester_number", nullable = false)
    private Integer semesterNumber;

    @Column(name = "label", length = 30, nullable = false)
    private String label;
}
