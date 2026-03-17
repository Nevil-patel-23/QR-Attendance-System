package com.university.attendance.models;

import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Represents a login identity in the system.
 * Maps to the 'users' table.
 *
 * Login strategy:
 *   - Admin logs in with PRN + password.
 *   - Teachers and Students log in with PRN (10-digit) + password.
 *
 * PRN (Permanent Registration Number) is a 10-digit unique number
 * assigned by the university.
 * Admin PRN is set by whoever creates the admin account.
 *
 * Soft-delete: set is_active = false to block login without
 * losing any historical attendance data.
 */
@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "user_id", updatable = false, nullable = false)
    private UUID userId;

    @Size(min = 10, max = 10)
    @Column(name = "prn", length = 10, nullable = false, unique = true)
    private String prn;

    @Column(name = "password_hash", length = 255, nullable = false)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false)
    private Role role;

    @Builder.Default
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
