package com.university.attendance.models;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Represents a teacher's profile and faculty assignment.
 * Maps to the 'teachers' table.
 *
 * Linked 1:1 to User (login credentials).
 * Belongs to one faculty via @ManyToOne.
 * prn = 10-digit Permanent Registration Number assigned by the university.
 */
@Entity
@Table(name = "teachers")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Teacher {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "teacher_id", updatable = false, nullable = false)
    private UUID teacherId;

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
    @JoinColumn(name = "faculty_id", nullable = false)
    private Faculty faculty;

    @Column(name = "designation", length = 100, nullable = false)
    private String designation;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
