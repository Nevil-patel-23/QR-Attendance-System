package com.university.attendance.models;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Assigns a teacher to a subject for a specific semester and year.
 * Maps to the 'teacher_subject_allocations' table.
 *
 * This is the central link in the chain:
 *   Teacher → teaches → Subject → in → Semester → for → AcademicYear
 *
 * TimetableSlots and AttendanceSessions both reference this table.
 *
 * UNIQUE(teacher_id, subject_id, semester_id, academic_year) prevents
 * the same teacher being assigned twice to the same subject+semester+year.
 */
@Entity
@Table(name = "teacher_subject_allocations", uniqueConstraints = {
        @UniqueConstraint(
                name = "uk_allocation_teacher_subject_sem_year",
                columnNames = {"teacher_id", "subject_id", "semester_id", "academic_year"}
        )
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TeacherSubjectAllocation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "allocation_id", updatable = false, nullable = false)
    private UUID allocationId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "teacher_id", nullable = false)
    private Teacher teacher;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subject_id", nullable = false)
    private Subject subject;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "semester_id", nullable = false)
    private Semester semester;

    @Column(name = "academic_year", length = 10, nullable = false)
    private String academicYear;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
