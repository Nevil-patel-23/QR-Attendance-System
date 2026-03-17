package com.university.attendance.models;

import jakarta.persistence.*;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Represents a subject within a semester (e.g. Advance Java Technologies).
 * Maps to the 'subjects' table.
 *
 * type = COMPULSORY means all students in the semester take it (inferred).
 * type = ELECTIVE means only opted-in students take it (tracked in enrollments).
 *
 * Subject code format: MCA2309 = course(MCA) + year(2) + semester(3) + subject(09)
 */
@Entity
@Table(name = "subjects")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Subject {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "subject_id", updatable = false, nullable = false)
    private UUID subjectId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "semester_id", nullable = false)
    private Semester semester;

    @Column(name = "name", length = 150, nullable = false)
    private String name;

    @Column(name = "code", length = 20, nullable = false, unique = true)
    private String code;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private SubjectType type;

    @Min(1)
    @Max(6)
    @Column(name = "credits", nullable = false)
    private Integer credits;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
