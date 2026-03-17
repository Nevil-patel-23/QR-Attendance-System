package com.university.attendance.models;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Represents an academic faculty (e.g. Faculty of Computer Science).
 * Maps to the 'faculties' table.
 *
 * A faculty contains one or more courses.
 * Example: Faculty of Computer Science → BCA, MCA
 */
@Entity
@Table(name = "faculties")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Faculty {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "faculty_id", updatable = false, nullable = false)
    private UUID facultyId;

    @Column(name = "name", length = 100, nullable = false, unique = true)
    private String name;

    @Column(name = "code", length = 10, nullable = false, unique = true)
    private String code;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
