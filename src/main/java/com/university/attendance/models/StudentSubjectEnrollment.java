package com.university.attendance.models;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Tracks which students have opted into elective subjects.
 * Maps to the 'student_subject_enrollments' table.
 *
 * ONLY elective opt-ins are stored here. Compulsory subjects are
 * inferred from student.current_semester_id — never stored in this table.
 *
 * The service layer enforces that subject_id must point to a subject
 * with type = ELECTIVE.
 *
 * UNIQUE(student_id, subject_id, academic_year) prevents double opt-in.
 */
@Entity
@Table(name = "student_subject_enrollments", uniqueConstraints = {
        @UniqueConstraint(
                name = "uk_enrollment_student_subject_year",
                columnNames = {"student_id", "subject_id", "academic_year"}
        )
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StudentSubjectEnrollment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "enrollment_id", updatable = false, nullable = false)
    private UUID enrollmentId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subject_id", nullable = false)
    private Subject subject;

    @Column(name = "academic_year", length = 10, nullable = false)
    private String academicYear;

    @CreationTimestamp
    @Column(name = "enrolled_at", updatable = false)
    private LocalDateTime enrolledAt;
}
