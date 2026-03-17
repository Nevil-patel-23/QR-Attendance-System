package com.university.attendance.models;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Represents one student's attendance outcome for one session.
 * Maps to the 'attendance_records' table.
 *
 * PRESENT records: created when a student successfully scans the QR.
 *   scanned_at = timestamp of the scan.
 *
 * ABSENT records: inserted by the @Scheduled background job after
 *   the session expires. scanned_at = null.
 *
 * THE MOST CRITICAL CONSTRAINT IN THE ENTIRE SYSTEM:
 * UNIQUE(session_id, student_id) — makes duplicate scans physically
 * impossible even when 50+ students scan simultaneously.
 * If two identical INSERT statements race, only one succeeds;
 * the other throws DataIntegrityViolationException which the
 * service layer catches and converts to DuplicateScanException.
 */
@Entity
@Table(name = "attendance_records", uniqueConstraints = {
        @UniqueConstraint(
                name = "uk_attendance_session_student",
                columnNames = {"session_id", "student_id"}
        )
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AttendanceRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "record_id", updatable = false, nullable = false)
    private UUID recordId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    private AttendanceSession session;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @Column(name = "scanned_at")
    private LocalDateTime scannedAt;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private AttendanceStatus status = AttendanceStatus.PRESENT;
}
