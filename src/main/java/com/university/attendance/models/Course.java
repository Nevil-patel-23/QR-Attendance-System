package com.university.attendance.models;

import jakarta.persistence.*;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Represents a degree programme (e.g. BCA, MCA, B.Tech CE).
 * Maps to the 'courses' table.
 *
 * A course belongs to one faculty and has multiple semesters.
 * duration_years drives how many semester rows exist (e.g. 3 years → 6 semesters).
 */
@Entity
@Table(name = "courses")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Course {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "course_id", updatable = false, nullable = false)
    private UUID courseId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "faculty_id", nullable = false)
    private Faculty faculty;

    @Column(name = "name", length = 150, nullable = false)
    private String name;

    @Column(name = "code", length = 10, nullable = false, unique = true)
    private String code;

    @Min(1)
    @Max(6)
    @Column(name = "duration_years", nullable = false)
    private Integer durationYears;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
