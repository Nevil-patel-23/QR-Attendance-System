package com.university.attendance.models;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Represents a student's profile and academic details.
 * Maps to the 'students' table.
 *
 * Linked 1:1 to User (login credentials).
 * prn = 10-digit Permanent Registration Number assigned by the university.
 * current_semester_id determines which compulsory subjects the student
 * takes and which timetable they see. Updated on semester promotion.
 *
 * batch_year = year of admission (e.g. 2022).
 */
@Entity
@Table(name = "students")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Student {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "student_id", updatable = false, nullable = false)
    private UUID studentId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(name = "prn", length = 10, nullable = false, unique = true)
    private String prn;

    @Column(name = "first_name", length = 80, nullable = false)
    private String firstName;

    @Column(name = "last_name", length = 80, nullable = false)
    private String lastName;

    @Column(name = "phone", length = 15, unique = true)
    private String phone;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "current_semester_id", nullable = false)
    private Semester currentSemester;

    @Column(name = "batch_year", nullable = false)
    private Integer batchYear;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
