package com.university.attendance.models;

import jakarta.persistence.*;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Defines the start and end dates of a semester for a course.
 * Maps to the 'academic_calendars' table.
 *
 * semester_number here means which half of the academic year:
 *   1 = odd semester (July–November typically)
 *   2 = even semester (January–May typically)
 * This is NOT the same as the absolute semester number in the semesters table.
 *
 * UNIQUE(course_id, academic_year, semester_number) prevents duplicate entries.
 */
@Entity
@Table(name = "academic_calendars", uniqueConstraints = {
        @UniqueConstraint(
                name = "uk_calendar_course_year_sem",
                columnNames = {"course_id", "academic_year", "semester_number"}
        )
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AcademicCalendar {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "calendar_id", updatable = false, nullable = false)
    private UUID calendarId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @Column(name = "academic_year", length = 10, nullable = false)
    private String academicYear;

    @Min(1)
    @Max(2)
    @Column(name = "semester_number", nullable = false)
    private Integer semesterNumber;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
